package talhajavedmukhtar.networkscan;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

/**
 * Created by Talha on 4/1/18.
 */

public class multicastDNSDiscovery extends AsyncTask {
    private static String TAG = "mDNS Discovery Error";

    private Context mContext;
    private static ArrayList<String> responses;
    private ListView responseView;
    private static ArrayAdapter<String> responseAdapter;

    NsdManager mNsdManager;
    NsdManager.DiscoveryListener mDiscoveryListener;

    public static final String SERVICE_TYPE = "_services._dns-sd._udp";


    multicastDNSDiscovery(Context context, ListView view){
        mContext = context;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

        responses = new ArrayList<>();
        responseView = view;
        responseAdapter = new ArrayAdapter<String>(context,android.R.layout.simple_list_item_1, android.R.id.text1, responses);
        responseView.setAdapter(responseAdapter);

    }

    @Override
    protected Object doInBackground(Object[] objects) {
        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            // Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found! Do something with it.
                Log.d(TAG, "Service discovery success" + service + " Host name: " + service.getHost().toString());
                responses.add(service.getHost().toString());
                responseAdapter.notifyDataSetChanged();
                if (service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.

                    Log.d(TAG, "Service Type: " + service.getServiceType() + " Host name: " + service.getHost().toString());

                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };

        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);

        return null;
    }


}
