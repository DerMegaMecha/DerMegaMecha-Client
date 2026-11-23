package com.dermegamecha.launcher.auth;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.jna.platform.win32.Crypt32Util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/** Microsoft OAuth -> Xbox Live -> XSTS -> Minecraft Services account verification. */
public final class MicrosoftAuthService {
    private static final String AUTHORITY = "https://login.microsoftonline.com/consumers/oauth2/v2.0";
    private static final String TOKEN_URL = AUTHORITY + "/token";
    private static final String SCOPE = "XboxLive.signin offline_access";
    private static final Gson JSON = new Gson();
    private final Path directory;
    private final Path credentialsFile;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    private final SecureRandom random = new SecureRandom();

    public MicrosoftAuthService(Path directory) { this.directory = directory; this.credentialsFile = directory.resolve("account.dpapi"); }

    public SignedInAccount signIn(Desktop desktop) throws Exception {
        String clientId = readClientId();
        try (ServerSocket callback = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            callback.setSoTimeout((int) TimeUnit.MINUTES.toMillis(3));
            String redirect = "http://localhost:" + callback.getLocalPort() + "/callback";
            String verifier = randomUrl(32);
            String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
            String state = randomUrl(24);
            URI authorize = URI.create(AUTHORITY + "/authorize?" + form(Map.of("client_id", clientId, "response_type", "code", "redirect_uri", redirect,
                    "response_mode", "query", "scope", SCOPE, "state", state, "code_challenge", challenge, "code_challenge_method", "S256", "prompt", "select_account")));
            desktop.browse(authorize);
            String code = awaitCallback(callback, state);
            JsonObject token = postForm(TOKEN_URL, Map.of("client_id", clientId, "grant_type", "authorization_code", "code", code,
                    "redirect_uri", redirect, "code_verifier", verifier, "scope", SCOPE));
            String refreshToken = required(token, "refresh_token");
            SignedInAccount account = authenticateMinecraft(required(token, "access_token"));
            saveRefreshToken(refreshToken);
            return account;
        }
    }

    public SignedInAccount restore() throws Exception {
        if (!Files.exists(credentialsFile)) return null;
        String refreshToken = readRefreshToken();
        JsonObject token = postForm(TOKEN_URL, Map.of("client_id", readClientId(), "grant_type", "refresh_token", "refresh_token", refreshToken, "scope", SCOPE));
        String nextRefresh = token.has("refresh_token") ? token.get("refresh_token").getAsString() : refreshToken;
        SignedInAccount account = authenticateMinecraft(required(token, "access_token"));
        saveRefreshToken(nextRefresh);
        return account;
    }

    public void logout() throws IOException { Files.deleteIfExists(credentialsFile); }

    private SignedInAccount authenticateMinecraft(String microsoftAccessToken) throws Exception {
        JsonObject userRequest = new JsonObject();
        JsonObject properties = new JsonObject(); properties.addProperty("AuthMethod", "RPS"); properties.addProperty("SiteName", "user.auth.xboxlive.com"); properties.addProperty("RpsTicket", "d=" + microsoftAccessToken);
        userRequest.add("Properties", properties); userRequest.addProperty("RelyingParty", "http://auth.xboxlive.com"); userRequest.addProperty("TokenType", "JWT");
        JsonObject user = postJson("https://user.auth.xboxlive.com/user/authenticate", userRequest.toString());
        String userToken = required(user, "Token");
        String userHash = user.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();

        JsonObject xstsRequest = new JsonObject(); JsonObject xstsProps = new JsonObject(); JsonArray tokens = new JsonArray(); tokens.add(userToken);
        xstsProps.add("UserTokens", tokens); xstsProps.addProperty("SandboxId", "RETAIL"); xstsRequest.add("Properties", xstsProps);
        xstsRequest.addProperty("RelyingParty", "rp://api.minecraftservices.com/"); xstsRequest.addProperty("TokenType", "JWT");
        JsonObject xsts = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", xstsRequest.toString());
        JsonObject mcRequest = new JsonObject(); mcRequest.addProperty("identityToken", "XBL3.0 x=" + userHash + ";" + required(xsts, "Token"));
        JsonObject mc = postJson("https://api.minecraftservices.com/authentication/login_with_xbox", mcRequest.toString());
        String minecraftToken = required(mc, "access_token");
        JsonObject entitlements = getJson("https://api.minecraftservices.com/entitlements/mcstore", minecraftToken);
        JsonArray items = entitlements.has("items") ? entitlements.getAsJsonArray("items") : new JsonArray();
        if (items.isEmpty()) throw new IOException("This Microsoft account has no Minecraft Java entitlement.");
        JsonObject profile = getJson("https://api.minecraftservices.com/minecraft/profile", minecraftToken);
        return new SignedInAccount(required(profile, "name"), required(profile, "id"), minecraftToken);
    }

