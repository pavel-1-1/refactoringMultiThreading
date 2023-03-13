package org.example;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Request {
    private static final List<String> validPaths = new ArrayList<>();
    private List<NameValuePair> params;

    protected Request() {
        File dir = new File("public");
        path(dir);
    }

    protected void Get(BufferedOutputStream out, String path, String[] headers) {
        System.out.println(Arrays.toString(headers));
        // парсим Query параметры если таковые есть
        try {
            if (path.indexOf('?') != -1) {
                params = URLEncodedUtils.parse(new URI(path), "UTF-8");

                if (getQueryParams().isEmpty()) {
                    outPathHtml(out, "formGet.html");
                }

                try {
                    FormReg form = new FormReg(getQueryParam("name"), Integer.parseInt(getQueryParam("age")), getQueryParam("sex"));
                    System.out.println(form);
                } catch (NumberFormatException e) {
                    outPathHtml(out, "formGet.html");
                    return;
                }
                outPathHtml(out, path.substring(0, path.lastIndexOf("html") + 4));
            }
            // проверяем наличие странички и оправляем
            if (!validPaths.contains(path)) {
                out.write(("""
                        HTTP/1.1 404 Not Found
                        Content-Length: 0
                        Connection: close
                                                
                                                
                        """).getBytes());
                out.flush();
                return;
            }

            outPathHtml(out, path);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected void Post(BufferedOutputStream out, BufferedInputStream in, String path, String[] headers) {
        try {
            System.out.println("headers" + Arrays.toString(headers));

            in.skip(4);
            final String contentLength = extractHeader(headers, "Content-Length");
            if (contentLength == null) {
                out.write("""
                        HTTP/1.1 400 Bad Request
                        Content-Length: 0
                        Connection: close
                                                
                                                
                        """.getBytes());
                out.flush();
                return;
            }

            final int length = Integer.parseInt(contentLength);
            final byte[] bodyBytes = in.readNBytes(length);
            final String body = new String(bodyBytes);
            String[] params = body.split("&");
            System.out.println("body" + Arrays.toString(params));

            // разпарсиваем тело с Query параметрами
            Map<String, String> map = new HashMap<>();
            for (String param : params) {
                String[] paramPars = param.split("=");
                if (paramPars.length == 2) {
                    map.put(paramPars[0], paramPars[1] == null ? "0" : paramPars[1]);
                } else {
                    outPathHtml(out, "index.html");
                    return;
                }
            }

            // создаем обьект с параметрами для дальнейшей работы
            try {
                FormReg form = new FormReg(map.get("name"), Integer.parseInt(map.get("age")), map.get("sex"));
                System.out.println(form);
            } catch (NumberFormatException e) {
                outPathHtml(out, "index.html");
                return;
            }

            outPathHtml(out, path.substring(0, path.lastIndexOf("html") + 4));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private List<NameValuePair> getQueryParams() {
        return params;
    }

    private String getQueryParam(String name) {
        String value = null;
        for (NameValuePair param : params) {
            if (param.getName().equals(name)) {
                value = param.getValue();
                break;
            }
        }
        return value;
    }

    private void outPathHtml(BufferedOutputStream out, String path) {
        try {
            final Path filePath = Path.of(".", "public", path);
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
    }

    private String extractHeader(String[] headers, String header) {
        for (String string : headers) {
            if (string.startsWith(header)) {
                return string.substring(string.indexOf(" ") + 1);
            }
        }
        return null;
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
