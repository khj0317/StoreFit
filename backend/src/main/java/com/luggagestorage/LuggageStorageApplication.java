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
	public ApplicationRunner logDatabaseLocation(@Value("${spring.datasource.url:(자동 연결)}") String datasourceUrl) {
		// 비밀번호 같은 쿼리 파라미터가 섞일 수 있어 ? 앞까지만 남긴다
		return args -> log.info("=== 데이터베이스: {} ===", datasourceUrl.split("\\?")[0]);
	}

}
