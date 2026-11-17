package com.app.ski.core;

import com.app.ski.model.SkiFile;
import com.app.ski.model.SkiProject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalServer {

    private ServerSocket serverSocket;
    private Thread serverThread;
    private volatile boolean running = false;

    private SkiProject project;
    private int port;

    private static final Pattern ROUTE_PATTERN =
            Pattern.compile(
                    "@route\\s+(GET|POST|PUT|DELETE|PATCH)\\s+\"([^\"]+)\"\\s*\\{",
                    Pattern.CASE_INSENSITIVE
            );

    public LocalServer(SkiProject project) {
        this.project = project;
    }

    public synchronized void start() throws Exception {

        if (running) {
            return;
        }

        serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();

        running = true;

        serverThread = new Thread(() -> {

            while (running) {

                try {

                    Socket socket = serverSocket.accept();

                    handleRequest(socket);

                } catch (Exception e) {

                    if (running) {
                        e.printStackTrace();
                    }
                }
            }

        });

        serverThread.start();
    }

    public synchronized void stop() {

        running = false;

        try {

            if (serverSocket != null) {
                serverSocket.close();
            }

        } catch (Exception ignored) {
        }

        serverSocket = null;
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public String getUrl() {
        return "http://127.0.0.1:" + port;
    }

    private void handleRequest(Socket socket) {

        try (
                Socket s = socket;
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        s.getInputStream()
                                )
                        );
                OutputStream output = s.getOutputStream()
        ) {

            String requestLine = reader.readLine();

            if (requestLine == null) {
                return;
            }

            /*
             * Example:
             *
             * GET / HTTP/1.1
             * GET /api/ping HTTP/1.1
             */
            String[] parts = requestLine.split(" ");

            if (parts.length < 2) {
                return;
            }

            String method = parts[0].toUpperCase();
            String path = parts[1];

            /*
             * Read remaining HTTP headers.
             */
            String line;

            while ((line = reader.readLine()) != null) {

                if (line.isEmpty()) {
                    break;
                }
            }

            String body;

            int statusCode;

            String contentType;

            /*
             * API request
             */
            if (path.startsWith("/api/")) {

                BackendResponse response =
                        handleBackendRoute(
                                method,
                                path
                        );

                body = response.body;
                statusCode = response.statusCode;
                contentType = "application/json; charset=utf-8";

            } else {

                /*
                 * Frontend request
                 */
                body = SkiCompiler.compileFrontend(project);

                statusCode = 200;

                contentType = "text/html; charset=utf-8";
            }

            byte[] bytes =
                    body.getBytes(StandardCharsets.UTF_8);

            String responseHeaders =
                    "HTTP/1.1 " +
                            statusCode +
                            " " +
                            statusText(statusCode) +
                            "\r\n" +
                            "Content-Type: " +
                            contentType +
                            "\r\n" +
                            "Content-Length: " +
                            bytes.length +
                            "\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Connection: close\r\n" +
                            "\r\n";

            output.write(
                    responseHeaders.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            output.write(bytes);
            output.flush();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private BackendResponse handleBackendRoute(
            String method,
            String path
    ) {

        SkiFile backend =
                project.getFile("backend.ski");

        if (backend == null) {

            return new BackendResponse(
                    404,
                    "{\"success\":false,\"message\":\"No backend.ski found\"}"
            );
        }

        String source = backend.getContent();

        Matcher matcher =
                ROUTE_PATTERN.matcher(source);

        while (matcher.find()) {

            String routeMethod =
                    matcher.group(1).toUpperCase();

            String routePath =
                    matcher.group(2);

            if (!routeMethod.equals(method)
                    || !routePath.equals(path)) {
                continue;
            }

            /*
             * Find the complete route block.
             */
            int openBrace =
                    matcher.end() - 1;

            int closeBrace =
                    findMatchingBrace(
                            source,
                            openBrace
                    );

            if (closeBrace < 0) {

                return new BackendResponse(
                        500,
                        "{\"success\":false,\"message\":\"Invalid backend route\"}"
                );
            }

            String routeBody =
                    source.substring(
                            openBrace + 1,
                            closeBrace
                    );

            return executeJsonResponse(routeBody);
        }

        return new BackendResponse(
                404,
                "{\"success\":false,\"message\":\"Route not found\"}"
        );
    }

    /**
     * Supports:
     *
     * res.json({
     *     success: true,
     *     message: "Pong from SKI backend 🚀"
     * });
     */
    private BackendResponse executeJsonResponse(
            String routeBody
    ) {

        int resIndex =
                routeBody.indexOf("res.json");

        if (resIndex < 0) {

            return new BackendResponse(
                    500,
                    "{\"success\":false,\"message\":\"Route must use res.json(...)\"}"
            );
        }

        int open =
                routeBody.indexOf(
                        '(',
                        resIndex
                );

        if (open < 0) {

            return new BackendResponse(
                    500,
                    "{\"success\":false,\"message\":\"Invalid res.json()\"}"
            );
        }

        int close =
                findMatchingParenthesis(
                        routeBody,
                        open
                );

        if (close < 0) {

            return new BackendResponse(
                    500,
                    "{\"success\":false,\"message\":\"Invalid JSON response\"}"
            );
        }

        String object =
                routeBody.substring(
                        open + 1,
                        close
                ).trim();

        /*
         * Convert JavaScript-style object:
         *
         * {
         *   success: true,
         *   message: "hello"
         * }
         *
         * into JSON.
         */
        if (object.startsWith("{")
                && object.endsWith("}")) {

            object =
                    object.substring(
                            1,
                            object.length() - 1
                    );
        }

        String json =
                convertObjectToJson(object);

        return new BackendResponse(
                200,
                json
        );
    }

    private String convertObjectToJson(
            String object
    ) {

        StringBuilder json =
                new StringBuilder();

        json.append("{");

        String[] entries =
                object.split(",");

        boolean first = true;

        for (String entry : entries) {

            String[] pair =
                    entry.split(
                            ":",
                            2
                    );

            if (pair.length != 2) {
                continue;
            }

            String key =
                    pair[0].trim();

            String value =
                    pair[1].trim();

            key = removeQuotes(key);

            if (!first) {
                json.append(",");
            }

            first = false;

            json.append("\"")
                    .append(escapeJson(key))
                    .append("\":");

            if (value.equals("true")
                    || value.equals("false")
                    || value.equals("null")
                    || value.matches("-?\\d+(\\.\\d+)?")) {

                json.append(value);

            } else {

                value =
                        removeQuotes(value);

                json.append("\"")
                        .append(escapeJson(value))
                        .append("\"");
            }
        }

        json.append("}");

        return json.toString();
    }

    private String removeQuotes(String value) {

        value = value.trim();

        if (value.length() >= 2) {

            char first =
                    value.charAt(0);

            char last =
                    value.charAt(
                            value.length() - 1
                    );

            if ((first == '"' && last == '"')
                    || (first == '\'' && last == '\'')) {

                return value.substring(
                        1,
                        value.length() - 1
                );
            }
        }

        return value;
    }

    private String escapeJson(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private int findMatchingParenthesis(
            String source,
            int open
    ) {

        int depth = 0;

        boolean string = false;
        boolean escaped = false;
        char quote = 0;

        for (int i = open; i < source.length(); i++) {

            char c = source.charAt(i);

            if (string) {

                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (c == '\\') {
                    escaped = true;
                    continue;
                }

                if (c == quote) {
                    string = false;
                }

                continue;
            }

            if (c == '"' || c == '\'') {

                string = true;
                quote = c;
                continue;
            }

            if (c == '(') {

                depth++;

            } else if (c == ')') {

                depth--;

                if (depth == 0) {
                    return i;
                }
            }
        }

        return -1;
    }

    private int findMatchingBrace(
            String source,
            int open
    ) {

        int depth = 0;

        boolean string = false;
        boolean escaped = false;
        char quote = 0;

        for (int i = open; i < source.length(); i++) {

            char c = source.charAt(i);

            if (string) {

                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (c == '\\') {
                    escaped = true;
                    continue;
                }

                if (c == quote) {
                    string = false;
                }

                continue;
            }

            if (c == '"' || c == '\'' || c == '`') {

                string = true;
                quote = c;
                continue;
            }

            if (c == '{') {

                depth++;

            } else if (c == '}') {

                depth--;

                if (depth == 0) {
                    return i;
                }
            }
        }

        return -1;
    }

    private String statusText(int status) {

        switch (status) {

            case 200:
                return "OK";

            case 404:
                return "Not Found";

            case 500:
                return "Internal Server Error";

            default:
                return "OK";
        }
    }

    private static class BackendResponse {

        final int statusCode;
        final String body;

        BackendResponse(
                int statusCode,
                String body
        ) {

            this.statusCode = statusCode;
            this.body = body;
        }
    }
}