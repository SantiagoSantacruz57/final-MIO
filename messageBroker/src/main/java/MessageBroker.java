import Demo.Printer;
import Demo.Response;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;
import Demo.PrinterPrx;

public class MessageBroker {
    private static PrinterPrx processingUnit1;
    private static PrinterPrx processingUnit2;
    
    public static void main(String[] args) {
        java.util.List<String> extraArgs = new java.util.ArrayList<>();
        try (Communicator communicator = Util.initialize(args, "config.messagebroker", extraArgs)) {
            // Connect to Processing Units
            processingUnit1 = PrinterPrx.checkedCast(communicator.propertyToProxy("ProcessingUnit1.Proxy"));
            processingUnit2 = PrinterPrx.checkedCast(communicator.propertyToProxy("ProcessingUnit2.Proxy"));
            
            if (processingUnit1 == null || processingUnit2 == null) {
                System.err.println("Processing Units not available!");
                return;
            }
            
            // MessageBroker Server
            com.zeroc.Ice.ObjectAdapter adapter = communicator.createObjectAdapter("BrokerAdapter");
            adapter.add(new BrokerPrinterI(), Util.stringToIdentity("Printer"));
            adapter.activate();
            
            System.out.println(" MessageBroker ready - balancing to PU1:10003 PU2:10004");
            communicator.waitForShutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    static class BrokerPrinterI implements Printer {
        @Override
        public Response printString(String s, Current current) {
            System.out.println(" Broker received: " + s);
            
            // Forward to both processing units + measure response size
            String pu1Response = pingProcessingUnit(processingUnit1, s);
            String pu2Response = pingProcessingUnit(processingUnit2, s);
            
            // Choose smallest response
            String winnerResponse = pu1Response.length() <= pu2Response.length() ? pu1Response : pu2Response;
            String winnerAddr = pu1Response.length() <= pu2Response.length() ? 
                "tcp -h localhost -p 10003" : "tcp -h localhost -p 10004";
            
            System.out.println(" Winner: " + winnerAddr + " (size: " + winnerResponse.length() + ")");
            
            Response result = new Response();
            result.value = winnerResponse + "\n Connect directly: BrokerPrinter.Proxy=" + winnerAddr;
            result.responseTime = 5;
            return result;
        }
    }
    
    private static String pingProcessingUnit(PrinterPrx pu, String message) {
        try {
            Response r = pu.printString("ping:" + message);
            return r.value;
        } catch (Exception e) {
            return "ERROR: Processing Unit down";
        }
    }
}
