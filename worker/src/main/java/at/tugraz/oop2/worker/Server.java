package at.tugraz.oop2.worker;

import at.tugraz.oop2.shared.FractalLogger;
import at.tugraz.oop2.shared.networking.PacketPing;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;

public class Server {

    static final int DEFAULT_PORT = 8010;

    static final int WORKER_ID = (int) (Math.random() * 0xffff);

    public static void main(String[] args) {
        int port = parsePort(args);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ServerLogger.log("Server shutdown");
        }));

        try {
            var serverSocket = new ServerSocket(port);
            ServerLogger.log("Starting server on port", port);
            serverSocket.setSoTimeout(0); // lets never timeout
            FractalLogger.logStartWorker(port);

            while (true) {
                try {
                    var socket = serverSocket.accept();
                    ServerLogger.log("New client bound to", socket.getRemoteSocketAddress().toString());
                    FractalLogger.logConnectionOpenedWorker();

                    while (!socket.isClosed()) {
                        // read data
                        var in = new ObjectInputStream(socket.getInputStream());
                        Object raw = in.readObject();
                        ServerLogger.log("Received", raw.getClass().getName());

                        if (raw instanceof PacketPing p) {
                            ServerLogger.log("Ping at", p.time, "from", System.currentTimeMillis() - p.time, "ms ago");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ServerLogger.log("Client disconnected!");
                FractalLogger.logConnectionLostWorker();
            }
        } catch (Exception e) {
            ServerLogger.err("Server stopped due to exception", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

    }

    static int parsePort(String[] args) {
        for (var arg : args) {
            if (!arg.startsWith("--port")) continue;
            var splits = arg.split("=");

            if (splits.length == 2) {
                try {
                    int port = Integer.parseInt(splits[1]);
                    return port;
                } catch (NumberFormatException e) {
                    ServerLogger.err("Could not parse port argument:", arg, e.getMessage());
                }
            }
        }

        ServerLogger.log("Using default port");
        return DEFAULT_PORT;
    }
}
