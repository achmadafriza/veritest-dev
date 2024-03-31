package com.veriopt.veritest.isabelle.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.veriopt.veritest.isabelle.TaskDeserializer;
import com.veriopt.veritest.isabelle.response.*;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@ConfigurationPropertiesScan
public class IsabelleConfig {
    @Bean("isabelleMapper")
    public ObjectMapper isabelleMapper() {
        TaskDeserializer deserializer = new TaskDeserializer();
        deserializer.register(SessionStartResponse.class, "session_id");
        deserializer.register(IsabelleGenericError.class, "kind", "message");
        deserializer.register(SessionStopResponse.class, "ok", "return_code");
        deserializer.register(SessionStopError.class, "ok", "return_code", "kind", "message");
        deserializer.register(TheoryResponse.class, "nodes");

        SimpleModule module = new SimpleModule("PolymorphicTaskDeserializerModule");
        module.addDeserializer(Task.class, deserializer);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.registerModule(module);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return mapper;
    }

    @Bean("ioExecutor")
    public Executor executor() {
        return Executors.newCachedThreadPool();
    }
}
