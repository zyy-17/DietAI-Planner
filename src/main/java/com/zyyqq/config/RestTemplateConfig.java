package com.zyyqq.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@Slf4j
public class RestTemplateConfig {

    @Value("${ai-service.connect-timeout:10}")
    private int connectTimeout;

    @Value("${ai-service.read-timeout:120}")
    private int readTimeout;

    @Value("${ai-service.pool.max-total:50}")
    private int poolMaxTotal;

    @Value("${ai-service.pool.max-per-route:20}")
    private int poolMaxPerRoute;

    @Bean
    public RestTemplate aiRestTemplate() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(poolMaxTotal);
        connectionManager.setDefaultMaxPerRoute(poolMaxPerRoute);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(connectTimeout))
                .setResponseTimeout(Timeout.ofSeconds(readTimeout))
                .build();

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy() {
                    @Override
                    public TimeValue getKeepAliveDuration(HttpResponse response, HttpContext context) {
                        TimeValue duration = super.getKeepAliveDuration(response, context);
                        if (duration == null || duration.getDuration() <= 0) {
                            return TimeValue.ofSeconds(30);
                        }
                        return duration;
                    }
                })
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(60))
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        RestTemplate restTemplate = new RestTemplate(factory);

        log.info("RestTemplate连接池初始化完成: maxTotal={}, maxPerRoute={}, connectTimeout={}s, readTimeout={}s",
                poolMaxTotal, poolMaxPerRoute, connectTimeout, readTimeout);

        return restTemplate;
    }
}