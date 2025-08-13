package com.accessvault.exception;

import com.accessvault.model.enums.AuditActionType;
import com.accessvault.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Load ONLY the dummy controller, and import the advice & mock config
@WebMvcTest(controllers = DummyDeniedController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestConfig.class })
class GlobalExceptionHandlerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean AuditLogService auditLogService() { return Mockito.mock(AuditLogService.class); }
    }

    @Autowired MockMvc mvc;
    @Autowired AuditLogService auditLogService;

    @MockBean
    private com.accessvault.security.JwtFilter jwtFilter;

    @Test
    void accessDenied_isHandled_andAudited() throws Exception {
        mvc.perform(get("/dummy/denied"))
           .andExpect(status().isForbidden())
           .andExpect(content().string("Access denied"));

        Mockito.verify(auditLogService, times(1))
                .log(eq(AuditActionType.UNAUTHORIZED_ACCESS), anyString(), anyString(), anyString());
    }
}
