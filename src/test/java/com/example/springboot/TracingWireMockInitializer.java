package com.example.springboot;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;

/**
 * This class is used to initialize WireMock server and set test property for zipkin/jaeger endpoint
 */
public class TracingWireMockInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    public static final String TRACING_URI = "/api/v2/spans";
    public static final String TRACING_WIREMOCK_BEAN = "tracingWiremock";
    public static final String zipkinEndpointProperty = "management.zipkin.tracing.endpoint";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        var wireMockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());
        wireMockServer.start();
        // setup tracing stub
        wireMockServer.stubFor(post(TRACING_URI).willReturn(aResponse().withBody("OK")));

        context.getBeanFactory().registerSingleton(TRACING_WIREMOCK_BEAN, wireMockServer);

        context.addApplicationListener(applicationEvent -> {
            if (applicationEvent instanceof ContextClosedEvent)
                wireMockServer.stop();
        });

        // set test property based on port number of wiremock server
        TestPropertyValues.of(Map.of(zipkinEndpointProperty, "http://localhost:" + wireMockServer.port() + TRACING_URI))
                .applyTo(context);

    }
}
