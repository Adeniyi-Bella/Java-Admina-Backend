package com.admina.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"app.security.entra.validate-on-startup=false",
		"app.security.entra.tenant-id=test-tenant",
		"app.security.entra.client-id=test-client",
		"app.security.entra.client-secret=test-secret"
})
class ApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
