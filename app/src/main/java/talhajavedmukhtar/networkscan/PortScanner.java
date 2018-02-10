package talhajavedmukhtar.networkscan;

import android.util.Log;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Talha on 2/8/18.
 */
public class PortScanner {
    private static ArrayList<String> openHosts;
    private static ArrayList<Integer> portsToDetect;

    public static ArrayList<String> openPortsHosts;

    PortScanner(ArrayList<String> openHosts, String ports){
        this.openHosts = openHosts;
        portsToDetect = new ArrayList<>();
        openPortsHosts = new ArrayList<>();

        String[] portList = ports.split(",");
        for (String port:
             portList) {
            portsToDetect.add(Integer.parseInt(port));
        }
    }

    public static Future<Boolean> portIsOpen(final ExecutorService es, final String ip, final int port, final int timeout) {
        return es.submit(new Callable<Boolean>() {
            @Override public Boolean call() {
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(ip, port), timeout);
                    socket.close();
                    return true;
                } catch (Exception ex) {
                    if(ex.getMessage().contains("ECONNREFUSED")){
                        Log.d("SocketError",ex.getMessage());
                        return true;
                    }
                    Log.d("SocketError",ex.getMessage());
                    return false;
                }
            }
        });
    }

    public static void execute(int timeout) {
        final ExecutorService es = Executors.newFixedThreadPool(20);

        final ArrayList<Future<Boolean>> futures = new ArrayList<>();
        for(String ip: openHosts) {
            for (Integer port : portsToDetect) {
                futures.add(portIsOpen(es, ip, port, timeout));
            }
        }

        es.shutdown();

        for (final Future<Boolean> f : futures) {
            try{
                if (f.get()) {
                    int index = futures.indexOf(f);

                    openPortsHosts.add(openHosts.get(index/portsToDetect.size()) + ":" + portsToDetect.get(index%portsToDetect.size()));
                }
            }catch (Exception ex){
                Log.d("futureError",ex.getMessage());
            }

        }



    }
}
