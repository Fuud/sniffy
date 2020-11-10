package io.sniffy.nio;

import io.sniffy.util.JVMUtil;

/**
 * @since 3.1.7
 */
public class SniffySelectorProviderModule {

    public static void initialize() {

        if (JVMUtil.getVersion() <= 7) return;

        try {
            SniffySelectorProvider.install();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
