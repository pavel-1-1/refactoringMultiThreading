package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final List<String> validPaths = new ArrayList<>();
    private ServerSocket serverSocket;
    private final ExecutorService treadPul = Executors.newFixedThreadPool(64);

    public Server(int port) {
        File dir = new File("public");
        path(dir);
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void processing() {
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                treadPul.execute(connection(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Runnable connection(Socket socket) {
        return () -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())) {
                final String requestLine = in.readLine();
                final String[] parts = requestLine.split(" ");
                System.out.println(requestLine);

                if (parts.length != 3) {
                    socket.close();
                    return;
                }

                final String path = parts[1];
                if (!validPaths.contains(path)) {
                    out.write(("""
                            HTTP/1.1 404 Not Found\r
                            Content-Length: 0\r
                            Connection: close\r
                            \r
                            """).getBytes());
                    out.flush();
                    return;
                }

                final Path filePath = Path.of(".", "public", path);
                System.out.println(filePath);
                final String mimeType = Files.probeContentType(filePath);
                final long length = Files.size(filePath);

                out.write(("HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n").getBytes());
                Files.copy(filePath, out);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    private void path(File dir) {
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    path(file);
                } else {
                    validPaths.add(file.getPath().substring(file.getPath().indexOf("\\")).replace("\\", "/"));
                }
            }
        }
    }
}
