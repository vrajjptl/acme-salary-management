package com.acme.salary.config;

import com.fasterxml.jackson.databind.MapperFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonConfig {
    @Bean
    Jackson2ObjectMapperBuilderCustomizer rejectScalarCoercion() {
        return builder -> builder.featuresToDisable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
    }
}
