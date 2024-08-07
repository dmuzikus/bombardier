package com.itmo.microservices.demo.common.metrics

import com.itmo.microservices.demo.bombardier.stages.TestStage
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics
import io.micrometer.core.instrument.util.NamedThreadFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class Metrics(private val tags: List<Tag>) {


    companion object {

        val globalRegistry = io.micrometer.core.instrument.Metrics.globalRegistry

        private const val externalCallDurationName = "http_external_duration"
        private const val timeHttpRequestLatent = "http_request_latent"
        private const val stageDurationOkName = "stage_duration_ok"
        private const val stageDurationFailName = "stage_duration_fail"
        private const val testDurationName = "test_duration"
        private const val externalSysDurationName = "external_sys_duration"
        private const val externalSysSubmitName = "external_sys_submit"
        private const val testDurationFailName = "test_duration_fail"
        private const val paymentsAmountName = "payments_amount"
        private const val paymentFinishedName = "payment_finished"
        private const val extSysChargeAmountName = "external_amount"

        val stageLabel = "stage"
        val serviceLabel = "service"

        @JvmStatic
        fun withTags(vararg labels: Pair<String, String>): Metrics {
            return Metrics(labels.map { Tag.of(it.first, it.second) })
        }

        @JvmStatic
        fun executorServiceMonitoring(executorService: ExecutorService, executorName: String) {
            ExecutorServiceMetrics.monitor(globalRegistry, executorService, executorName, listOf())
        }

        private val executor = Executors.newFixedThreadPool(16, NamedThreadFactory("metrics-dispatcher")).also {
            executorServiceMonitoring(it, "metrics-dispatcher")
        }
    }

    fun stageDurationRecord(timeMs: Long, state: TestStage.TestContinuationType) {
        executor.submit {
            if (state.iSFailState()) {
                Timer.builder(stageDurationFailName)
                    .publishPercentiles(0.95)
                    .tags(tags)
                    .register(globalRegistry)
                    .record(timeMs, TimeUnit.MILLISECONDS)
            } else {
                Timer.builder(stageDurationOkName)
                    .publishPercentiles(0.5)
                    .publishPercentiles(0.95)
                    .tags(tags)
                    .register(globalRegistry)
                    .record(timeMs, TimeUnit.MILLISECONDS)
            }
        }
    }

    fun testDurationRecord(timeMs: Long) {
        executor.submit {
            Timer.builder(testDurationName)
                .publishPercentiles(0.95)
                .tags(tags)
                .register(globalRegistry)
                .record(timeMs, TimeUnit.MILLISECONDS)
        }
    }

    fun externalSysDurationRecord(timeMs: Long) {
        executor.submit {
            Timer.builder(externalSysDurationName)
                .publishPercentiles(0.95)
                .tags(tags)
                .register(globalRegistry)
                .record(timeMs, TimeUnit.MILLISECONDS)
        }
    }

    fun externalSysRequestSubmitted() {
        executor.submit {
            Counter.builder(externalSysSubmitName)
                .tags(tags)
                .register(globalRegistry)
                .increment()
        }
    }

    fun paymentsAmountRecord(amount: Int) {
        executor.submit {
            Counter.builder(paymentsAmountName)
                .tags(tags)
                .register(globalRegistry)
                .increment(amount.toDouble())
        }
    }

    fun paymentFinished() {
        executor.submit {
            Counter.builder(paymentFinishedName)
                .tags(tags)
                .register(globalRegistry)
                .increment()
        }
    }

    fun externalSysChargeAmountRecord(amount: Int) {
        executor.submit {
            Counter.builder(extSysChargeAmountName)
                .tags(tags)
                .register(globalRegistry)
                .increment(amount.toDouble())
        }
    }

    fun externalMethodDurationRecord(timeMs: Long) {
        executor.submit {
            Timer.builder(externalCallDurationName)
                .publishPercentiles(0.95)
                .tags(tags)
                .register(globalRegistry).record(timeMs, TimeUnit.MILLISECONDS)
        }
    }

    fun timeHttpRequestLatent(timeMs: Long) {
        executor.submit {
            Timer.builder(timeHttpRequestLatent)
                .publishPercentiles(0.95)
                .tags(tags)
                .register(globalRegistry).record(timeMs, TimeUnit.MILLISECONDS)
        }
    }
}