package com.aihub.config;

import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final DifyProperties difyProperties;

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, difyProperties.getConnectTimeout())
                .responseTimeout(Duration.ofMillis(difyProperties.getReadTimeout()));

        return WebClient.builder()
                .baseUrl(difyProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(difyProperties.getMaxInMemorySize()))
                .build();
    }
}