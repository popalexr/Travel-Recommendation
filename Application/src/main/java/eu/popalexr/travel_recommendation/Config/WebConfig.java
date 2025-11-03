package eu.popalexr.travel_recommendation.Config;

import eu.popalexr.travel_recommendation.Interceptors.AuthenticatedInterceptor;
import eu.popalexr.travel_recommendation.Interceptors.GuestOnlyInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthenticatedInterceptor authenticatedInterceptor;
    private final GuestOnlyInterceptor guestOnlyInterceptor;

    public WebConfig(
        AuthenticatedInterceptor authenticatedInterceptor,
        GuestOnlyInterceptor guestOnlyInterceptor
    ) {
        this.authenticatedInterceptor = authenticatedInterceptor;
        this.guestOnlyInterceptor = guestOnlyInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticatedInterceptor)
            .addPathPatterns("/dashboard/**", "/logout", "/api/dashboard");

        registry.addInterceptor(guestOnlyInterceptor)
            .addPathPatterns("/login", "/register", "/");
    }
}
