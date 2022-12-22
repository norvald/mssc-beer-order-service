package guru.sfg.beer.order.service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.services.beer.BeerService;
import guru.sfg.beer.order.service.services.beer.BeerServiceRestTemplateImpl;
import guru.sfg.brewery.model.BeerDto;
import guru.sfg.brewery.model.BeerOrderStatusEnum;
import guru.sfg.brewery.model.BeerStyleEnum;
import guru.sfg.brewery.model.events.DeallocateOrderRequest;
import guru.sfg.brewery.model.events.FailedAllocationRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@WireMockTest(httpPort = 8083)
@SpringBootTest
public class BeerOrderManangerImplIT {
    @Autowired
    BeerOrderManager beerOrderManager;
    @Autowired
    BeerOrderRepository beerOrderRepository;
    @Autowired
    CustomerRepository customerRepository;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JmsTemplate jmsTemplate;

    @Autowired
    BeerService beerService;

    //    @Autowired
//    WireMockServer wireMockServer;
    Customer testCustomer;

    UUID beerId = UUID.randomUUID();

//    @TestConfiguration
//    static class RestTemplateBuilderProvider {
//        @Bean(destroyMethod = "stop")
//        public WireMockServer wireMockServer() {
//            WireMockServer server = new WireMockServer(new WireMockConfiguration().port(8083));
//            server.start();
//            return server;
//        }
//    }

    @BeforeEach
    void setup() {
        testCustomer = customerRepository.save(Customer.builder().customerName("Bart Simpson").build());
    }

    @Test
    void beerServiceTest() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc("12345").build();
        stubFor(get(BeerServiceRestTemplateImpl.BEERUPC_PATH + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        Optional<BeerDto> beerDtoOptional = beerService.getBeerByUpc("12345");
        System.out.println(beerDtoOptional.get());
    }

    @Test
    //@Transactional
    void testNewToAllocated() throws JsonProcessingException, InterruptedException {
        BeerDto beerDto = BeerDto.builder().id(beerId).beerStyle(BeerStyleEnum.APA.name()).upc("12345").build();
        stubFor(get(BeerServiceRestTemplateImpl.BEERUPC_PATH + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();

        BeerOrder saveBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
        UUID id = saveBeerOrder.getId();
        System.out.println("beerOrderId: "+id);

        log.debug("Wait for ALLOCATED");
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(id).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATED, foundOrder.getOrderStatus());
        });

        log.debug("Wait for orderQuantity");
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(id).get();
            BeerOrderLine line = foundOrder.getBeerOrderLines().iterator().next();
            assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
        });

        Optional<BeerOrder> updatedBeerOrder = beerOrderRepository.findById(id);
        if (updatedBeerOrder.isPresent()) {
            assertNotNull(updatedBeerOrder.get());
            assertEquals(BeerOrderStatusEnum.ALLOCATED, updatedBeerOrder.get().getOrderStatus());
        }

        BeerOrder savedBeerOrder2 = beerOrderRepository.findById(saveBeerOrder.getId()).get();

        assertNotNull(savedBeerOrder2);
        assertEquals(BeerOrderStatusEnum.ALLOCATED, savedBeerOrder2.getOrderStatus());
        savedBeerOrder2.getBeerOrderLines().forEach(line -> {
            assertEquals(line.getOrderQuantity(),line.getQuantityAllocated());
        });
    }
    @Test
    void testFailedValidation() throws JsonProcessingException, InterruptedException {
        BeerDto beerDto = BeerDto.builder().id(beerId).beerStyle(BeerStyleEnum.APA.name()).upc("12345").build();
        stubFor(get(BeerServiceRestTemplateImpl.BEERUPC_PATH + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("fail-validation");

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.VALIDATION_EXCEPTION, foundOrder.getOrderStatus());
        });
    }
    @Test
    void testValidationPendingToCancel() throws JsonProcessingException, InterruptedException {
        BeerDto beerDto = BeerDto.builder().id(beerId).beerStyle(BeerStyleEnum.APA.name()).upc("12345").build();
        stubFor(get(BeerServiceRestTemplateImpl.BEERUPC_PATH + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("dont-validate");
        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.VALIDATION_PENDING, foundOrder.getOrderStatus());

            beerOrderManager.cancelBeerOrder(foundOrder.getId());
        });

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.CANCELLED, foundOrder.getOrderStatus());
        });

    }
    @Test
    void testNewToAllocationPendingToCancel() throws JsonProcessingException, InterruptedException {
        BeerDto beerDto = BeerDto.builder().id(beerId).beerStyle(BeerStyleEnum.APA.name()).upc("12345").build();
        stubFor(get(BeerServiceRestTemplateImpl.BEERUPC_PATH + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("dont-allocate");

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATION_PENDING, foundOrder.getOrderStatus());
            log.debug("Cancel order: "+foundOrder.getId());
            beerOrderManager.cancelBeerOrder(foundOrder.getId());
        });
        log.debug("Wait for CANCELLED");
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.CANCELLED, foundOrder.getOrderStatus());
        });
    }
    @Test
    void testFailedAllocation() throws JsonProcessingException, InterruptedException {
        BeerDto beerDto = BeerDto.builder().id(beerId).beerStyle(BeerStyleEnum.APA.name()).upc("12345").build();
        stubFor(get(BeerServiceRestTemplateImpl.BEERUPC_PATH + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("fail-allocation");

   //        if(!StringUtils.equals(savedBeerOrder.getId().toString(), deallocateOrderRequest.getBeerOrderDto().getId().toString())) {
//            deallocateOrderRequest = (DeallocateOrderRequest) jmsTemplate.receiveAndConvert(JmsConfig.DEALLOCATE_ORDER_QUEUE);
//        }
     BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.ALLOCATION_EXCEPTION, foundOrder.getOrderStatus());
        });

        FailedAllocationRequest failedAllocationRequest = (FailedAllocationRequest) jmsTemplate.receiveAndConvert(JmsConfig.FAILED_ALLOCATION_QUEUE);
        assertNotNull(failedAllocationRequest);
        assertEquals(failedAllocationRequest.getBeerOrderId(), savedBeerOrder.getId().toString());
    }

    @Test
    void testAllocatedToCancelled() throws JsonProcessingException, InterruptedException {
        BeerDto beerDto = BeerDto.builder().id(beerId).beerStyle(BeerStyleEnum.APA.name()).upc("12345").build();
        stubFor(get(BeerServiceRestTemplateImpl.BEERUPC_PATH + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATED, foundOrder.getOrderStatus());
            beerOrderManager.cancelBeerOrder(foundOrder.getId());
        });
        log.debug("Wait for CANCELLED");
        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.CANCELLED, foundOrder.getOrderStatus());
        });
        log.debug("Check jms message: "+beerOrder.getId());
        DeallocateOrderRequest deallocateOrderRequest = (DeallocateOrderRequest) jmsTemplate.receiveAndConvert(JmsConfig.DEALLOCATE_ORDER_QUEUE);
