package eu.popalexr.travel_recommendation.Controllers;

import io.github.inertia4j.spring.Inertia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class HomeController {
    @Autowired
    private Inertia inertia;

    @GetMapping("/")
    public ResponseEntity<String> index() {
        return this.inertia.render("Welcome");
    }
}
