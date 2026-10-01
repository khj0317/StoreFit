package com.luggagestorage;

import org.junit.jupiter.api.Test;
import com.luggagestorage.support.TestcontainersConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LuggageStorageApplicationTests {

	@Test
	void contextLoads() {
	}

}
