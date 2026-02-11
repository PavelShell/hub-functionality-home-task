package com.arrive.hometask.config

import com.arrive.hometask.listener.ParkingEventListener
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.KafkaListenerErrorHandler
import org.springframework.kafka.support.serializer.DeserializationException
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
    fun consumerFactory(): ConsumerFactory<String?, String?> {
        val configProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            JsonDeserializer.USE_TYPE_INFO_HEADERS to false,
            // Additional consumer configs for reliability
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 50,
            ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG to 500000,
            ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG to 100000,
            ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG to 10000
        )
        return DefaultKafkaConsumerFactory(configProps)
    }

//    @Bean
//    fun producerFactory(): ProducerFactory<String, String> {
//        val props = mapOf(
//            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
//            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
//            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java
//        )
//        return DefaultKafkaProducerFactory(props)
//    }
//
//    @Bean
//    fun template(): KafkaTemplate<String, String> = KafkaTemplate(producerFactory())
//
//    @Bean
//    fun deadLetterPublishingRecoverer(template: KafkaTemplate<String, String>): DeadLetterPublishingRecoverer {
//        return DeadLetterPublishingRecoverer(template) { record, _ ->
//            TopicPartition(record.topic() + ".DLT", record.partition())
//        }
//    }

//    @Bean
//    fun parkingEventsErrorHandler1(
//        deadLetterPublishingRecoverer: DeadLetterPublishingRecoverer
//    ) = DefaultErrorHandler(deadLetterPublishingRecoverer, FixedBackOff(1000L, 2L))
//        .apply { addNotRetryableExceptions(IllegalArgumentException::class.java, DeserializationException::class.java) }

    @Bean
    fun parkingEventsErrorHandler(): KafkaListenerErrorHandler {
        val logger = LoggerFactory.getLogger(ParkingEventListener::class.java)
        return KafkaListenerErrorHandler { message, exception ->
            // todo implement more useful error handling
            logger.error("Error processing message: ${message}", exception)
            return@KafkaListenerErrorHandler "some value"
        }
    }

    @Primary
    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        factory.setCommonErrorHandler(DefaultErrorHandler(FixedBackOff(1000, 2)))
        return factory
    }
//
//    @Bean
//    fun parkingEventsDltTopic(): NewTopic = NewTopic("parking.events.DLT", 1, 1)
}
