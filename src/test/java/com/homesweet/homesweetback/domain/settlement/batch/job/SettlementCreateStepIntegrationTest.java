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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.batch.job.enabled=true")
@SpringBatchTest
@ActiveProfiles("test")
@Import(BatchConfig.class)
@DisplayName("정산 생성 step 통합테스트")
public class SettlementCreateStepIntegrationTest {

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

    @BeforeEach
    void clean() {
        // 정산 관련 삭제
        settlementRepository.deleteAll();

        // 주문 및 하위 엔티티 삭제
        orderRepository.deleteAll();

        // 필요하면 product/sku 등도 함께 삭제
    }

    @Test
    @DisplayName("정산 생성 Step이 정상적으로 실행되어 Settlement가 저장된다")
    void settlementCreateStep_success() throws Exception {
        Grade grade = gradeRepository.save(
                BatchHelperData.createGrade()
        );

        // seller
        User seller = userRepository.save(BatchHelperData.createSeller(grade));

        // 카테고리
        ProductCategoryEntity category = categoryRepository.save(
                BatchHelperData.createCategory()
        );

        // 상품
        ProductEntity product = productRepository.save(
                BatchHelperData.createProduct(seller, category)
        );

        // SKU
        SkuEntity sku = skuRepository.save(
                BatchHelperData.createSku(product)
        );

        // 주문 생성
        Order order = BatchHelperData.createCompletedOrder(seller, LocalDateTime.now().minusHours(2));

        // OrderItem 연결
        order = BatchHelperData.setupFullOrderGraph(order, sku);

        // Order 저장
        order = orderRepository.save(order);
        orderRepository.save(order);
        orderRepository.saveAndFlush(order);
        System.out.println("order orderedAt = " + order.getOrderedAt());
        System.out.println("saved order = " + order);


        // 2) When — Step 실행
        JobParameters params = new JobParametersBuilder()
                .addString("cutoff", order.getOrderedAt().minusDays(1).toString(), false) // cutoff 이후 주문이 대상
                .addLong("time", System.currentTimeMillis(), true)
                .toJobParameters();
//        jobLauncherTestUtils.launchJob(params);

        JobExecution execution = jobLauncherTestUtils.launchStep("settlementCreateStep", params);

        // 3) Then — Step 성공 여부
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        System.out.println("orderStatus in DB = " +
                orderRepository.findById(order.getId()).get().getOrderStatus());

        // Settlement가 저장되었는지 확인
        List<Settlement> settlements = settlementRepository.findAll();
        assertThat(settlements).hasSize(1);
    }
}
