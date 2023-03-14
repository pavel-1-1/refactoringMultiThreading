package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final static List<String> methods = List.of("GET", "POST");
    private ServerSocket serverSocket;
    //private final Request request;
    private final ExecutorService treadPul = Executors.newFixedThreadPool(64);

    public Server(int port) {
        //request = new Request();
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
        Request request = new Request();
        return () -> {
            try (BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                 BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())) {

                int limit = 4096;
                in.mark(limit);

                byte[] buffer = new byte[limit];
                int read = in.read(buffer);

                byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
                int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
                if (requestLineEnd == -1) {
                    badRequest(out);
                    socket.close();
                    return;
                }

                String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
                System.out.println(Arrays.toString(requestLine));
                System.out.println();
                if (requestLine.length != 3) {
                    badRequest(out);
                    socket.close();
                    return;
                }

                if (!methods.contains(requestLine[0])) {
                    badRequest(out);
                    socket.close();
                    return;
                }
                System.out.println(requestLine[0]);

                String path = requestLine[1].trim();
                System.out.println(path);
                if (!path.startsWith("/")) {
                    badRequest(out);
                    socket.close();
                    return;
                }
                System.out.println(path);

                byte[] headerDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
                int headersStart = requestLineEnd + requestLineDelimiter.length;
                int headersEnd = indexOf(buffer, headerDelimiter, headersStart, read);
                if (headersEnd == -1) {
                    badRequest(out);
                    System.out.println("err");
                    socket.close();
                    return;
                }

                in.reset();
                in.skip(headersStart);
                byte[] headersByte = in.readNBytes(headersEnd - headersStart);
                String[] headers = new String(headersByte).split("\r\n");

                switch (requestLine[0].trim()) {
                    case "GET" -> request.Get(out, path, headers);
                    case "POST" -> request.Post(out, in, path, headers);
                    default -> {
                        badRequest(out);
                        return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    private int indexOf(byte[] array, byte[] separator, int start, int max) {
        outer:
        for (int i = start; i < max - separator.length + 1; i++) {
            for (int j = 0; j < separator.length; j++) {
                if (array[i + j] != separator[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    protected void badRequest(BufferedOutputStream out) throws IOException {
        out.write("""
                HTTP/1.1 400 Bad Request
                Content-Length: 0
                Connection: close
                                
                                
                """.getBytes());
        out.flush();
    }
}
