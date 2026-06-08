package com.xxxx.event.security;

import com.xxxx.event.config.JwtSecurityConfig;
import com.xxxx.event.controller.EventController;
import com.xxxx.event.controller.dto.response.EventResponse;
import com.xxxx.event.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class)
@Import(JwtSecurityConfig.class)
@TestPropertySource(properties = {
        "gateway.jwt.secret=test-jwt-key-must-be-at-least-32-bytes",
        "gateway.jwt.issuer=xxxx-user-service"
})
class EventPublicEndpointsWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @Test
    void allowsPublicEventListingWithoutJwt() throws Exception {
        when(eventService.getAllEvents(isNull(), isNull(), isNull()))
                .thenReturn(List.of(EventResponse.builder().id(1L).name("Concert").status("PUBLISHED").build()));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Concert"));
    }
}