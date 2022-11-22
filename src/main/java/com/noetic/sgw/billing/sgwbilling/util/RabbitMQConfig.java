
package com.noetic.sgw.billing.sgwbilling.util;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    Queue retryQueue() {
        return new Queue("sgwbillingsavechargingQueue", false);
    }

    @Bean
    Queue savezongchargingQueue() {
        return new Queue("sgwbillingsavechargingZongmmlQueue", false);
    }


    @Bean
    DirectExchange exchange() {
        return new DirectExchange("direct-exchange");
    }

    @Bean
    Binding retryBinding(Queue retryQueue, DirectExchange exchange) {
        return BindingBuilder.bind(retryQueue).to(exchange).with("sgwbillingsavecharging");
    }
    @Bean
    Binding savezongchargingBinding(Queue savezongchargingQueue, DirectExchange exchange) {
        return BindingBuilder.bind(savezongchargingQueue).to(exchange).with("sgwbillingsavechargingZongmml");
    }



}