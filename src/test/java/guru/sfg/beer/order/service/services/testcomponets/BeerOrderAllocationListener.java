package guru.sfg.beer.order.service.services.testcomponets;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderAllocationListener {
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(Message msg) {
        boolean isAllocationError = false;
        boolean isPendingInventory = false;
        int diff;
        AllocateOrderRequest request = (AllocateOrderRequest) msg.getPayload();
        if (StringUtils.equals(request.getBeerOrderDto().getCustomerRef(), "fail-allocation")) {
            isAllocationError = true;
        }
        if (StringUtils.equals(request.getBeerOrderDto().getCustomerRef(), "partial-allocation")) {
            isPendingInventory = true;
            diff = 1;
        } else {
            diff = 0;
        }
        if(StringUtils.equals(request.getBeerOrderDto().getCustomerRef(),"dont-allocate")) {
            return;
        }
        request.getBeerOrderDto().getBeerOrderLines().forEach(beerOrderLineDto -> {
            beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity() - diff);
        });
        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE, AllocateOrderResult.builder()
                .beerOrderDto(request.getBeerOrderDto())
                .pendingInventory(isPendingInventory)
                .allocationError(isAllocationError)
                .build());
    }
}
