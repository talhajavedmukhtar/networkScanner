package talhajavedmukhtar.networkscan;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

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
    final static String TAG = Tags.makeTag("PortScanner");

    private static ArrayList<Host> discoveredHosts;
    private static ArrayList<Integer> portsToDetect;

    private ListView responseView;
    private static ArrayAdapter<String> responseAdapter;

    private Context mContext;

    public static ArrayList<String> openPortsHosts;

    PortScanner(Context c, ArrayList<Host> discoveredHosts, String ports, ListView rView){
        mContext = c;

        this.discoveredHosts = discoveredHosts;
        portsToDetect = new ArrayList<>();
        openPortsHosts = new ArrayList<>();

        String[] portList = ports.split(",");
        for (String port:
             portList) {
            portsToDetect.add(Integer.parseInt(port));
        }

        responseView = rView;
        responseAdapter = new ArrayAdapter<String>(mContext,android.R.layout.simple_list_item_1, android.R.id.text1, openPortsHosts);
        responseView.setAdapter(responseAdapter);
    }

    private static Future<Boolean> portIsOpen(final ExecutorService es, final String ip, final int port, final int timeout) {
        return es.submit(new Callable<Boolean>() {
            @Override public Boolean call() {
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(ip, port), timeout);
                    socket.close();
                    return true;
                } catch (Exception ex) {
                    if(!ex.getMessage().contains("ECONNREFUSED") && !ex.getMessage().contains("ENETUNREACH")){
                        Log.d("SocketError",ex.getMessage());
                        return true;
                    }
                    Log.d("SocketError",ex.getMessage());
                    return false;
                }
            }
        });
    }

    private static ArrayList<String> getUniqueHosts(){
        ArrayList<String> uniqueHosts = new ArrayList<>();
        for(Host h: discoveredHosts){
            if(!uniqueHosts.contains(h.getIpAd())){
                uniqueHosts.add(h.getIpAd());
                Log.d(TAG,h + " added!");
            }
        }
        return uniqueHosts;
    }

    public static void execute(int timeout) {
        final ExecutorService es = Executors.newFixedThreadPool(20);
        ArrayList<String> uniqueHosts = getUniqueHosts();

        final ArrayList<Future<Boolean>> futures = new ArrayList<>();
        for(String h: uniqueHosts) {
            for (Integer port : portsToDetect) {
                futures.add(portIsOpen(es, h, port, timeout));
            }
        }

        es.shutdown();

        for (final Future<Boolean> f : futures) {
            try{
                if (f.get()) {
                    final int index = futures.indexOf(f);
                    MainActivity.runOnUI(new Runnable() {
                        @Override
                        public void run() {
                            openPortsHosts.add(discoveredHosts.get(index/portsToDetect.size()).getIpAd() + ":" + portsToDetect.get(index%portsToDetect.size()));
                            responseAdapter.notifyDataSetChanged();
                        }
                    });

                }
            }catch (Exception ex){
                Log.d("futureError",ex.getMessage());
            }

        }

    }
}
