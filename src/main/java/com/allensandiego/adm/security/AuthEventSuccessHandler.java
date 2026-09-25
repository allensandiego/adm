package com.allensandiego.adm.security;

import com.allensandiego.adm.domain.AuthEvent;
import com.allensandiego.adm.domain.AuthOutcome;
import com.allensandiego.adm.service.AuthEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthEventSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final AuthEventService authEventService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws java.io.IOException, jakarta.servlet.ServletException {
        AuthEvent event = new AuthEvent();
        event.setUsername(authentication.getName());
        event.setOutcome(AuthOutcome.SUCCESS);
        event.setIpAddress(getClientIp(request));
        event.setUserAgent(request.getHeader("User-Agent"));
        authEventService.record(event);

        super.onAuthenticationSuccess(request, response, authentication);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
