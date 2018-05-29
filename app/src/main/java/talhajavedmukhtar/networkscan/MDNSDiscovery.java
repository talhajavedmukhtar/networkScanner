package talhajavedmukhtar.networkscan;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.pcap4j.packet.Dot11LinkAdaptationControl;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Talha on 4/1/18.
 */

public class MDNSDiscovery extends AsyncTask{
    private Context mContext;
    private static ArrayList<Host> discoveredHosts;
    private static ArrayList<String> responses;
    private static ArrayAdapter<String> responseAdapter;

    private NsdManager mNsdManager;

    private ArrayList<NsdManager.DiscoveryListener> discoveryListeners;

    private ProgressBar progressBar;

    //To find all the available networks SERVICE_TYPE = "_services._dns-sd._udp"
    public static final String SERVICE_TYPE = "_services._dns-sd._udp";
    //public static final String SERVICE_TYPE = "_googlezone._tcp.";
    public static final String TAG = Tags.makeTag("MDNSDiscovery");

    String mServiceName = "NSD";

    int timeout;


    public MDNSDiscovery(Context context, ArrayList<Host> hosts, ArrayList<String> resp, ArrayAdapter<String> adap, int tO) {
        mContext = context;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

        discoveredHosts = hosts;
        responses = resp;
        responseAdapter = adap;

        discoveryListeners = new ArrayList<>();

        progressBar = (ProgressBar) ((Activity)context).findViewById(R.id.pbLoading);

        timeout = tO;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        progressBar.setProgress(1);

        MainActivity.runOnUI(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext,"MDNS Discovery started",Toast.LENGTH_SHORT).show();
            }
        });

        //initiate discovery listeners
        discoverServices("");

        //wait for specified timeout
        waitTillTimeout(timeout,5);

        //remove all discovery listeners
        stopDiscovery();
        Log.d(TAG,"About to get done");
        return null;
    }


    public class initializeResolveListener implements NsdManager.ResolveListener {

        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Resolve failed " + errorCode);
            switch (errorCode) {
                case NsdManager.FAILURE_ALREADY_ACTIVE:
                    Log.e(TAG, "FAILURE ALREADY ACTIVE");
                    mNsdManager.resolveService(serviceInfo, new initializeResolveListener());
                    break;
                case NsdManager.FAILURE_INTERNAL_ERROR:
                    Log.e(TAG, "FAILURE_INTERNAL_ERROR");
                    break;
                case NsdManager.FAILURE_MAX_LIMIT:
                    Log.e(TAG, "FAILURE_MAX_LIMIT");
                    break;
            }
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
            final String host = serviceInfo.getHost().toString();

            MainActivity.runOnUI(new Runnable() {
                @Override
                public void run() {
                    final String resp = host + " : discovered through mDNS";
                    if(!responses.contains(resp)){
                        discoveredHosts.add(new Host(host,"MDNS"));
                        responses.add(host + " : discovered through mDNS");
                        responseAdapter.notifyDataSetChanged();
                    }
                }
            });

            if (serviceInfo.getServiceName().equals(mServiceName)) {
                Log.d(TAG, "Same IP.");
                return;
            }

        }
    }

    public void stopDiscovery() {
        Log.d(TAG,"Stopping Service Discovery" );
        for (NsdManager.DiscoveryListener dListener: discoveryListeners) {
            mNsdManager.stopServiceDiscovery(dListener);
            Log.d(TAG,"A discovery listener removed." );
        }
        progressBar.setProgress(progressBar.getMax());
        progressBar.setProgress(0);
    }

    public void discoverServices(String serviceType) {
        if(serviceType == ""){
            NsdManager.DiscoveryListener rootDiscoveryListener = new NsdManager.DiscoveryListener() {
                @Override
                public void onDiscoveryStarted(String regType) {
                    Log.d(TAG, "Service discovery started " + regType);
                }

                //this is the important overridden function
                @Override
                public void onServiceFound(NsdServiceInfo service) {
                    Log.d(TAG, "Service discovery success " + service);
                    //AVAILABLE_NETWORKS.add(service);

                    if(service.getServiceType().contains("local.")){
                        String newServiceType = service.getServiceName() + "." + service.getServiceType().substring(0,service.getServiceType().length()-6);
                        Log.d(TAG, "New Service Type:  " + newServiceType);
                        discoverServices(newServiceType);
                        mNsdManager.resolveService(service, new initializeResolveListener());
                    }

                }

                @Override
                public void onServiceLost(NsdServiceInfo service) {
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
            discoveryListeners.add(rootDiscoveryListener);
            mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, rootDiscoveryListener);
        }else{
            NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {

                @Override
                public void onDiscoveryStarted(String regType) {
                    Log.d(TAG, "Service discovery started " + regType);
                }

                @Override
                public void onServiceFound(NsdServiceInfo service) {
                    Log.d(TAG, "Service discovery success " + service);
                    //AVAILABLE_NETWORKS.add(service);

                    mNsdManager.resolveService(service, new initializeResolveListener());

                    if (!service.getServiceType().equals(SERVICE_TYPE)) {
                        Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                    } else if (service.getServiceName().equals(mServiceName)) {
                        Log.d(TAG, "Same Machine: " + mServiceName);
                    } else if (service.getServiceName().contains(mServiceName)) {
                        Log.d(TAG, "Resolving Services: " + service);
                        mNsdManager.resolveService(service, new initializeResolveListener());
                    }
                }

                @Override
                public void onServiceLost(NsdServiceInfo service) {
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
            discoveryListeners.add(discoveryListener);
            mNsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
            Log.d(TAG,"# of discovery listeners: " + discoveryListeners.size() );
        }

    }

    public void waitTillTimeout(final int timeout, final int intervals){
        //intervals is the number of times progress bar is "progressed"
        Thread waitingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int progressStatus;
                int max = 100;
                progressBar.setMax(max);
                Log.d(TAG,"progress bar max " + progressBar.getMax());

                for(int i = 1; i <= intervals; i++){
                    progressStatus = (i*max)/intervals;
                    Log.d(TAG,"new progress status: " + progressStatus);

                    try {
                        Thread.sleep(timeout/intervals);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    progressBar.setProgress(progressStatus);
                }
            }
        });
        waitingThread.start();
        try{
            waitingThread.join();
        }catch (Exception ex){
            Log.d(TAG,ex.getMessage());
        }

    }
}
