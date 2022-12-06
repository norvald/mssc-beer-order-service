package guru.sfg.beer.order.service.state;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.common.events.ValidateBeerOrderResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidateOrderResponseListener {

    private final BeerOrderManager beerOrderManager;
    private final BeerOrderRepository beerOrderRepository;

    @Transactional
    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_RESULT_QUEUE)
    public void listen(ValidateBeerOrderResponse event) {
        BeerOrder beerOrder = beerOrderRepository.findOneById(UUID.fromString(event.getBeerOrderId()));
        log.debug("Validation result for beer order "+event.getBeerOrderId() + " is "+event.getIsValid());
        beerOrderManager.validateOrder(beerOrder, event.getIsValid());
    }
}
