package com.adyen.mirakl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class BeforeTesterConfig {


    @Bean
    public Map<String, String> cucumberStateThing(){
        return new HashMap<>();
    }



}
