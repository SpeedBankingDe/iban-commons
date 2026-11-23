package de.speedbanking.bankdata.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

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
    void download_permanentRedirect308_followedManuallyToRelativeLocation() throws Exception {
        // HttpURLConnection does not follow a 308 by itself (neither on Java 8 nor on current
        // JDKs), so this hop goes through the manual redirect handling, including the resolution
        // of a relative Location against the request URI
        byte[] body = "redirected-payload".getBytes(UTF_8);
        server = startServer("/", exchange -> {
            if ("/dir/old".equals(exchange.getRequestURI().getPath())) {
                exchange.getResponseHeaders().add("Location", "new");
                exchange.sendResponseHeaders(308, -1);
                exchange.close();
            } else if ("/dir/new".equals(exchange.getRequestURI().getPath())) {
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            } else {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
            }
        });

        byte[] result = Downloader.createDefault().download(uri("/dir/old"), TIMEOUT, TIMEOUT);

        assertThat(result).isEqualTo(body);
    }

    @Test
    void download_secondManualRedirect_throwsHttpDownloadExceptionWithRedirectStatus() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server = startServer("/loop", exchange -> {
            requests.incrementAndGet();
            exchange.getResponseHeaders().add("Location", "/loop");
            exchange.sendResponseHeaders(308, -1);
            exchange.close();
        });

        assertThatThrownBy(() -> Downloader.createDefault().download(uri("/loop"), TIMEOUT, TIMEOUT))
            .isInstanceOf(HttpDownloadException.class)
            .satisfies(ex -> assertThat(((HttpDownloadException) ex).getStatusCode()).isEqualTo(308));
        assertThat(requests).hasValue(2);
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
        // HttpURLConnection follows a same-protocol redirect other than 308 internally
        // (getResponseCode() would already return the followed response), so this module's own
        // manual redirect handling in HttpDownloader.download() only sees a 302 for a
        // protocol-changing hop (http -> https, per the class Javadoc); a raw, unencoded space in
        // that Location header is not a valid RFC 3986 URI reference, and URI.resolve(String)
        // throws an unchecked IllegalArgumentException for it, which must be caught and wrapped
        // rather than surfacing as an undeclared RuntimeException to the caller
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

    @Test
    void download_announcedLengthBeyondLimit_throwsIOException() throws Exception {
        // a raw socket rather than HttpServer, which (since JDK 25) drops the connection without
        // sending the headers when the announced body never follows
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("localhost"))) {
            Thread responder = new Thread(() -> respondWithContentLength(socket, BoundedStreams.MAX_BYTES + 1L));
            responder.start();
            URI uri = URI.create(String.format("http://localhost:%s/huge", socket.getLocalPort()));

            assertThatThrownBy(() -> Downloader.createDefault().download(uri, TIMEOUT, TIMEOUT))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("exceeding the limit");
            responder.join(TIMEOUT.toMillis());
        }
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}: {2}")
    @CsvSource(delimiter = '|', value = {
        "https://a.example/x | http://b.example/y  | true",
        "HTTPS://a.example/x | ftp://b.example/y   | true",
        "https://a.example/x | https://b.example/y | false",
        "http://a.example/x  | https://b.example/y | false",
        "http://a.example/x  | http://b.example/y  | false",
    })
    void isDowngrade_schemeCombination_detectsHttpsToNonHttps(URI source, URI target, boolean expected) {
        assertThat(HttpDownloader.isDowngrade(source, target)).isEqualTo(expected);
    }

    private HttpServer startServer(String path, com.sun.net.httpserver.HttpHandler handler) throws IOException {
        HttpServer newServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        newServer.createContext(path, handler);
        newServer.start();
        return newServer;
    }

    /**
     * Answers a single request with a 200 status line and the given {@code Content-Length}, but no
     * body, then waits for the client to hang up.
     *
     * @param socket        the listening socket
     * @param contentLength the announced body length
     */
    private static void respondWithContentLength(ServerSocket socket, long contentLength) {
        try (Socket client = socket.accept();
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), US_ASCII));
             OutputStream out = client.getOutputStream()) {
            String line = in.readLine();
            while (line != null && !line.isEmpty()) {
                line = in.readLine();
            }
            out.write(String.format("HTTP/1.1 200 OK\r\nContent-Length: %s\r\n\r\n", contentLength).getBytes(US_ASCII));
            out.flush();
            in.read();
        } catch (IOException ignored) {
            // the client closing the connection early is the expected outcome
        }
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + server.getAddress().getPort() + path);
    }

}
