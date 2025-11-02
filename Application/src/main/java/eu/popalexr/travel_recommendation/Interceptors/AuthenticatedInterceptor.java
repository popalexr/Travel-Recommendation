package eu.popalexr.travel_recommendation.Interceptors;

import eu.popalexr.travel_recommendation.Constants.SessionConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;

@Component
public class AuthenticatedInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SessionConstants.AUTHENTICATED_USER_ID) != null) {
            return true;
        }

        if (isJsonRequest(request)) {
            respondJson(response, HttpStatus.UNAUTHORIZED, "Authentication required.");
        } else {
            response.sendRedirect("/login");
        }
        return false;
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String contentType = request.getHeader("Content-Type");
        return (accept != null && accept.contains("application/json"))
            || (contentType != null && contentType.contains("application/json"));
    }

    private void respondJson(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        String safeMessage = message.replace("\"", "\\\"");
        response.getWriter().write("{\"message\":\"" + safeMessage + "\"}");
    }
}
