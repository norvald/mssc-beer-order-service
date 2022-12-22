package guru.sfg.beer.order.service.state.actions;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import guru.sfg.brewery.model.BeerOrderEventEnum;
import guru.sfg.brewery.model.BeerOrderStatusEnum;
import guru.sfg.brewery.model.events.ValidateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class ValidateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {
    private final JmsTemplate jmsTemplate;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;

    @Transactional
    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> context) {
        log.debug("ValidateOrderAction triggered");
        String beerOrderId = (String) context.getMessage().getHeaders().get(BeerOrderManagerImpl.BEERORDER_ID_HEADER);
        Optional<BeerOrder> optionalBeerOrder = beerOrderRepository.findById(UUID.fromString(beerOrderId));
        optionalBeerOrder.ifPresentOrElse(beerOrder -> {
            ValidateOrderRequest validateOrderRequest = ValidateOrderRequest.builder()
                    .beerOrder(beerOrderMapper.beerOrderToDto(beerOrder))
                    .build();

            log.debug("Sending ValidateOrderRequest: "+validateOrderRequest);
            jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_QUEUE, validateOrderRequest);
        }, () -> {
            log.error("beerOrderRepository missing beerOrder: " + beerOrderId);
        });
    }
}
