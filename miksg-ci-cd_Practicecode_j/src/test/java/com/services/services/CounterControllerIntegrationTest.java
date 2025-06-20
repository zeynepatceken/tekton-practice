package com.services.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.main.allow-bean-definition-overriding=true"
    }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CounterControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CounterController counterController;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = String.format("http://localhost:%d", port);
        counterController.resetCounters();
    }

    @Test
    void testHealthEndpoint() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            baseUrl + "/health",
            Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OK", response.getBody().get("status"));
    }

    @Test
    void testCreateCounterSuccess() {
        String counterName = "test-counter";
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/counters/" + counterName,
            null,
            Map.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getHeaders().getLocation());
        assertEquals(counterName, response.getBody().get("name"));
        assertEquals(0, ((Number) response.getBody().get("counter")).intValue());
    }

    @Test
    void testCreateCounterDuplicate() {
        String counterName = "duplicate-counter";
        
        // Create first counter
        restTemplate.postForEntity(
            baseUrl + "/counters/" + counterName,
            null,
            Map.class
        );

        // Try to create duplicate
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/counters/" + counterName,
            null,
            Map.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().get("error").toString()
            .contains("Counter duplicate-counter already exists"));
    }

    @Test
    void testListCounters() {
        // Create test counters
        restTemplate.postForEntity(
            baseUrl + "/counters/counter1",
            null,
            Map.class
        );
        restTemplate.postForEntity(
            baseUrl + "/counters/counter2",
            null,
            Map.class
        );

        ResponseEntity<List> response = restTemplate.getForEntity(
            baseUrl + "/counters",
            List.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void testCompleteCounterLifecycle() {
        String counterName = "lifecycle-counter";

        // Create counter
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
            baseUrl + "/counters/" + counterName,
            null,
            Map.class
        );
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        // Update counter
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        
        ResponseEntity<Map> updateResponse = restTemplate.exchange(
            baseUrl + "/counters/" + counterName,
            HttpMethod.PUT,
            requestEntity,
            Map.class
        );
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals(1, ((Number) updateResponse.getBody().get("counter")).intValue());

        // Delete counter
        restTemplate.delete(baseUrl + "/counters/" + counterName);

        // Verify deletion
        ResponseEntity<Map> verifyResponse = restTemplate.getForEntity(
            baseUrl + "/counters/" + counterName,
            Map.class
        );
        assertEquals(HttpStatus.NOT_FOUND, verifyResponse.getStatusCode());
    }
}