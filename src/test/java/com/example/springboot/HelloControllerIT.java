package com.example.springboot;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.FindRequestsResult;
import io.opentelemetry.sdk.trace.SpanProcessor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;

import static com.example.springboot.TracingWireMockInitializer.TRACING_URI;
import static com.example.springboot.TracingWireMockInitializer.TRACING_WIREMOCK_BEAN;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
@AutoConfigureObservability
@ContextConfiguration(initializers = TracingWireMockInitializer.class)
public class HelloControllerIT {

    private final TestRestTemplate template = new TestRestTemplate();
    @LocalServerPort
    int port;
    @Autowired
    @Qualifier(TRACING_WIREMOCK_BEAN)
    private WireMockServer wireMockServer;
    @Autowired
    private SpanProcessor spanProcessor;

    @Test
    public void withNoSamplingFlag() throws Exception {
        wireMockServer.resetRequests();
        var headers = new HttpHeaders();
        headers.add("b3", "eca58a679c4142a267210d68736edcb5-67210d68736edcb5");
        var entity = new HttpEntity<>(headers);
        var response = template.exchange("http://localhost:" + port, HttpMethod.GET, entity, String.class);
        assertThat(response.getBody()).isEqualTo("Greetings from Spring Boot!");
        var traces = getTraces();
        /*
        Traces are not sent to zipkin. The proxies generally do not send the sampling decision in the b3
        header(s). This results in the sampling decision being made by the next hop. In this case, the
        spring-boot microservice DEFAULTS it to 0 even though sampling probability is 1.0. This is not as expected.
         */
        assertThat(traces.getRequests()).hasSize(1);
    }

    @Test
    public void withSamplingFlagTrue() throws Exception {
        wireMockServer.resetRequests();
        var headers = new HttpHeaders();
        headers.add("b3", "ddd58a679c4142a267210d68736edeee-aaa10d68736edbbb-1");
        var entity = new HttpEntity<>(headers);
        var response = template.exchange("http://localhost:" + port, HttpMethod.GET, entity, String.class);
        assertThat(response.getBody()).isEqualTo("Greetings from Spring Boot!");
        var traces = getTraces();
        assertThat(traces.getRequests()).hasSize(1);
        /*
        Traces are being sent to zipkin because sampling is set to true. As expected.
         */
    }

    private FindRequestsResult getTraces() throws InterruptedException {
        Thread.currentThread().join(1_000);
        spanProcessor.forceFlush();
        Thread.currentThread().join(1_000);

        // when get tracing data
        var result = wireMockServer.findRequestsMatching(postRequestedFor(urlEqualTo(TRACING_URI)).build());
        result.getRequests().forEach(r -> log.info("sent to tracing: {} ", r.getBodyAsString()));
        return result;
    }

}
