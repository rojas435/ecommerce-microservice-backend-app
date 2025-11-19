package com.selimhorri.app.config.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class ClientConfig {

	@Value("${http.client.connect-timeout-ms:2000}")
	private int connectTimeoutMs;

	@Value("${http.client.read-timeout-ms:3000}")
	private int readTimeoutMs;

	@LoadBalanced
	@Bean
	public RestTemplate restTemplateBean() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeoutMs);
		factory.setReadTimeout(readTimeoutMs);
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setRequestFactory(factory);
		return restTemplate;
	}

}










