package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.brewery.model.BeerOrderDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface BeerOrderManager  {
    BeerOrder newBeerOrder(BeerOrder beerOrder);
    @Transactional
    void validateOrder(BeerOrder beerOrder, Boolean isValid);
    void beerOrderAllocationPassed(BeerOrderDto beerOrderDto);
    void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto);
    void beerOrderAllocationFailed(BeerOrderDto beerOrderDto);
    void beerOrderPickedUp(UUID orderId);
    void cancelBeerOrder(UUID orderId);
}
