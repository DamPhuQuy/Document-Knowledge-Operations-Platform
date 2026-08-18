package com.platform.app.ai.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.platform.app.ai.grpc.AiServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {

    @Bean(destroyMethod = "shutdown")
    public ManagedChannel managedChannel(LlmProperties properties) {
        return ManagedChannelBuilder.forAddress(properties.host(), properties.port())
            .usePlaintext()
            .build();
    }

    @Bean
    public AiServiceGrpc.AiServiceBlockingStub aiServiceBlockingStub(ManagedChannel channel) {
        return AiServiceGrpc.newBlockingStub(channel);
    }
}
