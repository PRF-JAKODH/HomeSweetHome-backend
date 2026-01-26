package com.homesweet.homesweetback.domain.settlement.batch.job;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.grade.entity.Grade;
import com.homesweet.homesweetback.domain.grade.repository.GradeRepository;
import com.homesweet.homesweetback.domain.order.entity.DeliveryStatus;
import com.homesweet.homesweetback.domain.order.entity.Order;
import com.homesweet.homesweetback.domain.order.repository.OrderRepository;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.ProductCategoryJPARepository;
import com.homesweet.homesweetback.domain.product.category.repository.jpa.entity.ProductCategoryEntity;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.ProductJPARepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.SkuJPARepository;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.ProductEntity;
import com.homesweet.homesweetback.domain.product.product.command.repository.jpa.entity.SkuEntity;
import com.homesweet.homesweetback.domain.settlement.data.BatchHelperData;
import com.homesweet.homesweetback.domain.settlement.entity.Settlement;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@Import(BatchConfig.class)
@DisplayName("정산 취소 step 통합테스트")
public class SettlementCancelStepIntegrationTest {
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

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(settlementJob);
    }

    @Test
    @DisplayName("정산 취소 Step이 정상적으로 실행되어 Settlement가 취소된다.")
    void settlementCancelStep_success() {
        // 1) 판매자 Grade 저장
        Grade grade = gradeRepository.save(BatchHelperData.createGrade());

        // 2) 판매자 저장
        User seller = userRepository.save(BatchHelperData.createSeller(grade));

        // 3) 상품 Category 저장
        ProductCategoryEntity category = categoryRepository.save(BatchHelperData.createCategory());

        // 4) Product 저장
        ProductEntity product = productRepository.save(
                BatchHelperData.createProduct(seller, category)
        );

        // 5) SKU 저장
        SkuEntity sku = skuRepository.save(
                BatchHelperData.createSku(product)
        );

        // 6) 주문 생성 및 저장
        Order order = BatchHelperData.createCompletedOrder(seller, LocalDateTime.now().minusHours(5));
        order = BatchHelperData.setupFullOrderGraph(order, sku);
        orderRepository.saveAndFlush(order);

        // 7) 정산 생성 Step을 먼저 수행해야 Settlement 가 생김
        JobParameters createParams = new JobParametersBuilder()
                .addString("cutoff", order.getOrderedAt().minusDays(1).toString(), false)
                .addLong("time", System.currentTimeMillis(), true)
                .toJobParameters();

        jobLauncherTestUtils.launchStep("settlementCreateStep", createParams);

        // 8) 생성된 Settlement 존재 확인
        Settlement created = settlementRepository.findAll().get(0);
        assertThat(created.getSettlementStatus()).isEqualTo("PENDING");

        // 9) 주문 상태를 CANCELED 로 변경 후 저장
        order.setDeliveryStatus(DeliveryStatus.CANCELLED);
        orderRepository.saveAndFlush(order);

        Order savedOrder = orderRepository.findById(order.getId()).get();
        System.out.println("DB delivery = " + savedOrder.getDeliveryStatus());


        // 10) 취소 Step 실행
        JobParameters cancelParams = new JobParametersBuilder()
                .addString("cutoff", LocalDateTime.now().toString(), false)
                .addLong("time", System.currentTimeMillis(), true)
                .toJobParameters();

        JobExecution execution = jobLauncherTestUtils.launchStep("settlementCancelStep", cancelParams);
        execution.getAllFailureExceptions().forEach(Throwable::printStackTrace);

        // 11) Step 성공 검증
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 12) 정산 상태가 CANCELED 로 업데이트 되었는지 확인
        Settlement canceled = settlementRepository.findAll().get(0);
        assertThat(canceled.getSettlementStatus()).isEqualTo("CANCELED");
        System.out.println("Settlement Status = " + canceled.getSettlementStatus());
    }
}
