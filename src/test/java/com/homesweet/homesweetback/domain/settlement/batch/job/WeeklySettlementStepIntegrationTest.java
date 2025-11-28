package com.homesweet.homesweetback.domain.settlement.batch.job;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.grade.entity.Grade;
import com.homesweet.homesweetback.domain.grade.repository.GradeRepository;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.ProductCategoryJPARepository;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.ProductJPARepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.SkuJPARepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.SkuEntity;
import com.homesweet.homesweetback.domain.settlement.data.BatchHelperData;
import com.homesweet.homesweetback.domain.settlement.entity.DailySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.WeeklySettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(properties = "spring.batch.job.enabled=true")
@SpringBatchTest
@ActiveProfiles("test")
@Import(BatchConfig.class)
@DisplayName("주별 집계 step 통합테스트")
class WeeklySettlementStepIntegrationTest {
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    @Autowired
    @Qualifier("settlementJob")  // BatchConfig에 있는 Job 이름과 정확히 일치해야 함
    private Job settlementJob;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private SettlementRepository settlementRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductCategoryJPARepository categoryRepository;
    @Autowired
    private ProductJPARepository productRepository;
    @Autowired
    private SkuJPARepository skuRepository;
    @Autowired
    private GradeRepository gradeRepository;
    @Autowired
    private WeeklySettlementRepository weeklySettlementRepository;
    @Autowired
    private DailySettlementRepository dailySettlementRepository;
    @BeforeEach
    void clean() {
        // 정산 관련 삭제
        weeklySettlementRepository.deleteAll();
        dailySettlementRepository.deleteAll();
        settlementRepository.deleteAll();

        // 주문 및 하위 엔티티 삭제
        orderRepository.deleteAll();
    }
    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(settlementJob);
    }
    @Test
    @DisplayName("weeklyStep 실행 시 주별 집계가 된다.")
    void weeklyStep() {
        LocalDateTime orderedAt = LocalDateTime.now().minusHours(1);
        Grade grade = gradeRepository.save(BatchHelperData.createGrade());
        User seller = userRepository.save(BatchHelperData.createSeller(grade));

        ProductCategoryEntity category = categoryRepository.save(BatchHelperData.createCategory());
        ProductEntity product = productRepository.save(BatchHelperData.createProduct(seller, category));
        SkuEntity sku = skuRepository.save(BatchHelperData.createSku(product));

        Order order = BatchHelperData.createCompletedOrder(seller, orderedAt);
        order = BatchHelperData.setupFullOrderGraph(order, sku);
        orderRepository.saveAndFlush(order);

        LocalDate cutoffDate = orderedAt.toLocalDate();
        String cutoff = cutoffDate.atStartOfDay().toString();

        JobParameters createParams = new JobParametersBuilder()
                .addString("cutoff", cutoff, false)
                .addLong("time", System.currentTimeMillis(), true)
                .toJobParameters();

        jobLauncherTestUtils.launchStep("settlementCreateStep", createParams);

        assertThat(settlementRepository.findAll())
                .as("Settlement는 1개 생성되어야 한다")
                .hasSize(1);

        JobParameters dailyParams = new JobParametersBuilder()
                .addString("cutoff", cutoff, false)
                .addLong("time", System.currentTimeMillis(),true)
                .toJobParameters();

        JobExecution dailyExe = jobLauncherTestUtils.launchStep("dailyStep", dailyParams);
        assertThat(dailySettlementRepository.findAll())
                .as("DailySettlement는 1개 생성되어야 한다")
                .hasSize(1);

        JobParameters weeklyParams = new JobParametersBuilder()
                .addString("cutoff", cutoff,false)
                .addLong("time", System.currentTimeMillis(),true)
                .toJobParameters();

        JobExecution weeklyExe = jobLauncherTestUtils.launchStep("weeklyStep", weeklyParams);
        assertThat(weeklyExe.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<WeeklySettlement> weekList = weeklySettlementRepository.findAll();
        assertThat(weekList)
                .as("WeeklySettlement는 1개 생성되어야 한다")
                .hasSize(1);

        WeeklySettlement weekly = weekList.get(0);

        LocalDate weekStart = cutoffDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = cutoffDate.with(DayOfWeek.SUNDAY);

        assertThat(weekly.getWeekStartDate()).isEqualTo(weekStart);
        assertThat(weekly.getWeekEndDate()).isEqualTo(weekEnd);

        Settlement st = settlementRepository.findAll().get(0);

        assertThat(weekly.getTotalSales()).isEqualTo(st.getSalesAmount());
        assertThat(weekly.getTotalFee()).isEqualTo(st.getFee());
        assertThat(weekly.getTotalVat()).isEqualTo(st.getVat());
        assertThat(weekly.getTotalSettlement()).isEqualTo(st.getSettlementAmount());
    }
}