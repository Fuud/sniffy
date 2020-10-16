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
        return Integer.parseInt(version);
    }

    public static void initialize() {

        if (getVersion() >= 9) return; // TODO: change to 8

        try {
            SniffySelectorProviderBootstrap.initialize();
            SniffySelectorProvider.install();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
