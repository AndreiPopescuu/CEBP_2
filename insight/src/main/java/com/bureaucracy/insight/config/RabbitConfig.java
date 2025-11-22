package com.bureaucracy.insight.config;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RabbitConfig {

    @Bean
    public MessageConverter messageConverter() {
        SimpleMessageConverter converter = new SimpleMessageConverter();
        // Explicitly allow Java objects like HashMap to be read
        converter.setAllowedListPatterns(List.of("java.util.*", "java.lang.*"));
        return converter;
    }
}