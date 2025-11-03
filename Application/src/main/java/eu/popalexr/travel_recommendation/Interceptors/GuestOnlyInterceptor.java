package eu.popalexr.travel_recommendation.Interceptors;

import eu.popalexr.travel_recommendation.Constants.SessionConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class GuestOnlyInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        Object uid = request.getAttribute(SessionConstants.AUTHENTICATED_USER_ID);
        if (uid == null) {
            return true;
        }

        if (isJsonRequest(request)) {
            respondJson(response, HttpStatus.CONFLICT, "Already authenticated.");
        } else {
            response.sendRedirect("/dashboard");
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
