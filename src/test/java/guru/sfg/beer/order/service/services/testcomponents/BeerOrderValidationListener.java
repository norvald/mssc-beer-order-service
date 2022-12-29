package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.ValidateOrderRequest;
import guru.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import wiremock.org.apache.commons.lang3.StringUtils;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderValidationListener {
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_QUEUE)
    public void listen(Message msg) {
        boolean isValid = true;
        ValidateOrderRequest request = (ValidateOrderRequest) msg.getPayload();
        log.debug("ValidateBeerOrderRequest: " + request);
        if (StringUtils.equals(request.getBeerOrder().getCustomerRef(), "fail-validation")) {
            isValid = false;
        }
        if (StringUtils.equals(request.getBeerOrder().getCustomerRef(), "dont-validate")) {
            return;
        }

        jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE,
                ValidateOrderResult.builder()
                        .isValid(isValid)
                        .orderId(request.getBeerOrder().getId().toString())
                        .build());
    }
}
