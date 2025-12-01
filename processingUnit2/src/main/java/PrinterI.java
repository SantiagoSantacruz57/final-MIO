import Demo.Printer;
import Demo.Response;
import com.zeroc.Ice.Current;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.net.*;
import java.io.IOException;

public class PrinterI implements Printer {

    // acumuladores para throughput
    private static long serverStartTime = System.nanoTime();
    private static long totalBytesSent = 0;

    public Response printString(String s, Current current) {
        long startTime = System.nanoTime();

        String [] parts = s.split(":");
        if (parts.length < 3) {
            return new Response(1, "Formato inválido. Debe ser: usuario:host:comando");
        }

        String username = parts[0];
        String hostname = parts[1];
        String command = parts[2];
        String clientInfo = username + "/" + hostname;

        Response result;

        if (command.startsWith("listifs")){
            result = showListIfs(clientInfo);
        } else if (isPositiveInteger(command)) {
            result = showFibonacci(command, clientInfo);
        } else if (command.startsWith("listports")) {
            result = showListPorts(command, clientInfo);
        } else if (command.startsWith("!")){
            result = handleSystemCommand(command, clientInfo);
        } else {
            result = new Response(1, "Comando no reconocido: "+ command);
        }

        long endTime = System.nanoTime();
        double elapsedMs = (endTime - startTime) / 1_000_000.0;
        result.responseTime = (long)elapsedMs;

        // calcular tamaño de respuesta en bytes
        int responseBytes = result.value.getBytes().length;
        totalBytesSent += responseBytes;

        // calcular throughput en B/s
        double elapsedSeconds = (endTime - serverStartTime) / 1_000_000_000.0;
        double throughputBps = totalBytesSent / elapsedSeconds;

        // añadir métrica de throughput al mensaje
        result.value += "\n[Throughput servidor: " + String.format("%.2f", throughputBps) + " B/s]";

        return result;
    }

    private boolean isPositiveInteger(String str) {
        try {
            int value = Integer.parseInt(str);
            return value > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Response showFibonacci(String command, String clientInfo){
        int n = Integer.parseInt(command);
        StringBuilder sb = new StringBuilder();
        System.out.println(clientInfo + ": Request of fibonacci:" + n);
        for (int i = 0; i < n; i++) {
            sb.append(fibonacci(i)).append(" ");
        }
        List<Integer> primeFactors = getPrimeFactors(n);
        return new Response(0, "La serie de fibonacci del número " + n + " es: " + sb.toString() + "\n"
                + ". Los factores primos de " + n + " son: " + primeFactors.toString());
    }

    private Integer fibonacci(int n) {
        if (n <= 1) {
            return n;
        }
        return fibonacci(n - 2) + fibonacci(n - 1);
    }

    private List<Integer> getPrimeFactors(int n) {
        List<Integer> factors = new ArrayList<>();
        while (n % 2 == 0) {
            factors.add(2);
            n /= 2;
        }
        for (int i = 3; i <= n / i; i += 2) {
            while (n % i == 0) {
                factors.add(i);
                n /= i;
            }
        }
        if (n > 1) {
            factors.add(n);
        }
        return factors;
    }

    private String listIfs(){
        String result = "";
        try {
            StringBuilder output = new StringBuilder();
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements()){
                NetworkInterface netint = nets.nextElement();
                output.append(netint.getName()).append(" - ").append(netint.getDisplayName()).append("\n");
            }
            result = output.toString();
        } catch (SocketException e){
            result = "Error obteniendo las network interfaces: " + e.getMessage();
        }
        return result;
    }

    private Response showListIfs(String clientInfo){
        String response = listIfs();
        System.out.println(clientInfo + ":" + "Request of listIfs");
        return new Response(0, "Server response: " + response);
    }

    private Response showListPorts(String command, String clientInfo) {
        String[] parts = command.split("\\s+");
        if (parts.length < 2) {
            return new Response(1, "Uso: listports <IPv4> [start end]");
        }

        String ip = parts[1].trim();
        int start = 1;
        int end = 1024;

        if (parts.length >= 4) {
            try {
                start = Integer.parseInt(parts[2]);
                end = Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                return new Response(1, "start y end deben ser enteros");
            }
        }

        if (start < 1) start = 1;
        if (end > 65535) end = 65535;

        String result = scanPorts(ip, start, end, 200);
        System.out.println(clientInfo + ": Request of listports " + ip + " rango " + start + "-" + end);
        return new Response(0, result);
    }

    private String scanPorts(String ip, int startPort, int endPort, int timeoutMs) {
        StringBuilder sb = new StringBuilder();
        sb.append("Scanning ").append(ip).append(" ports ").append(startPort).append("-").append(endPort).append("\n");

        for (int port = startPort; port <= endPort; port++) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, port), timeoutMs);
                sb.append(port).append("/tcp open\n");
            } catch (IOException ignored) {
            }
        }
        if (sb.toString().trim().isEmpty()) {
            sb.append("No se encontraron puertos abiertos en el rango ").append(startPort).append("-").append(endPort);
        }
        return sb.toString();
                                                                                                                                    
    }

    private Response handleSystemCommand(String message, String clientInfo) {
        String command = message.substring(1);
        System.out.println(clientInfo + ": Request of system command: " + command);

        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            String result = output.toString();
            return new Response(0, clientInfo + ": Result of command: " + result);
        } catch (IOException e) {
            return new Response(1, "Error executing command: " + e.getMessage());
        }
    }
}
