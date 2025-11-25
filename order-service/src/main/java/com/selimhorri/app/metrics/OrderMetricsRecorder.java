package com.selimhorri.app.metrics;

import org.springframework.stereotype.Component;

import com.selimhorri.app.dto.OrderDto;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class OrderMetricsRecorder {

    private final Counter ordersCreated;
    private final Counter ordersUpdated;
    private final Counter ordersDeleted;
    private final Counter orderFeeTotal;

    public OrderMetricsRecorder(final MeterRegistry meterRegistry) {
        this.ordersCreated = Counter.builder("order_service_orders_created_total")
            .description("Cantidad total de órdenes creadas")
            .register(meterRegistry);
        this.ordersUpdated = Counter.builder("order_service_orders_updated_total")
            .description("Cantidad total de órdenes actualizadas")
            .register(meterRegistry);
        this.ordersDeleted = Counter.builder("order_service_orders_deleted_total")
            .description("Cantidad total de órdenes eliminadas")
            .register(meterRegistry);
        this.orderFeeTotal = Counter.builder("order_service_order_fee_total")
            .description("Ingresos acumulados en órdenes (USD)")
            .baseUnit("USD")
            .register(meterRegistry);
    }

    public void recordOrderCreated(final OrderDto orderDto) {
        this.ordersCreated.increment();
        this.orderFeeTotal.increment(extractAmount(orderDto));
    }

    public void recordOrderUpdated(final OrderDto orderDto) {
        this.ordersUpdated.increment();
        this.orderFeeTotal.increment(extractAmount(orderDto));
    }

    public void recordOrderDeleted() {
        this.ordersDeleted.increment();
    }

    private double extractAmount(final OrderDto orderDto) {
        if (orderDto == null || orderDto.getOrderFee() == null) {
            return 0D;
        }
        return Math.max(orderDto.getOrderFee(), 0D);
    }
}
