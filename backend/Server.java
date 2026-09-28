import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/** Servidor de demonstração: estáticos + API de pré-reserva, mesmo contrato do server.py. */
public class Server {
    static final Path ROOT = Paths.get(".").toAbsolutePath().normalize();
    static final Path DATA = ROOT.resolve("data/pre_reservas_java.jsonl");
    static final Set<String> EST = Set.of("primavera", "verao", "outono", "inverno");
    static final Set<String> TAM = Set.of("PP", "P", "M", "G", "GG");
    static final Set<String> PECAS = Set.of("jaquetas", "calcas", "botas", "toucas");
    static final Pattern EMAIL = Pattern.compile("^[^@\\s]{1,64}@[^@\\s]{1,190}\\.[^@\\s]{2,}$");
    static final Map<String, String> TYPES = Map.of(
        ".html", "text/html; charset=utf-8", ".css", "text/css; charset=utf-8",
        ".js", "application/javascript; charset=utf-8", ".svg", "image/svg+xml",
        ".png", "image/png", ".json", "application/json; charset=utf-8", ".txt", "text/plain; charset=utf-8");

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", Server::route);
        server.start();
        System.out.println("LATAM demo em http://localhost:" + port);
    }

    static void route(HttpExchange x) throws IOException {
        try {
            String p = x.getRequestURI().getPath(), m = x.getRequestMethod();
            x.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            x.getResponseHeaders().set("X-Frame-Options", "DENY");
            x.getResponseHeaders().set("Cache-Control", "no-cache");
            if (p.equals("/api/health")) send(x, 200, TYPES.get(".json"), "{\"ok\":true}");
            else if (p.equals("/api/pre-reserva")) {
                if (m.equals("POST")) preReserva(x); else send(x, 405, TYPES.get(".json"), "{\"erro\":\"metodo\"}");
            } else if (m.equals("GET") || m.equals("HEAD")) estatico(x, p, m.equals("HEAD"));
            else send(x, 405, TYPES.get(".json"), "{\"erro\":\"metodo\"}");
        } catch (Exception e) {
            send(x, 500, TYPES.get(".json"), "{\"erro\":\"interno\"}");
        } finally { x.close(); }
    }

    static void estatico(HttpExchange x, String p, boolean head) throws IOException {
        if (p.equals("/")) p = "/index.html";
        Path f = ROOT.resolve(p.substring(1)).normalize();
        String rel = ROOT.relativize(f).toString();
        boolean privado = rel.startsWith("data") || rel.startsWith("backend") || rel.startsWith(".");
        if (!f.startsWith(ROOT) || privado || !Files.isRegularFile(f)) {
            send(x, 404, TYPES.get(".txt"), "404 - arquivo não encontrado"); return;
        }
        String n = f.getFileName().toString().toLowerCase();
        String type = TYPES.entrySet().stream().filter(e -> n.endsWith(e.getKey())).map(Map.Entry::getValue)
            .findFirst().orElse("application/octet-stream");
        byte[] body = Files.readAllBytes(f);
        x.getResponseHeaders().set("Content-Type", type);
        x.sendResponseHeaders(200, head ? -1 : body.length);
        if (!head) x.getResponseBody().write(body);
    }

    static void preReserva(HttpExchange x) throws IOException {
        byte[] raw = x.getRequestBody().readNBytes(4097);
        if (raw.length == 0 || raw.length > 4096) { send(x, 413, TYPES.get(".json"), "{\"erro\":\"tamanho\"}"); return; }
        Map<String, List<String>> f = new HashMap<>();
        for (String par : new String(raw, StandardCharsets.UTF_8).split("&")) {
            String[] kv = par.split("=", 2);
            f.computeIfAbsent(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), k -> new ArrayList<>())
             .add(kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8).trim() : "");
        }
        List<String> erros = new ArrayList<>();
        if (!EMAIL.matcher(g(f, "email")).matches()) erros.add("email");
        if (g(f, "destino").isEmpty()) erros.add("destino");
        if (!EST.contains(g(f, "estacao"))) erros.add("estacao");
        if (!TAM.contains(g(f, "tamanho"))) erros.add("tamanho");
        List<String> pecas = f.getOrDefault("pecas", List.of()).stream().filter(PECAS::contains).distinct().sorted().toList();
        if (pecas.isEmpty()) erros.add("pecas");
        if (!g(f, "consentimento").equals("on")) erros.add("consentimento");
        if (!erros.isEmpty()) {
            send(x, 422, TYPES.get(".json"), "{\"erro\":\"validacao\",\"campos\":[\"" + String.join("\",\"", erros) + "\"]}");
            return;
        }
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String linha = "{\"id\":\"" + id + "\",\"em\":\"" + LocalDateTime.now().withNano(0) + "\",\"nome\":\"" + j(g(f, "nome"))
            + "\",\"email\":\"" + j(g(f, "email")) + "\",\"destino\":\"" + j(g(f, "destino")) + "\",\"estacao\":\"" + g(f, "estacao")
            + "\",\"tamanho\":\"" + g(f, "tamanho") + "\",\"estilo\":\"" + j(g(f, "estilo")) + "\",\"pecas\":[\""
            + String.join("\",\"", pecas) + "\"]}\n";
        synchronized (Server.class) {
            Files.createDirectories(DATA.getParent());
            Files.writeString(DATA, linha, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        send(x, 201, TYPES.get(".json"), "{\"ok\":true,\"id\":\"" + id + "\"}");
    }

    static String g(Map<String, List<String>> f, String k) {
        String v = f.getOrDefault(k, List.of("")).get(0);
        return v.length() > 80 ? v.substring(0, 80) : v;
    }
    static String j(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\"").replaceAll("[\\p{Cntrl}]", " "); }

    static void send(HttpExchange x, int code, String type, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type", type);
        x.sendResponseHeaders(code, b.length);
        x.getResponseBody().write(b);
    }
}
