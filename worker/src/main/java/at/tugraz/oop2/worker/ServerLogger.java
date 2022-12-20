package at.tugraz.oop2.worker;

import org.apache.log4j.Logger;

public class ServerLogger {
    private static Logger loggerSingleton;

    private static Logger logger() {
        if (loggerSingleton == null) {
            var id = String.format("WORKER-%04X", Server.WORKER_ID);
            loggerSingleton = Logger.getLogger(id);
        }
        return loggerSingleton;
    }

    public static void log(Object... args) {
        var builder = new StringBuilder();

        for (var o : args) {
            builder.append(o);
            builder.append(' ');
        }

        logger().info(builder.toString());
    }

    public static void err(Object... args) {
        var builder = new StringBuilder();

        for (var o : args) {
            builder.append(o);
            builder.append(' ');
        }

        logger().error(builder.toString());
    }
}
