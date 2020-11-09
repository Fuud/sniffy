package io.sniffy.nio.compat;

public class SniffyCompatSelectorProviderModule {

    private static int getVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        if (version.contains("-")) {
            version = version.substring(0, version.indexOf("-"));
        }
        return Integer.parseInt(version);
    }

    public static void initialize() {

        if (getVersion() >= 8) return; // TODO: change to 8

        try {
            Class.forName("io.sniffy.nio.compat.CompatSniffySelectorProviderBootstrap").getMethod("initialize").invoke(null);
            Class.forName("io.sniffy.nio.compat.CompatSniffySelectorProvider").getMethod("install").invoke(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
