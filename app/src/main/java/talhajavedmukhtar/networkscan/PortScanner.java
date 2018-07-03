package talhajavedmukhtar.networkscan;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
public class PortScanner extends AsyncTask{
    final static String TAG = Tags.makeTag("PortScanner");

    private ProgressBar progressBar;
    private ArrayList<String> selectedIps;

    private ArrayList<String> openPortMessages;
    private ArrayAdapter<String> openPortsAdapter;

    private int maxPort;
    private int timeout;
    private int noOfThreads;

    private TextView devicesDoneView;

    private Context context;

    private PortScanActivity portScanActivity;

    PortScanner(Context ctx, ArrayList<String> sIps, ArrayList<String> oPM, ArrayAdapter<String> adap, int max, int tO, int threads){
        context = ctx;

        progressBar = (ProgressBar) ((Activity)context).findViewById(R.id.psLoading);
        devicesDoneView = (TextView) ((Activity)context).findViewById(R.id.devicesDoneView);
        selectedIps = sIps;

        portScanActivity = (PortScanActivity) context;

        openPortMessages = oPM;
        openPortsAdapter = adap;

        maxPort = max;
        timeout = tO;
        noOfThreads = threads;
    }


    private static Future<Boolean> portIsOpen(final ExecutorService es, final String ip , final int port, final int timeout){
        return es.submit(new Callable<Boolean>(){
            @Override
            public Boolean call() throws Exception {
                Socket socket = new Socket();
                try {
                    socket.connect(new InetSocketAddress(ip, port), timeout);
                    socket.close();
                    return true;
                } catch (Exception ex) {
                    Log.d(TAG+".SocketError",ex.getMessage() + " for ip: " + ip);
                    /*if(ex.getMessage().contains("ECONNREFUSED")){
                        return true;
                    }*/
                    return false;
                }
            }
        });
    }

    private void scanPorts(String ip, int max){
        final ExecutorService es = Executors.newFixedThreadPool(noOfThreads);
        ArrayList<Future<Boolean>> futures = new ArrayList<>();

        progressBar.setProgress(0);


        for (int i = 0; i < max; i++) {
            futures.add(portIsOpen(es, ip, i, timeout));
        }

        es.shutdown();

        int i = 0;
        for (final Future<Boolean> f : futures) {
            try{
                if (f.get()) {
                    final int index = i;
                    final String ipAd = ip;
                    PortScanActivity.runOnUI(new Runnable() {
                        @Override
                        public void run() {
                            updateHostInfo(ipAd,index);
                        }
                    });
                }
            }catch (Exception e){
                Log.d(TAG,e.getMessage());
            }finally {
                i += 1;
                progressBar.setProgress(i);
            }
        }
    }

    private void updateHostInfo(String ip, int openPort){
        for(Host h: SummaryActivity.discoveredHosts){
            if(h.getIpAd().equals(ip)){
                h.addOpenPort(openPort);
            }
        }

        String oldMessage = openPortMessages.get(selectedIps.indexOf(ip));
        String[] parts = oldMessage.split(" ");
        int openPortsUpdated = Integer.parseInt(parts[parts.length-1]) + 1;
        String updatedMessage = ip + " Open Ports Found: " + openPortsUpdated;
        openPortMessages.set(selectedIps.indexOf(ip),updatedMessage);
        openPortsAdapter.notifyDataSetChanged();
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        int i;
        for(i = 0; i < selectedIps.size(); i++){
            publishProgress(i);
            try{
                scanPorts(selectedIps.get(i),maxPort);
            }catch (Exception ex){
                Log.d(TAG,ex.getMessage());
                Toast.makeText(context,"Please try with a fewer number of threads",Toast.LENGTH_LONG).show();
            }
        }
        publishProgress(i);

        return null;
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        super.onProgressUpdate(values);
        portScanActivity.updateDevicesDone((int)values[0]);
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        portScanActivity.enableSaveButton();
    }
}
