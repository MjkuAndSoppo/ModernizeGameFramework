import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class DebugServer {
    public static void main(String[] args) throws Exception {
        String session = "lighting-not-covering";
        String outdir = ".dbg";
        int port = 7777;
        boolean clean = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--session": session = args[++i]; break;
                case "--outdir": outdir = args[++i]; break;
                case "--port": port = Integer.parseInt(args[++i]); break;
                case "--clean": clean = true; break;
            }
        }
        Files.createDirectories(Paths.get(outdir));
        Path logFile = Paths.get(outdir, "trae-debug-log-" + session + ".ndjson");
        Path envFile = Paths.get(outdir, session + ".env");
        if (clean && Files.exists(logFile)) Files.writeString(logFile, "");
        int actualPort = port;
        HttpServer server = null;
        for (int i = 0; i < 10; i++) {
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", actualPort), 0);
                break;
            } catch (IOException e) {
                actualPort++;
            }
        }
        if (server == null) throw new RuntimeException("No port available");
        String apiUrl = "http://127.0.0.1:" + actualPort + "/event";
        Files.writeString(envFile, "DEBUG_SERVER_URL=" + apiUrl + "\nDEBUG_SESSION_ID=" + session + "\n");
        final Path finalLogFile = logFile;
        server.createContext("/event", exchange -> {
            String method = exchange.getRequestMethod();
            Headers headers = exchange.getResponseHeaders();
            headers.set("Access-Control-Allow-Origin", "*");
            headers.set("Access-Control-Allow-Methods", "POST, OPTIONS");
            headers.set("Access-Control-Allow-Headers", "Content-Type");
            if ("OPTIONS".equals(method)) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }
            if ("POST".equals(method)) {
                try (InputStream is = exchange.getRequestBody()) {
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    synchronized (DebugServer.class) {
                        Files.writeString(finalLogFile, body + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    }
                    byte[] resp = "ok".getBytes();
                    exchange.sendResponseHeaders(200, resp.length);
                    exchange.getResponseBody().write(resp);
                } catch (Exception e) {
                    exchange.sendResponseHeaders(400, 0);
                }
            } else {
                exchange.sendResponseHeaders(405, 0);
            }
            exchange.close();
        });
        server.setExecutor(null);
        server.start();
        System.out.println("@@DEBUG_SERVER_INFO");
        System.out.println("{");
        System.out.println("  \"api_url\": \"" + apiUrl + "\",");
        System.out.println("  \"session_id\": \"" + session + "\",");
        System.out.println("  \"log_dir\": \"" + Paths.get(outdir).toAbsolutePath().toString().replace("\\", "/") + "\",");
        System.out.println("  \"log_file\": \"" + finalLogFile.toAbsolutePath().toString().replace("\\", "/") + "\",");
        System.out.println("  \"env_file\": \"" + envFile.toAbsolutePath().toString().replace("\\", "/") + "\"");
        System.out.println("}");
        System.out.println("@@END_DEBUG_SERVER_INFO");
        System.out.println("Debug server running on " + apiUrl);
    }
}
