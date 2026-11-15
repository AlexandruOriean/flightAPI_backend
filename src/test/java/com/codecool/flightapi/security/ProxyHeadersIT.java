package com.codecool.flightapi.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Over real HTTP, so Tomcat's RemoteIpValve runs (MockMvc bypasses it). The test client connects from
 * loopback, which Tomcat trusts as an internal proxy, like a load balancer in the same network.
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:flight-api-proxy-it;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            // Test-only key (64 random bytes, Base64). Never use outside tests.
            "app.jwt.secret=dGVzdC1vbmx5LWtleS1kby1ub3QtdXNlLWluLXByb2R1Y3Rpb24tMDEyMzQ1Njc4OWFiY2RlZmdoaWprbG1ub3A="
        })
class ProxyHeadersIT {

    private static final String HSTS = "Strict-Transport-Security";

    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("HTTPS terminated at a trusted proxy (X-Forwarded-Proto: https) -> request is secure, HSTS is sent")
    void forwardedHttpsIsSecure() throws Exception {
        HttpResponse<Void> response = client.send(
                health().header("X-Forwarded-Proto", "https").build(), HttpResponse.BodyHandlers.discarding());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue(HSTS))
                .hasValueSatisfying(value -> assertThat(value).contains("max-age="));
    }

    @Test
    @DisplayName("plain HTTP without forwarded headers -> no HSTS")
    void plainHttpIsNotSecure() throws Exception {
        HttpResponse<Void> response = client.send(health().build(), HttpResponse.BodyHandlers.discarding());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue(HSTS)).isEmpty();
    }

    private HttpRequest.Builder health() {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health"));
    }
}
