package guru.sfg.beer.order.service.state;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.brewery.model.BeerOrderEventEnum;
import guru.sfg.brewery.model.BeerOrderStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderStateChangeInterceptor extends StateMachineInterceptorAdapter<BeerOrderStatusEnum, BeerOrderEventEnum> {
    private final BeerOrderRepository beerOrderRepository;

    @Override
    public void preStateChange(State<BeerOrderStatusEnum, BeerOrderEventEnum> state, Message<BeerOrderEventEnum> message,
                               Transition<BeerOrderStatusEnum, BeerOrderEventEnum> transition, StateMachine<BeerOrderStatusEnum,
            BeerOrderEventEnum> stateMachine, StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> rootStateMachine) {

            Optional.ofNullable(message)
                    .flatMap(msg -> Optional.ofNullable((String) msg.getHeaders().getOrDefault(BeerOrderManagerImpl.BEERORDER_ID_HEADER, " ")))
                    .ifPresent(beerOrderId -> {
                        log.debug("Saving state for orderId: " + beerOrderId + "Status: " + state.getId());

                        BeerOrder beerOrder = beerOrderRepository.getById(UUID.fromString(beerOrderId));
                        beerOrder.setOrderStatus(state.getId());
                        log.debug("Saving: " + beerOrder);
                        BeerOrder savedBeerOrder = beerOrderRepository.saveAndFlush(beerOrder);
                        log.debug("savedBeerOrder: " + savedBeerOrder);
                        log.debug("savedBeerOrderId: " + savedBeerOrder.getId());
                    });

    }
}
