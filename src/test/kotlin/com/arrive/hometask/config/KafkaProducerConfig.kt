package com.arrive.hometask.config

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

/**
 * Kafka Producer Configuration for Tests
 *
 * Provides KafkaTemplate bean for sending test messages to embedded Kafka broker.
 */
@TestConfiguration
@Profile("test")
class KafkaProducerConfig {

    @Value("\${spring.kafka.bootstrap-servers:localhost:9093}")
    private lateinit var bootstrapServers: String

    @Bean
    fun producerFactory(): ProducerFactory<String?, String?> {
        val props: MutableMap<String, Any> = HashMap()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.CLIENT_ID_CONFIG] = "hub-functionality-home-task-producer"
        props[ProducerConfig.MAX_BLOCK_MS_CONFIG] = 5000
        return DefaultKafkaProducerFactory(props)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String?, String?>? {
        return KafkaTemplate(producerFactory())
    }
}
