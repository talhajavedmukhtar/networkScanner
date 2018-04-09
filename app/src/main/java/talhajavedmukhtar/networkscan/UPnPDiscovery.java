package talhajavedmukhtar.networkscan;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
    private String DEFAULT_TAG = Tags.makeTag("UPnPDiscovery");

    private Context mContext;
    private static ArrayList<String> responses;
    private static ArrayAdapter<String> responseAdapter;
    private ListView responseView;

    private String DEFAULT_IP = "239.255.255.250";
    private int DEFAULT_PORT = 1900;
    private String DISCOVERY_QUERY = "M-SEARCH * HTTP/1.1" + "\r\n" +
            "HOST: 239.255.255.250:1900" + "\r\n" +
            "MAN: \"ssdp:discover\"" + "\r\n" +
            "MX: 1"+ "\r\n" +
            "ST: ssdp:all" + "\r\n" + // Use this for all UPnP Devices
            "\r\n";

    UPnPDiscovery(Context context, ListView view, ArrayList<String> resp, ArrayAdapter<String> adap){
        mContext = context;
        responses = resp;
        responseView = view;
        responseAdapter = adap;
        //responseView.setAdapter(responseAdapter);
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
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
                socket.bind(new InetSocketAddress(DEFAULT_PORT)); // <-- now bind it


                DatagramPacket datagramPacketRequest = new DatagramPacket(query.getBytes(), query.length(), group, port);
                socket.send(datagramPacketRequest);

                long time = System.currentTimeMillis();
                long curTime = System.currentTimeMillis();

                //responses.clear();
                //Toast.makeText(mContext,"Socket for UPnP Discovery Opened",Toast.LENGTH_SHORT).show();
                //ArrayList<String> responses = new ArrayList<>();
                while (curTime - time < 1000) {
                    DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024);
                    socket.receive(datagramPacket);
                    String response = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                    Log.d(DEFAULT_TAG,response);
                    if (response.substring(0, 12).toUpperCase().equals("HTTP/1.1 200")) {
                        final String ip = datagramPacket.getAddress().getHostAddress() + " : discovered through UPnP";
                        Log.d(DEFAULT_TAG,ip);
                        if(!responses.contains(ip)){
                            MainActivity.runOnUI(new Runnable() {
                                @Override
                                public void run() {
                                    responses.add(ip);
                                    responseAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }
                    curTime = System.currentTimeMillis();
                }

            } catch (final Exception e) {
                e.printStackTrace();
                Log.d(DEFAULT_TAG,e.toString());
            } finally {
                if (socket != null) {
                    socket.close();
                }
                //Toast.makeText(mContext,"Socket for UPnP Discovery Closed",Toast.LENGTH_SHORT).show();
            }
            lock.release();
        }else{
            Toast.makeText(mContext,"Wifi manager is null",Toast.LENGTH_SHORT).show();
        }

        return null;
    }

}
