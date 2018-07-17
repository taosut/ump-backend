package vn.ssdc.vnpt;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.client.RestTemplate;
import vn.vnpt.ssdc.core.ObjectCache;
import vn.vnpt.ssdc.core.RedisCache;
import vn.vnpt.ssdc.event.AMQPService;
import vn.vnpt.ssdc.event.EventBus;
import vn.vnpt.ssdc.event.amqp.AMQPEventBus;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import javax.sql.DataSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Created by vietnq on 10/25/16.
 */
@Configuration
@EnableKafka
@EnableAsync
public class UmpConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    @Primary
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.redis")
    public RedisConnectionFactory redisConnectionFactory() {
        return new JedisConnectionFactory();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean
    public ObjectCache ssdcCache(RedisTemplate redisTemplate) {
        return new RedisCache(redisTemplate);
    }

    @Bean
    public RepositoryFactory repositoryFactory(DataSource dataSource, ObjectCache objectCache) {
        return new RepositoryFactory(dataSource, objectCache);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /* ========================= RABBIT MQ ============================ */
    @Bean
    @ConfigurationProperties(prefix = "spring.rabbitmq")
    public CachingConnectionFactory connectionFactory() {
        return new CachingConnectionFactory();
    }

    @Bean
    public EventBus eventBus(CachingConnectionFactory connectionFactory, ApplicationContext ctx) {
        return new AMQPEventBus(connectionFactory, ctx);
    }

    @Bean
    public AMQPService amqpService(ApplicationContext ctx) {
        return new AMQPService(ctx);
    }

    @Bean
    public JobFactory jobFactory(ApplicationContext ctx) {
        QuartzJobFactory sampleJobFactory = new QuartzJobFactory();
        sampleJobFactory.setApplicationContext(ctx);
        return sampleJobFactory;
    }

    @Bean
    Scheduler scheduler(ApplicationContext ctx) throws SchedulerException, FileNotFoundException, IOException {
        //Quartz Server Properties
        Properties prop = new Properties();
        Resource resource = ctx.getResource("classpath:quartz.properties");
        prop.load(resource.getInputStream());
        Scheduler scheduler = new StdSchedulerFactory(prop).getScheduler();
        scheduler.setJobFactory(jobFactory(ctx));
        scheduler.start();
        return scheduler;
    }

    @Value("${elasticSearchUrl}")
    public String elasticSearchUrl;

    @Bean
    public JestClient elasticSearchClient() {
        JestClientFactory jestClientFactory = new JestClientFactory();
        jestClientFactory.setHttpClientConfig(new HttpClientConfig.Builder(elasticSearchUrl)
                .multiThreaded(true)
                .build());
        return jestClientFactory.getObject();
    }

    /**
     * config for producer kafka
     */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    /**
     * config for comsumer kafka
     */
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return props;
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Value("${thread-pool.core-pool-size}")
    private String corePoolSize;

    @Value("${thread-pool.max-pool-size}")
    private String maxPoolSize;

    @Value("${thread-pool.queue-capability}")
    private String queueCapability;

    @Value("${thread-pool.timeout}")
    private String timeout;

    @Bean(name = "threadPoolTaskExecutor")
    public Executor threadPoolTaskExecutor() {
        ThreadPoolExecutor threadpool = new ThreadPoolExecutor(
                Integer.valueOf(corePoolSize),
                Integer.valueOf(maxPoolSize),
                Integer.valueOf(timeout),
                TimeUnit.SECONDS,
                new ArrayBlockingQueue(Integer.valueOf(queueCapability)),
                new ThreadPoolExecutor.CallerRunsPolicy());
        ThreadPoolMonitor monitor = new ThreadPoolMonitor(threadpool, 10);
        Thread monitorThread = new Thread(monitor);
        monitorThread.start();
        return threadpool;

    }

}
