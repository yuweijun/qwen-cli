package com.example.askquery;

import com.example.askquery.service.InteractiveService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AskQueryApplication {

    public static void main(String[] args) {
        SpringApplication.run(AskQueryApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(InteractiveService interactiveService) {
        return args -> {
            // If a first query is provided as program arg, pass it as initial query
            String initial = null;
            if (args != null && args.length > 0) {
                initial = args[0];
            }
            interactiveService.run(initial);
        };
    }
}