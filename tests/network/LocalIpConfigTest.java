import java.io.File;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

/** Run from game/: java --class-path '../libs/*' ../tests/network/LocalIpConfigTest.java */
class LocalIpConfigTest {
    static int httpAttempts;
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        boolean autoProbe = args.length == 1 && args[0].equals("auto-probe");
        URL.setURLStreamHandlerFactory(protocol -> protocol.equals("http") || protocol.equals("https")
            ? new URLStreamHandler() {
                protected URLConnection openConnection(URL url) throws java.io.IOException {
                    httpAttempts++;
                    if (!autoProbe) throw new AssertionError("Unexpected external request: " + url);
                    throw new java.io.IOException("Blocked test request; no network access");
                }
            } : null);
        List<String> logs = new ArrayList<>();
        Logger root = Logger.getLogger("");
        Handler recorder = new Handler() {
            public void publish(LogRecord record) { logs.add(record.getMessage()); }
            public void flush() {}
            public void close() {}
        };
        root.addHandler(recorder);
        if (!autoProbe) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.newSchema(new File("data/xsd/ipconfig.xsd")).newValidator()
                .validate(new StreamSource(new File("config/ipconfig.xml")));
        }
        Class<?> type = Class.forName("org.l2jmobius.gameserver.config.ServerConfig$IPConfigData");
        Constructor<?> ctor = type.getDeclaredConstructor();
        ctor.setAccessible(true);
        long start = System.nanoTime();
        Object config = ctor.newInstance();
        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        if (autoProbe) {
            check(httpAttempts == 1, "Missing XML must attempt external-IP request exactly once");
            check(logs.stream().anyMatch(s -> s.contains("using automatic configuration")), "Expected automatic branch");
            System.out.println("PASS: absent XML triggers blocked external-IP request (no real network)");
        } else {
            Method hosts = type.getDeclaredMethod("getHosts"), subnets = type.getDeclaredMethod("getSubnets");
            hosts.setAccessible(true); subnets.setAccessible(true);
            check(hosts.invoke(config).equals(List.of("127.0.0.1", "127.0.0.1")), "Local and fallback hosts must be loopback");
            check(subnets.invoke(config).equals(List.of("127.0.0.0/8", "0.0.0.0/0")), "Native subnet/fallback mappings");
            check(httpAttempts == 0, "No external-IP HTTP requests");
            check(logs.stream().anyMatch(s -> s.contains("using manual configuration")), "Expected manual branch");
            check(logs.stream().noneMatch(s -> s.contains("automatic configuration") || s.startsWith("Error parsing")), "No auto branch or parsing error");
            System.out.printf(Locale.ROOT, "PASS: native XML/XSD, manual branch, loopback mappings, zero external requests; loader %.2f ms; no full GameServer startup%n", elapsedMs);
        }
        root.removeHandler(recorder);
    }
}
