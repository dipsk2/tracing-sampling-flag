package com.example.springboot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@RestController
@Slf4j
public class HelloController {

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        var loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeHeaders(true);
        loggingFilter.setMaxPayloadLength(64000);
        return loggingFilter;
    }

    @GetMapping("/")
    public String index() {
        log.info("HelloController.index");
        return "Greetings from Spring Boot!";
    }

}
