package talhajavedmukhtar.networkscan;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.nfc.Tag;
import android.os.AsyncTask;
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

public class NsdClient extends AsyncTask{
    private Context mContext;
    private static ArrayList<String> responses;
    private ListView responseView;
    private static ArrayAdapter<String> responseAdapter;

    private NsdManager mNsdManager;
    NsdManager.DiscoveryListener mDiscoveryListener;

    private ProgressBar progressBar;

    //To find all the available networks SERVICE_TYPE = "_services._dns-sd._udp"
    public static final String SERVICE_TYPE = "_services._dns-sd._udp";
    //public static final String SERVICE_TYPE = "_googlezone._tcp.";
    public static final String TAG = Tags.makeTag("MDNSDiscovery");

    String mServiceName = "NSD";

    private static ArrayList<NsdServiceInfo> ServicesAvailable = new ArrayList<>();

    public NsdClient(Context context, ListView view, ArrayList<String> resp, ArrayAdapter<String> adap) {
        mContext = context;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

        responses = resp;
        responseView = view;
        responseAdapter = adap;

        progressBar = (ProgressBar) ((Activity)context).findViewById(R.id.pbLoading);
    }

    public void initializeNsd() {
        initializeDiscoveryListener();
    }

    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started " + regType);
            }

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
                if (ServicesAvailable.equals(service)) {
                    ServicesAvailable = null;
                }
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

        initializeNsd();
        discoverServices("");
        //stopDiscovery();
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
                    if(!responses.contains(host + " : discovered through mDNS")){
                        responses.add(host + " : discovered through mDNS");
                        Log.d(TAG,host + " Added!");
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
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        progressBar.setProgress(progressBar.getMax());
        progressBar.setProgress(0);
    }

    public List<NsdServiceInfo> getChosenServiceInfo() {
        return ServicesAvailable;
    }

    public void discoverServices(String serviceType) {
        if(serviceType == ""){
            mNsdManager.discoverServices(
                    SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        }else{
            mNsdManager.discoverServices(
                    serviceType, NsdManager.PROTOCOL_DNS_SD, new NsdManager.DiscoveryListener() {

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
                            if (ServicesAvailable.equals(service)) {
                                ServicesAvailable = null;
                            }
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
                    });

        }

    }
}
