package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.common.model.BeerOrderEventEnum;
import guru.sfg.common.model.BeerOrderStatusEnum;
import org.springframework.statemachine.StateMachine;
import org.springframework.transaction.annotation.Transactional;

public interface BeerOrderManager  {
    BeerOrder newBeerOrder(BeerOrder beerOrder);

    @Transactional
    StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> validateOrder(BeerOrder beerOrder, Boolean isValid);
}
