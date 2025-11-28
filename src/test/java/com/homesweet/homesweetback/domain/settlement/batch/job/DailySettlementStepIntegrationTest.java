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
import com.homesweet.homesweetback.domain.settlement.repository.DailySettlementRepository;
import com.homesweet.homesweetback.domain.settlement.repository.SettlementRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(properties = "spring.batch.job.enabled=true")
@SpringBatchTest
@ActiveProfiles("test")
@Import(BatchConfig.class)
@DisplayName("일별 집계 step 통합테스트")
class DailySettlementStepIntegrationTest {
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
    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(settlementJob);
    }
    @Test
    @DisplayName("dailyStep 실행 시 일별 집계가 된다.")
    void dailyStep() {
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

        // 4) Step1 : Settlement 생성
        JobParameters createParams = new JobParametersBuilder()
                .addString("cutoff", order.getOrderedAt().minusDays(1).toString(),false)
                .addLong("time", System.currentTimeMillis(),true)
                .toJobParameters();

        jobLauncherTestUtils.launchStep("settlementCreateStep", createParams);

        // Settlement 1개 존재 확인
        List<Settlement> settlements = settlementRepository.findAll();
        assertThat(settlements).hasSize(1);

        LocalDate cutoffDate = order.getOrderedAt().toLocalDate();
        JobParameters params = new JobParametersBuilder()
            .addString("cutoff", cutoffDate.atStartOfDay().toString(),false)
            .addLong("time", System.currentTimeMillis(), true).toJobParameters();

        // 5) Step3 : Daily 집계 Step 실행
        JobExecution execution = jobLauncherTestUtils.launchStep("dailyStep", params);
        execution.getAllFailureExceptions().forEach(Throwable::printStackTrace);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 6) DailySettlement 생성 확인
        List<DailySettlement> dailySettlements = dailySettlementRepository.findAll();
        assertThat(dailySettlements).hasSize(1);

        DailySettlement daily = dailySettlements.get(0);
        LocalDate expectedDate = order.getOrderedAt().toLocalDate();

        assertThat(daily.getSettlementDate().toLocalDate()).isEqualTo(expectedDate);

        // 금액 검증 (정확한 계산은 SettlementCalculator 기반)
        assertThat(daily.getTotalSales()).isEqualTo(settlements.get(0).getSalesAmount());
        assertThat(daily.getTotalSettlement()).isEqualTo(settlements.get(0).getSettlementAmount());
    }
}