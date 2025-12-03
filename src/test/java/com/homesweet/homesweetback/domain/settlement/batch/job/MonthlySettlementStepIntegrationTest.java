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
import com.homesweet.homesweetback.domain.settlement.entity.MonthlySettlement;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
import com.homesweet.homesweetback.domain.settlement.entity.WeeklySettlement;
import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.MonthlySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.WeeklySettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.batch.job.enabled=true")
@SpringBatchTest
@ActiveProfiles("test")
@Import(BatchConfig.class)
@DisplayName("월별 집계 step 통합테스트")
class MonthlySettlementStepIntegrationTest {
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
    private DailySettlementRepository dailySettlementRepository;
    @Autowired
    private WeeklySettlementRepository weeklySettlementRepository;
    @Autowired
    private MonthlySettlementRepository monthlySettlementRepository;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(settlementJob);
    }
    @BeforeEach
    void clean() {
        // 정산 관련 삭제
        monthlySettlementRepository.deleteAll();
        weeklySettlementRepository.deleteAll();
        dailySettlementRepository.deleteAll();
        settlementRepository.deleteAll();

        // 주문 및 하위 엔티티 삭제
        orderRepository.deleteAll();
    }
    @Test
    @Disabled("DailySettlement 생성 이슈로 인해 임시 비활성화")
    @DisplayName("monthlyStep 실행 시 월별 집계가 된다.")
    void monthlyStep() {
        LocalDateTime orderedAt = LocalDateTime.now().minusHours(1);
        // 1) Grade & Seller 저장
        Grade grade = gradeRepository.save(BatchHelperData.createGrade());
        User seller = userRepository.save(BatchHelperData.createSeller(grade));

        // 2) Product Category, Product, SKU 저장
        ProductCategoryEntity category = categoryRepository.save(BatchHelperData.createCategory());
        ProductEntity product = productRepository.save(BatchHelperData.createProduct(seller, category));
        SkuEntity sku = skuRepository.save(BatchHelperData.createSku(product));

        // 3) Order 생성 및 저장
        Order order = BatchHelperData.createCompletedOrder(seller, LocalDateTime.now().minusHours(5));
        order = BatchHelperData.setupFullOrderGraph(order, sku);
        orderRepository.saveAndFlush(order);
        LocalDate cutoffDate = orderedAt.toLocalDate();
        String cutoff = cutoffDate.atStartOfDay().toString();

        JobParameters createParams = new JobParametersBuilder()
                .addString("cutoff", cutoff, false)
                .addLong("time", System.currentTimeMillis(),true)
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
                .addString("cutoff", cutoff, false)
                .addLong("time", System.currentTimeMillis(), true)
                .toJobParameters();

        JobExecution weeklyExe = jobLauncherTestUtils.launchStep("weeklyStep", weeklyParams);
        assertThat(weeklyExe.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<WeeklySettlement> weekList = weeklySettlementRepository.findAll();
        assertThat(weekList)
                .as("WeeklySettlement는 1개 생성되어야 한다")
                .hasSize(1);

        WeeklySettlement weekly = weekList.get(0);

        JobParameters monthlyParams = new JobParametersBuilder()
                .addString("cutoff", cutoff, false)
                .addLong("time", System.currentTimeMillis(), true)
                .toJobParameters();

        JobExecution monthlyExe = jobLauncherTestUtils.launchStep("monthlyStep", monthlyParams);
        assertThat(weeklyExe.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<MonthlySettlement> monthlyList = monthlySettlementRepository.findAll();
        assertThat(monthlyList)
                .as("MonthlySettlement는 1개 생성되어야 한다")
                .hasSize(1);

        Settlement st = settlementRepository.findAll().get(0);

        assertThat(weekly.getTotalSales()).isEqualTo(st.getSalesAmount());
        assertThat(weekly.getTotalFee()).isEqualTo(st.getFee());
        assertThat(weekly.getTotalVat()).isEqualTo(st.getVat());
        assertThat(weekly.getTotalSettlement()).isEqualTo(st.getSettlementAmount());
    }
}