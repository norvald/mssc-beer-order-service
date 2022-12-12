package guru.sfg.beer.order.service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.services.beer.BeerService;
import guru.sfg.beer.order.service.services.beer.BeerServiceRestTemplateImpl;
import guru.sfg.common.model.BeerDto;
import guru.sfg.common.model.BeerOrderStatusEnum;
import guru.sfg.common.model.BeerStyleEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        BeerDto beerDto = BeerDto.builder().id(beerId).beerStyle(BeerStyleEnum.APA).upc("12345").build();
        stubFor(get(BeerServiceRestTemplateImpl.BEERUPC_PATH + "12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();

        BeerOrder saveBeerOrder = beerOrderManager.newBeerOrder(beerOrder);
        UUID id = saveBeerOrder.getId();
        System.out.println("beerOrderId: "+id);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(id).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATED, foundOrder.getOrderStatus());
        });

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(id).get();
            BeerOrderLine line = foundOrder.getBeerOrderLines().iterator().next();
            assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
        });
//        check(id);
//    }
//    public void check(UUID id) {
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
