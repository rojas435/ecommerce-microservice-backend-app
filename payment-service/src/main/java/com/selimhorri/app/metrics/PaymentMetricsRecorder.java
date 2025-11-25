package com.selimhorri.app.metrics;

import org.springframework.stereotype.Component;

import com.selimhorri.app.dto.PaymentDto;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class PaymentMetricsRecorder {

    private final Counter paymentsTotal;
    private final Counter paymentsFailed;
    private final Counter paymentsAmount;
    private final Counter paymentsDeleted;

    public PaymentMetricsRecorder(final MeterRegistry meterRegistry) {
        this.paymentsTotal = Counter.builder("payment_service_payments_total")
            .description("Cantidad total de pagos procesados")
            .register(meterRegistry);
        this.paymentsFailed = Counter.builder("payment_service_payments_failed_total")
            .description("Cantidad total de pagos fallidos")
            .register(meterRegistry);
        this.paymentsAmount = Counter.builder("payment_service_payments_amount_total")
            .description("Monto total procesado por el servicio de pagos (USD)")
            .baseUnit("USD")
            .register(meterRegistry);
        this.paymentsDeleted = Counter.builder("payment_service_payments_deleted_total")
            .description("Pagos eliminados o revertidos")
            .register(meterRegistry);
    }

    public void recordPayment(final PaymentDto sourcePayload, final PaymentDto persistedDto) {
        this.paymentsTotal.increment();
        final PaymentDto metricSource = sourcePayload != null ? sourcePayload : persistedDto;
        final double amount = extractAmount(metricSource);
        if (amount > 0D) {
            this.paymentsAmount.increment(amount);
        }
        if (metricSource == null || !Boolean.TRUE.equals(metricSource.getIsPayed())) {
            this.paymentsFailed.increment();
        }
    }

    public void recordPaymentDeletion() {
        this.paymentsDeleted.increment();
    }

    private double extractAmount(final PaymentDto paymentDto) {
        if (paymentDto == null || paymentDto.getOrderDto() == null || paymentDto.getOrderDto().getOrderFee() == null) {
            return 0D;
        }
        return Math.max(paymentDto.getOrderDto().getOrderFee(), 0D);
    }
}
