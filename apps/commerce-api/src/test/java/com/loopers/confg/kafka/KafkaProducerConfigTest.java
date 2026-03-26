package com.loopers.confg.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
    "spring.kafka.listener.auto-startup=false"
})
class KafkaProducerConfigTest {

    @Autowired
    private ProducerFactory<Object, Object> producerFactory;

    @DisplayName("Kafka Producer는 acks=all로 설정되어야 한다.")
    @Test
    void producerShouldHaveAcksAll() {
        Map<String, Object> configs = producerFactory.getConfigurationProperties();
        assertThat(String.valueOf(configs.get("acks"))).isEqualTo("all");
    }

    @DisplayName("Kafka Producer는 enable.idempotence=true로 설정되어야 한다.")
    @Test
    void producerShouldHaveIdempotenceEnabled() {
        Map<String, Object> configs = producerFactory.getConfigurationProperties();
        assertThat(String.valueOf(configs.get("enable.idempotence"))).isEqualTo("true");
    }

    @DisplayName("Kafka Producer는 max.in.flight.requests.per.connection=5로 설정되어야 한다.")
    @Test
    void producerShouldHaveMaxInFlightFive() {
        Map<String, Object> configs = producerFactory.getConfigurationProperties();
        assertThat(String.valueOf(configs.get("max.in.flight.requests.per.connection"))).isEqualTo("5");
    }
}
