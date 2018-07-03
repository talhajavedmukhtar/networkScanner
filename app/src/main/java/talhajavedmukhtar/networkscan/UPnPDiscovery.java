package talhajavedmukhtar.networkscan;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Talha on 3/24/18.
 */
public class UPnPDiscovery extends AsyncTask {
    private String TAG = Tags.makeTag("UPnPDiscovery");

    private Context mContext;
    private static ArrayList<Host> discoveredHosts;
    private static ArrayList<String> responses;
    private static ArrayAdapter<String> responseAdapter;

    private String DEFAULT_IP = "239.255.255.250";
    private int DEFAULT_PORT = 1900;
    private String DISCOVERY_QUERY = "M-SEARCH * HTTP/1.1" + "\r\n" +
            "HOST: 239.255.255.250:1900" + "\r\n" +
            "MAN: \"ssdp:discover\"" + "\r\n" +
            "MX: 1"+ "\r\n" +
            "ST: ssdp:all" + "\r\n" + // Use this for all UPnP Devices
            "\r\n";

    int timeout;

    private ProgressBar progressBar;

    UPnPDiscovery(Context context, ArrayList<Host> hosts, ArrayList<String> resp, ArrayAdapter<String> adap, int tO){
        mContext = context;
        responses = resp;
        discoveredHosts = hosts;
        responseAdapter = adap;

        progressBar = (ProgressBar) ((Activity)context).findViewById(R.id.pbLoading);
        timeout = tO;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        progressBar.setProgress(1);
        MainActivity.runOnUI(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext,"UPnP Discovery started",Toast.LENGTH_SHORT).show();
            }
        });

        ArrayList<String> discoveredIps = new ArrayList<>();

        Log.d(TAG,"Waiting for UPnP");
        WifiManager wifi = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(wifi != null) {
            WifiManager.MulticastLock lock = wifi.createMulticastLock("The Lock");
            lock.acquire();
            DatagramSocket socket = null;
            try {
                InetAddress group = InetAddress.getByName(DEFAULT_IP);
                int port = DEFAULT_PORT;
                String query = DISCOVERY_QUERY;

                socket = new DatagramSocket(null); // <-- create an unbound socket first
                socket.setReuseAddress(true);
                socket.setSoTimeout(timeout);
                socket.bind(new InetSocketAddress(DEFAULT_PORT)); // <-- now bind it


                DatagramPacket datagramPacketRequest = new DatagramPacket(query.getBytes(), query.length(), group, port);
                socket.send(datagramPacketRequest);

                long time = System.currentTimeMillis();
                long curTime = System.currentTimeMillis();

                //responses.clear();
                //Toast.makeText(mContext,"Socket for UPnP Discovery Opened",Toast.LENGTH_SHORT).show();
                //ArrayList<String> responses = new ArrayList<>();
                while (curTime - time < timeout) {
                    Log.d(TAG,"Waiting for UPnP");

                    int progressStatus;
                    int max = 100;
                    progressBar.setMax(max);
                    progressStatus = (((int)(curTime - time))/timeout)*100;
                    progressBar.setProgress(progressStatus);

                    DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024);
                    socket.receive(datagramPacket);
                    String response = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                    Log.d(TAG,response);
                    if (response.substring(0, 12).toUpperCase().equals("HTTP/1.1 200")) {
                        final String ip = datagramPacket.getAddress().getHostAddress();
                        final String resp = ip + " : discovered through UPnP";
                        if(!discoveredIps.contains(ip)){
                            discoveredIps.add(ip);
                            MainActivity.runOnUI(new Runnable() {
                                @Override
                                public void run() {
                                    discoveredHosts.add(new Host(ip,"UPnP"));
                                    responses.add(resp);
                                    responseAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                        Log.d(TAG,ip);
                    }

                    curTime = System.currentTimeMillis();
                    Log.d(TAG,"Difference is: " + Long.toString(curTime-time));
                }

            } catch (final Exception e) {
                e.printStackTrace();
                Log.d(TAG,e.toString() + " : " + e.getMessage());
            } finally {
                if (socket != null) {
                    socket.close();
                }
                Log.d(TAG,"done");
                //Toast.makeText(mContext,"Socket for UPnP Discovery Closed",Toast.LENGTH_SHORT).show();
                progressBar.setProgress(progressBar.getMax());
            }
            lock.release();

        }else{
            MainActivity.runOnUI(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext,"Wifi manager is null",Toast.LENGTH_SHORT).show();
                }
            });
        }

        return null;
    }

}
