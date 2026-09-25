package com.allensandiego.adm.security;

import com.allensandiego.adm.domain.AuthEvent;
import com.allensandiego.adm.domain.AuthOutcome;
import com.allensandiego.adm.service.AuthEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthEventFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AuthEventService authEventService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws java.io.IOException, jakarta.servlet.ServletException {
        String username = request.getParameter("username");
        if (username == null) {
            username = "unknown";
        }

        AuthEvent event = new AuthEvent();
        event.setUsername(username);
        event.setOutcome(AuthOutcome.FAILURE);
        event.setIpAddress(getClientIp(request));
        event.setUserAgent(request.getHeader("User-Agent"));
        authEventService.record(event);

        // Record the failure event (already done above)
        // Perform proper redirect to login page with error parameter
        super.setDefaultFailureUrl("/login?error");
        super.onAuthenticationFailure(request, response, exception);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
