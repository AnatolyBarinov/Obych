package org.example;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class RequestHandler {
    private final List<String> VALID_PATHS;

    public RequestHandler() {
        VALID_PATHS = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    }

    public void handleConnection(BufferedReader in, BufferedOutputStream out) {
        try {
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
}