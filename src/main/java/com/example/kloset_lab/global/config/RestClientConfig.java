package com.example.kloset_lab.global.config;

import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${ai.api.base-url}")
    String baseUrl;

    /**
     * AI 서버 호출용 RestClient (커넥션 풀링 적용)
     *
     * @return RestClient
     */
    @Bean
    public RestClient restClient() {
        // 커넥션 풀 설정
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(50); // 전체 최대 커넥션 수
        connectionManager.setDefaultMaxPerRoute(50); // 호스트(AI 서버)당 최대 커넥션 수
        connectionManager.setDefaultConnectionConfig(ConnectionConfig.custom()
                .setConnectTimeout(10, TimeUnit.SECONDS)
                .setSocketTimeout(5, TimeUnit.MINUTES)
                .build());

        // 요청 설정
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(10, TimeUnit.SECONDS)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(factory)
                .build();
    }
}
