package guru.sfg.beer.order.service.state.actions;

import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.brewery.model.BeerOrderEventEnum;
import guru.sfg.brewery.model.BeerOrderStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ValidationFailureAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> context) {
        String beerOrderId = (String) context.getMessage().getHeaders().get(BeerOrderManagerImpl.BEERORDER_ID_HEADER);
        log.error("Compensation Transaction - Validation failed: "+beerOrderId);
    }
}
