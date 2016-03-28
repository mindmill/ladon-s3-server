package de.mc.ladon.s3server.auth;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple Client to make raw http requests
 */
public class TestClient implements AutoCloseable {

    private Socket socket;
    private BufferedWriter out;
    private BufferedReader in;

    public TestClient(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
        in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

    }

    public void close() throws IOException {
        out.close();
        in.close();
    }

    public List<String> makeRequest(File request) throws IOException {
        System.out.println(" * Request");

        for (String line : getContents(request)) {
            System.out.println(line);
            out.write(line + "\r\n");
        }
        out.write("\r\n");
        out.flush();
        return readResponse(in);
    }

    private List<String> readResponse(BufferedReader in) throws IOException {
        List<String> result = new ArrayList<>();
        String line;
        while ((line = in.readLine()) != null) {
            result.add(line);
        }
        return result;
    }

    private List<String> getContents(File file) throws IOException {
        List<String> contents = new ArrayList<>();
        BufferedReader input = new BufferedReader(new FileReader(file));
        String line;
        while ((line = input.readLine()) != null) {
            contents.add(line);
        }
        input.close();
        return contents;
    }
}