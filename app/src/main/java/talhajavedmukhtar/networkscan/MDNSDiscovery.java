package talhajavedmukhtar.networkscan;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

/**
 * Created by Talha on 3/24/18.
 */
public class MDNSDiscovery extends AsyncTask{
    private static String DEFAULT_TAG = "mDNS Discovery Error";

    private Context mContext;
    private static ArrayList<String> responses;
    private ListView responseView;
    private static ArrayAdapter<String> responseAdapter;

    MDNSDiscovery(Context context, ListView view){
        mContext = context;
        responses = new ArrayList<>();
        responseView = view;
        responseAdapter = new ArrayAdapter<String>(context,android.R.layout.simple_list_item_1, android.R.id.text1, responses);
        responseView.setAdapter(responseAdapter);

    }

    @Override
    protected Object doInBackground(Object[] objects) {
        try {
            // Create a JmDNS instance
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());

            // Add a service listener
            jmdns.addServiceListener("_googlecast._tcp.local.", new SampleListener());

            //Toast.makeText(mContext,"MDNS service discovery initiated",Toast.LENGTH_SHORT).show();

            Log.d(DEFAULT_TAG,"Before waiting");
            // Wait a bit
            Thread.sleep(30000);
            Log.d(DEFAULT_TAG,"After waiting");

            //Toast.makeText(mContext,"MDNS service discovery terminated",Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.d(DEFAULT_TAG,e.toString());
            //Toast.makeText(mContext,"MDNS service discovery terminated unexpectedly",Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    private static class SampleListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
            Log.d(DEFAULT_TAG,event.getInfo().toString());
            responses.add(event.getInfo().toString());
            MainActivity.runOnUI(new Runnable() {
                @Override
                public void run() {
                    responseAdapter.notifyDataSetChanged();
                }
            });

        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            System.out.println("Service removed: " + event.getInfo());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            Log.d(DEFAULT_TAG,event.getInfo().toString());
            System.out.println("Service resolved: " + event.getInfo());
        }
    }
}
