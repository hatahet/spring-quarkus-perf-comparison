package org.acme.helidon.mp;

import io.helidon.logging.common.LogConfig;
import io.helidon.microprofile.server.Server;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        long started = System.nanoTime();
        LogConfig.configureRuntime();
        Server server = Server.create().start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "helidon4-mp-shutdown"));
        System.out.println("helidon4-mp started in " + ((System.nanoTime() - started) / 1_000_000)
                + "ms on port " + server.port());
    }
}
