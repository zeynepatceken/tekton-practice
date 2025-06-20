package com.services.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(CounterController.class)
class CounterControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CounterController counterController;

    @BeforeEach
    void setUp() {
        counterController.resetCounters();
    }

    @Test
    void healthEndpoint_ShouldReturnOK() throws Exception {
        mockMvc.perform(get("/health"))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$.status", is("OK")));
    }

    @Test
    void indexEndpoint_ShouldReturnServiceInfo() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$.status", is(200)))
               .andExpect(jsonPath("$.message", is("Hit Counter Service")))
               .andExpect(jsonPath("$.version", is("1.0.0")))
               .andExpect(jsonPath("$.url", containsString("/counters")));
    }

    @Test
    void createCounter_ShouldCreateNewCounter() throws Exception {
        String counterName = "test-counter";
        mockMvc.perform(post("/counters/{name}", counterName))
               .andExpect(status().isCreated())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$.name", is(counterName)))
               .andExpect(jsonPath("$.counter", is(0)))
               .andExpect(header().exists("Location"));
    }

    @Test
    void createCounter_ShouldReturnConflict_WhenCounterExists() throws Exception {
        String counterName = "duplicate-counter";
        
        // Create first counter
        mockMvc.perform(post("/counters/{name}", counterName))
               .andExpect(status().isCreated());

        // Try to create duplicate
        mockMvc.perform(post("/counters/{name}", counterName))
               .andExpect(status().isConflict())
               .andExpect(jsonPath("$.error", containsString("already exists")));
    }

    @Test
    void listCounters_ShouldReturnEmptyList_WhenNoCounters() throws Exception {
        mockMvc.perform(get("/counters"))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listCounters_ShouldReturnAllCounters() throws Exception {
        // Create some counters
        mockMvc.perform(post("/counters/counter1"));
        mockMvc.perform(post("/counters/counter2"));

        mockMvc.perform(get("/counters"))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$", hasSize(2)))
               .andExpect(jsonPath("$[*].name", containsInAnyOrder("counter1", "counter2")))
               .andExpect(jsonPath("$[*].counter", everyItem(is(0))));
    }

    @Test
    void readCounter_ShouldReturnCounter_WhenExists() throws Exception {
        String counterName = "test-counter";
        mockMvc.perform(post("/counters/{name}", counterName));

        mockMvc.perform(get("/counters/{name}", counterName))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$.name", is(counterName)))
               .andExpect(jsonPath("$.counter", is(0)));
    }

    @Test
    void readCounter_ShouldReturnNotFound_WhenCounterDoesNotExist() throws Exception {
        mockMvc.perform(get("/counters/nonexistent"))
               .andExpect(status().isNotFound())
               .andExpect(jsonPath("$.error", containsString("does not exist")));
    }

    @Test
    void updateCounter_ShouldIncrementCounter() throws Exception {
        String counterName = "test-counter";
        mockMvc.perform(post("/counters/{name}", counterName));

        mockMvc.perform(put("/counters/{name}", counterName))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$.name", is(counterName)))
               .andExpect(jsonPath("$.counter", is(1)));
    }

    @Test
    void updateCounter_ShouldReturnNotFound_WhenCounterDoesNotExist() throws Exception {
        mockMvc.perform(put("/counters/nonexistent"))
               .andExpect(status().isNotFound())
               .andExpect(jsonPath("$.error", containsString("does not exist")));
    }

    @Test
    void deleteCounter_ShouldReturnNoContent() throws Exception {
        String counterName = "test-counter";
        mockMvc.perform(post("/counters/{name}", counterName));

        mockMvc.perform(delete("/counters/{name}", counterName))
               .andExpect(status().isNoContent());

        mockMvc.perform(get("/counters/{name}", counterName))
               .andExpect(status().isNotFound());
    }

    @Test
    void deleteCounter_ShouldReturnNoContent_WhenCounterDoesNotExist() throws Exception {
        mockMvc.perform(delete("/counters/nonexistent"))
               .andExpect(status().isNoContent());
    }

    @Test
    void counterLifecycle_ShouldWorkCorrectly() throws Exception {
        String counterName = "lifecycle-counter";

        // Create counter
        mockMvc.perform(post("/counters/{name}", counterName))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.counter", is(0)));

        // Update counter multiple times
        mockMvc.perform(put("/counters/{name}", counterName))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.counter", is(1)));

        mockMvc.perform(put("/counters/{name}", counterName))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.counter", is(2)));

        // Read counter
        mockMvc.perform(get("/counters/{name}", counterName))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.counter", is(2)));

        // Delete counter
        mockMvc.perform(delete("/counters/{name}", counterName))
               .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/counters/{name}", counterName))
               .andExpect(status().isNotFound());
    }
}