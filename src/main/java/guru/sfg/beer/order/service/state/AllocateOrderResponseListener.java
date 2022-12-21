package guru.sfg.beer.order.service.state;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocateOrderResponseListener {

    private final BeerOrderManager beerOrderManager;
    private final BeerOrderRepository beerOrderRepository;

    @Transactional
    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE)
    public void listen(AllocateOrderResult event) {
        log.debug("Allocation result for beer order "+event.getBeerOrderDto().getId() + " is "+event.getPendingInventory());
        if(event.getAllocationError()) {
            beerOrderManager.beerOrderAllocationFailed(event.getBeerOrderDto());
        } else if(event.getPendingInventory()) {
            beerOrderManager.beerOrderAllocationPendingInventory(event.getBeerOrderDto());
        } else {
            beerOrderManager.beerOrderAllocationPassed(event.getBeerOrderDto());
        }
    }
}
