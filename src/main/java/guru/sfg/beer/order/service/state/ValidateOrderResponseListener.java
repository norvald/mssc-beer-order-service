package guru.sfg.beer.order.service.state;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.brewery.model.events.ValidateBeerOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidateOrderResponseListener {

    private final BeerOrderManager beerOrderManager;
    private final BeerOrderRepository beerOrderRepository;

    @Transactional
    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE)
    public void listen(ValidateBeerOrderResponse event) {
        log.debug("listen("+event+")");
        Optional<BeerOrder> optionalBeerOrder = beerOrderRepository.findById(UUID.fromString(event.getBeerOrderId()));
        optionalBeerOrder.ifPresentOrElse(beerOrder -> {
            log.debug("Validation result for beer order " + event.getBeerOrderId() + " is " + event.getIsValid());
            log.debug("beerOrder: " + beerOrder);
            beerOrderManager.validateOrder(beerOrder, event.getIsValid());
        }, () -> {
            log.error("beerOrderRepository missing beerOrder: "+event.getBeerOrderId());
        });
    }
}