    private JsonObject postJson(String url, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return sendJson(request);
    }
    private JsonObject getJson(String url, String bearer) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + bearer).GET().build();
        return sendJson(request);
    }
    private JsonObject postForm(String url, Map<String, String> values) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form(values))).build();
        return sendJson(request);
    }
    private JsonObject sendJson(HttpRequest request) throws Exception {
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IOException("Authentication service returned HTTP " + response.statusCode() + ". Check your account and try again.");
        return JSON.fromJson(response.body(), JsonObject.class);
    }

    private String awaitCallback(ServerSocket server, String expectedState) throws Exception {
        try (var socket = server.accept()) {
            socket.setSoTimeout(15000);
            var in = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            String first = in.readLine();
            String line; while ((line = in.readLine()) != null && !line.isEmpty()) { /* consume HTTP headers */ }
            if (first == null || !first.startsWith("GET ")) throw new IOException("Invalid Microsoft sign-in callback.");
            String path = first.split(" ")[1]; Map<String, String> params = parseQuery(URI.create("http://localhost" + path).getRawQuery());
            String html = "<html><body style='font-family:sans-serif;background:#111720;color:#eafaff;padding:3em'><h2>Sign-in complete</h2><p>You can return to DerMegaMecha Launcher.</p></body></html>";
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: " + bytes.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().write(bytes); socket.getOutputStream().flush();
            if (params.containsKey("error")) throw new IOException("Sign-in cancelled or denied.");
            if (!expectedState.equals(params.get("state"))) throw new IOException("Microsoft sign-in state validation failed.");
            String code = params.get("code"); if (code == null || code.isBlank()) throw new IOException("Microsoft did not return an authorization code.");
            return code;
        }
    }

    private void saveRefreshToken(String token) throws Exception {
        if (!System.getProperty("os.name", "").toLowerCase().contains("windows")) throw new IOException("Secure persistent sign-in is currently supported on Windows only.");
        Files.createDirectories(directory); Files.write(credentialsFile, Crypt32Util.cryptProtectData(token.getBytes(StandardCharsets.UTF_8)));
    }
    private String readRefreshToken() throws Exception {
        if (!System.getProperty("os.name", "").toLowerCase().contains("windows")) throw new IOException("Windows Credential Protection is required.");
        return new String(Crypt32Util.cryptUnprotectData(Files.readAllBytes(credentialsFile)), StandardCharsets.UTF_8);
    }
    private String readClientId() throws IOException {
        String env = System.getenv("DERMEGAMECHA_MICROSOFT_CLIENT_ID");
        if (env != null && !env.isBlank()) return env.trim();
        Path config = directory.resolve("launcher.properties");
        if (!Files.exists(config)) throw new IOException("Microsoft sign-in has not been configured by the launcher publisher.");
        Properties props = new Properties(); try (var reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) { props.load(reader); }
        String id = props.getProperty("microsoft.clientId", "").trim();
        if (id.isEmpty() || id.equals("YOUR_PUBLIC_CLIENT_APPLICATION_ID")) throw new IOException("Microsoft sign-in has not been configured by the launcher publisher.");
        return id;
    }
    private String randomUrl(int bytes) { byte[] value = new byte[bytes]; random.nextBytes(value); return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
    private static String required(JsonObject object, String key) throws IOException {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) throw new IOException("Authentication response is incomplete.");
        return object.get(key).getAsString();
    }
    private static String form(Map<String, String> values) {
        StringBuilder out = new StringBuilder(); values.forEach((key, value) -> { if (!out.isEmpty()) out.append('&'); out.append(URLEncoder.encode(key, StandardCharsets.UTF_8)).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8)); }); return out.toString();
    }
    private static Map<String, String> parseQuery(String query) {
        Map<String, String> out = new HashMap<>(); if (query == null) return out;
        for (String part : query.split("&")) { String[] pair = part.split("=", 2); out.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : ""); }
        return out;
    }
}
