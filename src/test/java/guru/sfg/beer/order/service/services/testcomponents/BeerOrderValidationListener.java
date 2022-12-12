package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.common.events.ValidateBeerOrderRequest;
import guru.sfg.common.events.ValidateBeerOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderValidationListener {
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_QUEUE)
    public void listen(Message msg) {
        ValidateBeerOrderRequest request = (ValidateBeerOrderRequest) msg.getPayload();
        log.debug("ValidateBeerOrderRequest: "+request);
        jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESULT_QUEUE,
                ValidateBeerOrderResponse.builder()
                        .isValid(true)
                        .beerOrderId(request.getBeerOrderDto().getId().toString())
                        .build());
    }
}
