package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int THREAD_POOL_SIZE = 64;
    private static final List<String> VALID_PATHS = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    private final ExecutorService threadPool;

    public Server() {
        threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server running on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                threadPool.execute(() -> handleConnection(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    private void handleConnection(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            String requestLine = in.readLine();

            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }

            String[] parts = requestLine.split(" ");
            if (parts.length != 3) {
                return;
            }

            String path = parts[1];
            if (!isValidPath(path)) {
                sendErrorResponse(out, 404, "Not Found");
                return;
            }

            Path filePath = Path.of(".", "public", path);
            String mimeType = Files.probeContentType(filePath);

            if (path.equals("/classic.html")) {
                handleSpecialCase(out, filePath, mimeType);
            } else {
                handleRegularCase(out, filePath, mimeType);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isValidPath(String path) {
        return VALID_PATHS.contains(path);
    }

    private void sendErrorResponse(BufferedOutputStream out, int statusCode, String statusMessage) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(response.getBytes());
        out.flush();
    }

    private void handleSpecialCase(BufferedOutputStream out, Path filePath, String mimeType) throws IOException {
        String template = Files.readString(filePath);
        String content = template.replace(
                "{time}",
                LocalDateTime.now().toString()
        );
        byte[] contentBytes = content.getBytes();
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + mimeType + "\r\n" +
                "Content-Length: " + contentBytes.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(response.getBytes());
        out.write(contentBytes);
        out.flush();
    }

    private void handleRegularCase(BufferedOutputStream out, Path filePath, String mimeType) throws IOException {
        long length = Files.size(filePath);
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + mimeType + "\r\n" +
                "Content-Length: " + length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(response.getBytes());
        Files.copy(filePath, out);
        out.flush();
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start(9999);
    }
}