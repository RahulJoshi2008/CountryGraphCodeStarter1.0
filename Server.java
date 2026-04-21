import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

public abstract class Server {
    private HttpServer server;
    private int port; 
    private Map<String,String> countryColors;

    private final List<String> messageQueue = Collections.synchronizedList(new ArrayList<>());

    public void sendMessageToUser(String message) {
        messageQueue.add(message);
        System.out.println(message);
    }

    public Server(int port) {
        this.port = port;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new DefaultRoute());         
            server.createContext("/static", new StaticFileHandler()); 
            server.createContext("/country-clicked", new CountryClickedHandler()); 
            server.createContext("/api", new APIHandler()); 
            server.createContext("/get-messages", new MessageHandler());

        } catch (IOException e) {
            throw new RuntimeException("Failed to start HTTP server on port " + port, e);
        }
        countryColors = new HashMap<String,String>();
    }

    public Server() {
        this(8000);
    }

    public abstract void getInputCountries(String country1, String country2);
    public abstract void getColorPath();
    public abstract void handleClick(String country);

    public void clearCountryColors(){
        countryColors.clear();
    }

    public void printCountryColors(){
        System.out.println(countryColors);
    }

    public void addCountryColor(String country, String color){
        countryColors.put(country,color);
    }
    
    public void setMessage(String message){
        countryColors.put("extra", message);
    }

    public boolean removeCountryColor(String country){
        if (!countryColors.containsKey(country))
            return false;
        countryColors.remove(country);
        return true;
    }

    public boolean isColored(String country){
        return countryColors.containsKey(country);
    }

    public class MessageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); 
                return;
            }

            String jsonResponse;
            synchronized (messageQueue) {
                jsonResponse = mapToJSON(Collections.singletonMap("messages", String.join(";;", messageQueue)));
                messageQueue.clear(); 
            }

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }

    static class DefaultRoute implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] res = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("index.html"));
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, res.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(res);
            }
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();   
            String filePath = path.substring("/static/".length()); 

            java.nio.file.Path fullPath = java.nio.file.Paths.get(filePath);

            if (!java.nio.file.Files.exists(fullPath) || java.nio.file.Files.isDirectory(fullPath)) {
                String notFound = "404 Not Found";
                exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
                byte[] bytes = notFound.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            byte[] fileContent = java.nio.file.Files.readAllBytes(fullPath);

            if (path.endsWith(".js")) {
                exchange.getResponseHeaders().add("Content-Type", "application/javascript; charset=UTF-8");
            } else if (path.endsWith(".css")) {
                exchange.getResponseHeaders().add("Content-Type", "text/css; charset=UTF-8");
            } else if (path.endsWith(".html")) {
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            } else {
                exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
            }

            exchange.sendResponseHeaders(200, fileContent.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileContent);
            }
        }
    }

    public class CountryClickedHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String country;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    country = reader.lines().collect(Collectors.joining("\n"));
                }

                handleClick(country); 

                String jSONClickedMap = mapToJSON(countryColors);
                byte[] responseBytes = jSONClickedMap.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                } 

            } else {
                exchange.sendResponseHeaders(405, 0); 
                exchange.getResponseBody().close();
            }
        }
    }

    public class APIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String input;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    input = reader.lines().collect(Collectors.joining("\n"));
                }

                HashMap<String, String> jsonObject = parseJSON(input);
                String country1 = jsonObject.get("country1");
                String country2 = jsonObject.get("country2");

                getInputCountries(country1, country2);

                String jsonResponse = mapToJSON(countryColors);

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                } 

            } else {
                exchange.sendResponseHeaders(405, 0); 
                exchange.getResponseBody().close();
            }
        }
    }

    public void run() {
        server.setExecutor(null);
        server.start();
        System.out.println("Server is running on port " + this.port);
    }

    public void openURL() {
        try {
            Desktop desktop = Desktop.getDesktop();
            URI uri = new URI("http://localhost:" + this.port + "/");
            desktop.browse(uri);
        } catch (Exception e) {
            System.err.println("Failed to open URL: http://localhost:" + this.port + "/ - " + e.getMessage());
        }
    }

    private String mapToJSON(Map<String, String> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":\"")
                    .append(entry.getValue()).append("\"");
            first = false;
        }

        json.append("}");
        return json.toString();
    }

    private HashMap<String, String> parseJSON(String jsonStr) {
        HashMap<String, String> result = new HashMap<>();
        jsonStr = jsonStr.trim();
        if (jsonStr.startsWith("{") && jsonStr.endsWith("}")) {
            jsonStr = jsonStr.substring(1, jsonStr.length() - 1);
        }

        String[] pairs = jsonStr.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();

                if (key.startsWith("\"") && key.endsWith("\"")) {
                    key = key.substring(1, key.length() - 1);
                }
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                result.put(key, value);
            }
        }
        return result;
    }

    public static ArrayList<String> parseCountryList(String input) {
        input = input.trim();
        if (input.startsWith("[") && input.endsWith("]")) {
            input = input.substring(1, input.length() - 1); 
        }

        String[] items = input.split(",");
        ArrayList<String> result = new ArrayList<>();

        for (String item : items) {
            result.add(item.trim().replaceAll("^\"|\"$", "")); 
        }

        return result;
    }
}