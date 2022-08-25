package com.github.aarcanjoq.spring.cloud.gateway.sandbox;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import static org.mockserver.model.HttpResponse.response;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RetryTest {

    public static MockServerContainer mockServer;

    private static String serverIpAddress;

    private static int serverPort;

    @Autowired
    TestRestTemplate template;

    @BeforeAll
    static void setUpBeforeClass() {
        mockServer = new MockServerContainer(DockerImageName.parse("jamesdbloom/mockserver:mockserver-5.11.2"));
        mockServer.start();
        mockServer.waitingFor(Wait.forHealthcheck());

        serverIpAddress = mockServer.getContainerIpAddress();
        serverPort = mockServer.getServerPort();
        System.setProperty("redirect.app.uri", String.format("http://%s:%s", serverIpAddress, serverPort));
    }

    @Test
    void itShouldRetryAndServiceSuccess() {
        final MockServerClient client = new MockServerClient(serverIpAddress, serverPort)
                .reset();
        client.when(HttpRequest.request()
                        .withPath("/retry"), Times.exactly(3))
                .respond(response()
                        .withStatusCode(504)
                        .withBody("{\"errorCode\":\"5.01\"}")
                        .withHeader("Content-Type", "application/json"));
        client.when(HttpRequest.request()
                        .withPath("/retry"))
                .respond(response()
                        .withBody("{\"id\":1,\"number\":\"1234567891\"}")
                        .withHeader("Content-Type", "application/json"));

        final ResponseEntity<String> r = template.exchange("/retry", HttpMethod.GET, null, String.class);
        log.info("Received: status->{}, payload->{}", r.getStatusCodeValue(), r.getBody());
        Assertions.assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void itShouldRetryAndServiceFailed() {
        new MockServerClient(serverIpAddress, serverPort)
                .reset()
                .when(HttpRequest.request()
                        .withPath("/retry"))
                .respond(response()
                        .withStatusCode(504)
                        .withBody("{\"errorCode\":\"5.01\"}")
                        .withHeader("Content-Type", "application/json"));

        final ResponseEntity<String> r = template.exchange("/retry", HttpMethod.GET, null, String.class);
        log.info("Received: status->{}, payload->{}", r.getStatusCodeValue(), r.getBody());
        Assertions.assertThat(r.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }

    @AfterAll
    static void tearDownAfterClass() {
        mockServer.close();
    }
}
