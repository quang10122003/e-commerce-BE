package shop.shop.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RabbitMqConfig {

    @Value("${app.rabbitMq.exchange}")
     String exchangeName;

    @Value("${app.rabbitMq.orderSepayCheckQueue}")
     String orderSepayCheckQueue;

    @Value("${app.rabbitMq.orderSepayCheckRoutingKey}")
     String orderSepayCheckRoutingKey;
     
    @Value("${app.rabbitMq.order-sepay-delay-ttl-ms}")
    int timeDelayQueueSepay;

    @Value("${app.rabbitMq.orderSepayDelayQueue}")
     String orderSepayDelayQueue;

     @Value("${app.rabbitMq.orderSepayDelayRoutingKey}")
     String orderSepayDelayRoutingKey;

    @Bean
    public Queue orderSepayCheckQueue() {
        return QueueBuilder.durable(orderSepayCheckQueue).build();
        // return new Queue(orderSepayCheckQueue, true);
    }

    // cấu hình Queue delay để giữ sau 1 ngày thì chuyển sang Exchange dựa vào routingkey chuyển vào queue để xử lý
    @Bean
    public Queue orderSepayDelayQueue() {
        return QueueBuilder.durable(orderSepayDelayQueue).ttl(
                timeDelayQueueSepay)
                .deadLetterExchange(exchangeName).deadLetterRoutingKey(orderSepayCheckRoutingKey).build();
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    // cấu hình buiding để định tuyến exchange điều hướng đúng vào queue khi có tin
    // nhắn đến bằng routingkey
    @Bean
    public Binding orderSepayBinding() {
        return BindingBuilder
                .bind(orderSepayCheckQueue())
                .to(exchange())
                .with(orderSepayCheckRoutingKey);
    }

    @Bean
    public Binding orderSepayBindingDelay(){
        return BindingBuilder.bind(orderSepayDelayQueue()).to(exchange()).with(orderSepayDelayRoutingKey);
    }

    // cấu hình cho gửi đc json vào qeue
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
