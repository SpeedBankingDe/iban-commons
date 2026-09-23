package de.speedbanking.bankdata.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;

/**
 * Integration tests for {@link Downloader#createDefault()} (backed by {@code HttpDownloader}),
 * using a JDK-embedded {@link HttpServer} on {@code localhost} instead of any real network
 * dependency, per this module's zero-dependency HTTP transport requirement.
 */
@SuppressWarnings({"restriction", "checkstyle:MethodName"})
final class HttpDownloaderTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void download_okResponse_returnsBodyBytes() throws Exception {
        byte[] body = "bank-data-payload".getBytes(UTF_8);
        server = startServer("/data", exchange -> {
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });

        byte[] result = Downloader.createDefault().download(uri("/data"), TIMEOUT, TIMEOUT);

        assertThat(result).isEqualTo(body);
    }

    @Test
    void download_followsSingleRedirectHop() throws Exception {
        byte[] body = "redirected-payload".getBytes(UTF_8);
        server = startServer("/", exchange -> {
            if ("/old".equals(exchange.getRequestURI().getPath())) {
                exchange.getResponseHeaders().add("Location", "/new");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
            } else if ("/new".equals(exchange.getRequestURI().getPath())) {
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            } else {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
            }
        });

        byte[] result = Downloader.createDefault().download(uri("/old"), TIMEOUT, TIMEOUT);

        assertThat(result).isEqualTo(body);
    }

    @Test
    void download_redirectWithoutLocationHeader_throwsIOException() throws Exception {
        server = startServer("/no-location", exchange -> exchange.sendResponseHeaders(302, -1));

        assertThatThrownBy(() -> Downloader.createDefault().download(uri("/no-location"), TIMEOUT, TIMEOUT))
            .isInstanceOf(IOException.class)
            .isNotInstanceOf(HttpDownloadException.class)
            .hasMessageContaining("without a Location header");
    }

    @Test
    void download_redirectWithMalformedLocationHeader_throwsIOException() throws Exception {
        // HttpURLConnection follows a same-protocol redirect internally (getResponseCode() would
        // already return the followed response), so this module's own manual redirect handling in
        // HttpDownloader.download() only ever sees a 3xx for a protocol-changing hop (http -> https,
        // per the class Javadoc); a raw, unencoded space in that Location header is not a valid RFC
        // 3986 URI reference, and URI.resolve(String) throws an unchecked IllegalArgumentException
        // for it, which must be caught and wrapped rather than surfacing as an undeclared
        // RuntimeException to the caller
        server = startServer("/bad-location", exchange -> {
            exchange.getResponseHeaders().add("Location", "https://exa mple.invalid/new");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        assertThatThrownBy(() -> Downloader.createDefault().download(uri("/bad-location"), TIMEOUT, TIMEOUT))
            .isInstanceOf(IOException.class)
            .isNotInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("malformed Location header");
    }

    @Test
    void download_nonOkStatus_throwsHttpDownloadExceptionWithStatusCode() throws Exception {
        server = startServer("/missing", exchange -> exchange.sendResponseHeaders(404, -1));

        assertThatThrownBy(() -> Downloader.createDefault().download(uri("/missing"), TIMEOUT, TIMEOUT))
            .isInstanceOf(HttpDownloadException.class)
            .isInstanceOf(IOException.class)
            .satisfies(ex -> assertThat(((HttpDownloadException) ex).getStatusCode()).isEqualTo(404));
    }

    private HttpServer startServer(String path, com.sun.net.httpserver.HttpHandler handler) throws IOException {
        HttpServer newServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        newServer.createContext(path, handler);
        newServer.start();
        return newServer;
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + server.getAddress().getPort() + path);
    }

}
