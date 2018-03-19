package com.adyen.mirakl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CucumberConfiguration {

    @Bean
    public Map<String, Object> cucumberMap() {
        return new HashMap<>();
    }
}
