import Demo.Response;
import java.util.Scanner;

public class Bus {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        java.util.List<String> extraArgs = new java.util.ArrayList<>();

        try (com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args, "config.bus", extraArgs)) {
            Response response = null;
            Demo.PrinterPrx service = Demo.PrinterPrx
        .checkedCast(communicator.propertyToProxy("MessageBroker.Proxy"));


            if (service == null) {
                throw new Error("Invalid Bus proxy");
            }

            String username = System.getProperty("user.name");
            String hostname = java.net.InetAddress.getLocalHost().getHostName();
            String prefix = username + ":" + hostname + ":BUS:";

            System.out.println("Bus started as " + prefix);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
