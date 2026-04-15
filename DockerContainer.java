private static Future<Void> waitForPort(String host, int port, int timeoutSeconds) {
    long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;

    while (System.currentTimeMillis() < deadline) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            LOGGER.info("ActiveMQ available on {}:{}", host, port);
            return Future.succeededFuture();
        } catch (IOException e) {
            LOGGER.debug("Waiting for ActiveMQ on {}:{}...", host, port);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return Future.failedFuture(ie);
            }
        }
    }

    return Future.failedFuture("Timeout waiting for ActiveMQ on " + host + ":" + port);
}
