package com.platform.app.ticket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.app.iam.web.dto.RegisterRequest;
import com.platform.app.ticket.application.dto.AddTicketMessageCommand;
import com.platform.app.ticket.application.dto.CreateTicketCommand;
import com.platform.app.ticket.domain.model.TicketCategory;
import com.platform.app.ticket.domain.model.TicketPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class TicketIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Register a test customer and obtain token
        RegisterRequest registerRequest = RegisterRequest.builder()
                .fullName("Ticket Customer")
                .email("ticket.user@example.com")
                .password("Password123!")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        if (root.has("data") && root.get("data").has("accessToken")) {
            this.jwtToken = root.get("data").get("accessToken").asText();
        }
    }

    @Test
    void shouldCreateAndRetrieveTicketLifecycle() throws Exception {
        CreateTicketCommand createCommand = CreateTicketCommand.builder()
                .title("Database connection timeout")
                .description("Production database times out after 30 seconds under heavy traffic load.")
                .priority(TicketPriority.URGENT)
                .category(TicketCategory.TECHNICAL)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCommand)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Database connection timeout"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andReturn();

        JsonNode ticketJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long ticketId = ticketJson.get("data").get("id").asLong();

        // Add message to the ticket
        AddTicketMessageCommand messageCommand = AddTicketMessageCommand.builder()
                .content("I have uploaded the relevant query execution plan and logs.")
                .internalNote(false)
                .build();

        mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/messages")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageCommand)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("I have uploaded the relevant query execution plan and logs."));

        // Get ticket details
        mockMvc.perform(get("/api/v1/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messages.length()").value(1));

        // List tickets
        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }
}
