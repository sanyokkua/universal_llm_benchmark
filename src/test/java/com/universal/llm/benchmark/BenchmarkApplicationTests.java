package com.universal.llm.benchmark;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BenchmarkApplicationTests {

	@Test
	void contextLoads() {
		System.out.println("Hello World from test");
		assertTrue(true, "Success");
	}

}
