package dev.aincrad.client;

import java.net.URI;

/** One server origin owns HTTP credentials and the WebSocket upgrade. */
public record Endpoint(URI base, String origin) {
    public static Endpoint parse(String value) {
        try {
            URI uri = URI.create(value.strip());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost()==null || uri.getRawUserInfo()!=null || uri.getRawQuery()!=null || uri.getRawFragment()!=null || !(uri.getPath().isEmpty() || uri.getPath().equals("/")) || uri.getPort()>65535 || uri.getPort()==0)
                throw new IllegalArgumentException();
            String scheme=uri.getScheme().toLowerCase(java.util.Locale.ROOT);
            String host=uri.getHost().toLowerCase(java.util.Locale.ROOT);
            if (host.indexOf(':')>=0 && !host.startsWith("[")) host="["+host+"]";
            // Browser Origin serialization omits default ports, as does the native client.
            int port=uri.getPort();
            String origin=scheme+"://"+host+((port==-1 || port==80 && scheme.equals("http") || port==443 && scheme.equals("https"))?"":":"+port);
            return new Endpoint(URI.create(origin+"/"), origin);
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("Servidor: usa http://host:puerto o https://host, sin usuario, ruta ni parámetros.");
        }
    }
    public URI api(String path) { return base.resolve(path.startsWith("/")?path.substring(1):path); }
    public URI socket() { return URI.create((base.getScheme().equals("https")?"wss":"ws")+origin.substring(origin.indexOf(':'))+"/ws"); }
}
