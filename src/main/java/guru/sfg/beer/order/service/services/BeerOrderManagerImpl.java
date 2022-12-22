package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.state.BeerOrderStateChangeInterceptor;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.BeerOrderEventEnum;
import guru.sfg.brewery.model.BeerOrderStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
@Service
public class BeerOrderManagerImpl implements BeerOrderManager {
    public static final String BEERORDER_ID_HEADER = "beerorder_id";
    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;

    @Transactional
    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);

        log.debug("newBeerOrder Saving: " + beerOrder);
        BeerOrder savedBeerOrder = beerOrderRepository.saveAndFlush(beerOrder);
        log.debug("savedBeerOrder: " + savedBeerOrder);
        log.debug("savedBeerOrderId: " + savedBeerOrder.getId());
        sendBeerOrderEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Transactional
    @Override
    public void validateOrder(BeerOrder beerOrder, Boolean isValid) {
        log.debug("validateOrder beerOrder: " + beerOrder + "->" + isValid);
        //StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = build(beerOrder);
        if (isValid) {
            log.debug("Trigger VALIDATION_PASSED");
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED);
            waitForStatus(beerOrder.getId(), BeerOrderStatusEnum.VALIDATED);
            Optional<BeerOrder> optionalBeerOrder = beerOrderRepository.findById(beerOrder.getId());
            optionalBeerOrder.ifPresentOrElse(validatedOrder -> {
                sendBeerOrderEvent(validatedOrder, BeerOrderEventEnum.ALLOCATE_ORDER);
            }, () -> {
                log.warn("Missing beerOrder");
            });
        } else {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_FAILED);
        }
        //return sm;
    }

    @Transactional
    @Override
    public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto) {
        Optional<BeerOrder> optionalBeerOrder = beerOrderRepository.findById(beerOrderDto.getId());
        optionalBeerOrder.ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_SUCCESS);
            waitForStatus(beerOrder.getId(), BeerOrderStatusEnum.ALLOCATED);
            log.debug("beerOrderAllocationPassed: ALLOCATED");
            updateAllocatedQty(beerOrderDto);
        }, () -> {
            log.warn("Missing beerOrder");
        });
    }

    @Override
    public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto) {
        Optional<BeerOrder> optionalBeerOrder = beerOrderRepository.findById(beerOrderDto.getId());
        optionalBeerOrder.ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);
            waitForStatus(beerOrder.getId(), BeerOrderStatusEnum.PENDING_INVENTORY);
            log.debug("beerOrderAllocationPassed: PENDING_INVENTORY");
            updateAllocatedQty(beerOrderDto);
        }, () -> {
            log.warn("Missing beerOrder");
        });
    }

    @Override
    public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto) {
        Optional<BeerOrder> optionalBeerOrder = beerOrderRepository.findById(beerOrderDto.getId());
        optionalBeerOrder.ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_FAILED);
        }, () -> {
            log.warn("Missing beerOrder");
        });
    }

    private void updateAllocatedQty(BeerOrderDto beerOrderDto) {
        Optional<BeerOrder> optionalAllocatedOrder = beerOrderRepository.findById(beerOrderDto.getId());

        optionalAllocatedOrder.ifPresentOrElse(allocatedOrder -> {
            allocatedOrder.getBeerOrderLines().forEach(beerOrderLine -> {
                beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
                    if (beerOrderLine.getId().equals(beerOrderLineDto.getId())) {
                        beerOrderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
                    }
                });
            });
            log.debug("updateAllocatedQty Saving: " + allocatedOrder);
            BeerOrder savedBeerOrder = beerOrderRepository.saveAndFlush(allocatedOrder);
            log.debug("savedBeerOrder: " + savedBeerOrder);
            log.debug("savedBeerOrderId: " + savedBeerOrder.getId());
        }, () -> {
            log.warn("Missing beerOrder");
        });
    }

    @Override
    @Transactional
    public void beerOrderPickedUp(UUID orderId) {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(orderId);
        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.BEERORDER_PICKED_UP);
        }, () -> log.error("Order Not Found. Id: " + orderId));
    }

    @Override
    @Transactional
    public void cancelBeerOrder(UUID orderId) {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(orderId);
        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.CANCEL_ORDER);
        }, () -> log.error("Order cancelled. Id: " + orderId));
    }


    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum eventEnum) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = build(beerOrder);
        log.debug("beerOrder: " + beerOrder + " -> " + eventEnum);
        Message msg = MessageBuilder.withPayload(eventEnum)
                .setHeader(BEERORDER_ID_HEADER, beerOrder.getId().toString())
                .build();

        sm.sendEvent(msg);
    }

    private void waitForStatus(UUID beerOrderId, BeerOrderStatusEnum statusEnum) {
        AtomicBoolean found = new AtomicBoolean(false);
        AtomicInteger loopCount = new AtomicInteger(0);

        while (!found.get()) {
            if (loopCount.incrementAndGet() > 10) {
                found.set(true);
                log.debug("Loop retries exceeded");
            }

            beerOrderRepository.findById(beerOrderId).ifPresentOrElse(beerOrder -> {
                if (beerOrder.getOrderStatus().equals(statusEnum)) {
                    found.set(true);
                    log.debug("Order found");
                } else {
                    log.debug("Order Status not equal. Expected: " + statusEnum.name() + " Found: " + beerOrder.getOrderStatus().name());
                }
            }, () -> {
                log.debug("Order Id not found");
            });

            if (!found.get()) {
                try {
                    log.debug("Sleeping for retry");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
        }
    }

    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = stateMachineFactory.getStateMachine(beerOrder.getId());

        sm.stop();
        sm.getStateMachineAccessor().doWithAllRegions(sma -> {
            sma.addStateMachineInterceptor(beerOrderStateChangeInterceptor);
            sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null));
        });
        sm.start();

        return sm;
    }
}
