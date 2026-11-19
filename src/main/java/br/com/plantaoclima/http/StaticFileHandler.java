package br.com.plantaoclima.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serve os arquivos estaticos da pasta web/ (HTML, CSS, JS, SVG, imagens).
 */
public class StaticFileHandler implements HttpHandler {

    private final Path webRoot;

    public StaticFileHandler(Path webRoot) {
        this.webRoot = webRoot.toAbsolutePath().normalize();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String reqPath = exchange.getRequestURI().getPath();
        if (reqPath == null || "/".equals(reqPath)) {
            reqPath = "/index.html";
        }
        Path alvo = webRoot.resolve(reqPath.substring(1)).normalize();

        // Protecao contra path traversal
        if (!alvo.startsWith(webRoot) || !Files.exists(alvo) || Files.isDirectory(alvo)) {
            byte[] msg = "Arquivo nao encontrado.".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(404, msg.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(msg);
            }
            return;
        }

        byte[] content = Files.readAllBytes(alvo);
        exchange.getResponseHeaders().set("Content-Type", detectarContentType(alvo.getFileName().toString()));
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
    }

    private String detectarContentType(String nomeArquivo) {
        String lower = nomeArquivo.toLowerCase();
        if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }
}
