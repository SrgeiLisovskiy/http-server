package ru.netology;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final List validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private int port;
    private String path;
    private ExecutorService executor;
    private Request request;
    private HashMap<String, Map<String, Handler>> handlers;


    public Server(int port, int sizeThreads) {
        this.port = port;
        this.executor = Executors.newFixedThreadPool(sizeThreads);
    }

    void connectionProcessing(Socket socket) throws URISyntaxException {
        try (
                final var in = new BufferedInputStream(socket.getInputStream());
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            request = Request.createRequest(in);
            if (request == null) {
                out.write((
                        "HTTP/1.1 400 Bad Request\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
            } else {
                path = request.getPath();
            }
            if (validPaths.contains(path)) {
                defaultHandler(path, out);
            } else {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
            }

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    public void defaultHandler(String path, BufferedOutputStream out) throws IOException {
        final var filePath = Path.of(".", "public", path);
        final var mimeType = Files.probeContentType(filePath);

        // special case for classic
        if (path.equals("/classic.html")) {
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(content);
            out.flush();
        }

        final var length = Files.size(filePath);
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }

    public void addHandler(String method, String path, Handler handler) {
        if (!handlers.containsKey(method)) {
            handlers.put(method, new HashMap<>());
        }
        handlers.get(method).put(path, handler);
    }


    void start() {
        try (final var serverSocket = new ServerSocket(9999)) {
            while (!serverSocket.isClosed()) {
                final var socket = serverSocket.accept();
                executor.execute(() -> {
                    try {
                        connectionProcessing(socket);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
