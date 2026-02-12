package com.arrive.hometask.config

import com.arrive.hometask.listener.model.ParkingEvent
import com.arrive.hometask.listener.ParkingEventListener
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.KafkaListenerErrorHandler
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.util.backoff.FixedBackOff

/**
 * Kafka Consumer Configuration
 *
 * Configures Kafka consumer for parking events with:
 * - JSON deserializer for ParkingEvent
 * - Manual acknowledgment mode
 * - Error handling deserializer
 */
@Configuration
@EnableKafka
class KafkaConsumerConfig {

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Value("\${spring.kafka.consumer.group-id}")
    private lateinit var groupId: String

    @Primary
    @Bean
    fun consumerFactory(): ConsumerFactory<String, ParkingEvent> {
        val configProps = mapOf(
            JsonDeserializer.USE_TYPE_INFO_HEADERS to false,
            JsonDeserializer.VALUE_DEFAULT_TYPE to ParkingEvent::class.java,
            JsonDeserializer.TRUSTED_PACKAGES to "com.arrive.hometask.listener",
            ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to JsonDeserializer::class.java,
            ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS to StringDeserializer::class.java,
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            // Additional consumer configs for reliability
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 50,
            ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG to 500000,
            ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG to 100000,
            ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG to 10000
        )
        return DefaultKafkaConsumerFactory(configProps)
    }

    @Bean
    fun listenerErrorHandler(): KafkaListenerErrorHandler {
        val logger = LoggerFactory.getLogger(ParkingEventListener::class.java)
        return KafkaListenerErrorHandler { message, exception ->
            // todo implement more useful error handling
            logger.error("Error processing message: ${message}", exception)
            return@KafkaListenerErrorHandler "some value"
        }
    }

    @Primary
    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, ParkingEvent> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, ParkingEvent>()
        factory.consumerFactory = consumerFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        factory.setCommonErrorHandler(DefaultErrorHandler(FixedBackOff(1000, 2)))
        return factory
    }
}
