package com.luggagestorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LuggageStorageApplication {

	private static final Logger log = LoggerFactory.getLogger(LuggageStorageApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(LuggageStorageApplication.class, args);
	}

	@Bean
	public ApplicationRunner logDatabaseLocation(@Value("${spring.datasource.url}") String datasourceUrl) {
		return args -> log.info("=== 데이터베이스 파일 위치: {} (재부팅해도 이 경로가 항상 같아야 데이터가 유지됩니다) ===", datasourceUrl);
	}

}
