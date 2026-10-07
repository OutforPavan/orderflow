package com.outforpavan.orderflow.orders.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "saga.enabled", havingValue = "true", matchIfMissing = true)
public class SagaScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(SagaScheduler.class);
    private final SagaWorker worker;

    public SagaScheduler(SagaWorker worker) { this.worker = worker; }

    @Scheduled(fixedDelayString = "${saga.poll-interval:500}")
    public void run() {
        try { worker.processOnce(); }
        catch (RuntimeException unavailable) {
            // A failed coordinator write is recovered by an expired lease and participant idempotency.
            LOG.warn("Saga storage unavailable; durable work will resume on recovery ({})",
                    unavailable.getClass().getSimpleName());
        }
    }
}
