package com.campus.courier.config;

import com.campus.courier.entity.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.ACCEPTED, OrderStatus.CANCELLED),
            OrderStatus.ACCEPTED, Set.of(OrderStatus.PICKING, OrderStatus.CANCELLED),
            OrderStatus.PICKING, Set.of(OrderStatus.DELIVERING),
            OrderStatus.DELIVERING, Set.of(OrderStatus.COMPLETED),
            OrderStatus.COMPLETED, Set.of(),
            OrderStatus.CANCELLED, Set.of(),
            OrderStatus.ERROR, Set.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED)
    );

    public void validate(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("状态不能为空");
        }

        Set<OrderStatus> allowedTargets = VALID_TRANSITIONS.get(from);
        if (allowedTargets == null || !allowedTargets.contains(to)) {
            throw new IllegalStateException(
                    String.format("非法状态转移: %s → %s", from.getDesc(), to.getDesc())
            );
        }
    }
}
