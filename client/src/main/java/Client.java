import Demo.Response;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        java.util.List<String> extraArgs = new java.util.ArrayList<>();

        try (com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args, "config.client", extraArgs)) {
            Response response = null;
            Demo.PrinterPrx service = Demo.PrinterPrx
                    .checkedCast(communicator.propertyToProxy("Printer.Proxy"));

            if (service == null) {
                throw new Error("Invalid proxy");
            }

            String username = System.getProperty("user.name");
            String hostname = java.net.InetAddress.getLocalHost().getHostName();
            String prefix = username + ":" + hostname + ":";

            System.out.println("Client started as " + prefix);

            long startTime = System.nanoTime(); // inicio
            long totalBytes = 0;                // acumulador de bytes recibidos

            while (true) {
                System.out.print("Ingrese un comando (o 'exit' para terminar): ");
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("exit")) {
                    break;
                }

                response = service.printString(prefix + input);

                // calcular bytes recibidos en esta respuesta
                int responseBytes = response.value.getBytes().length;
                totalBytes += responseBytes;

                long currentTime = System.nanoTime();
                double elapsedSeconds = (currentTime - startTime) / 1_000_000_000.0;
                double throughputBps = totalBytes / elapsedSeconds; // bytes/s

                System.out.println("Respuesta del server: " + response.value);
                System.out.println("Tiempo de respuesta (latencia): " + response.responseTime + " ms");
                System.out.println("Throughput acumulado: " + String.format("%.2f", throughputBps) + " B/s\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