//        if(!StringUtils.equals(savedBeerOrder.getId().toString(), deallocateOrderRequest.getBeerOrderDto().getId().toString())) {
//            deallocateOrderRequest = (DeallocateOrderRequest) jmsTemplate.receiveAndConvert(JmsConfig.DEALLOCATE_ORDER_QUEUE);
//        }
        assertNotNull(deallocateOrderRequest);
        assertEquals(savedBeerOrder.getId().toString(), deallocateOrderRequest.getBeerOrderDto().getId().toString());
    }
    @Test
    void testPartialAllocation() throws JsonProcessingException, InterruptedException {
        BeerDto beerDto = BeerDto.builder().id(beerId).beerStyle(BeerStyleEnum.APA.name()).upc("12345").build();
        stubFor(get(BeerServiceRestTemplateImpl.BEERUPC_PATH + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("partial-allocation");

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.PENDING_INVENTORY, foundOrder.getOrderStatus());
        });
    }

    @Test
    void testNewToPickedUp() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).beerStyle(BeerStyleEnum.APA.name()).upc("12345").build();
        stubFor(get(BeerServiceRestTemplateImpl.BEERUPC_PATH + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATED, foundOrder.getOrderStatus());
            beerOrderManager.beerOrderPickedUp(savedBeerOrder.getId());
        });

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(beerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.PICKED_UP, foundOrder.getOrderStatus());
        });

        BeerOrder picketUpBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

        assertNotNull(picketUpBeerOrder);
        assertEquals(BeerOrderStatusEnum.PICKED_UP, picketUpBeerOrder.getOrderStatus());
    }
    public BeerOrder createBeerOrder() {
        BeerOrder beerOrder = BeerOrder.builder()
                .customer(testCustomer)
                .build();

        Set<BeerOrderLine> lines = new HashSet<>();
        lines.add(BeerOrderLine.builder()
                .beerId(beerId)
                .upc("12345")
                .orderQuantity(1)
                .beerOrder(beerOrder)
                .build());
        beerOrder.setBeerOrderLines(lines);

        return beerOrder;
    }


}
