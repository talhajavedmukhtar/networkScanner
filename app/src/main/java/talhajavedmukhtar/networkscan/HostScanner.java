package talhajavedmukhtar.networkscan;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by Talha on 4/4/18.
 */

public class HostScanner extends AsyncTask{
    final String TAG = Tags.makeTag("HostScanner");

    String ipAddress;
    int cidr;
    int timeout;

    static Context mContext;

    public static ArrayList<Host> discoveredHosts;
    private static ArrayList<String> responses;
    private ListView responseView;
    private static ArrayAdapter<String> responseAdapter;

    HostScanner(String ipAd, int c, Context context, ListView view, int tO){
        ipAddress = ipAd;
        cidr = c;
        timeout = tO;

        mContext = context;

        discoveredHosts = new ArrayList<>();
        responses = new ArrayList<>();
        responseView = view;
        responseAdapter = new ArrayAdapter<String>(context,android.R.layout.simple_list_item_1, android.R.id.text1, responses);
        responseView.setAdapter(responseAdapter);

    }


    @Override
    protected Object doInBackground(Object[] objects) {
        /*new UDPEchoDiscovery(ipAddress,cidr,mContext,responses,responseAdapter,timeout).execute();

        return 1;*/

        int i = 0;
        try{
            ArrayList<AsyncTask> tasks = new ArrayList<>();
            tasks.add(new TCPSYNDiscovery(ipAddress,cidr,mContext,discoveredHosts,responses,responseAdapter,timeout));
            tasks.add(new MDNSDiscovery(mContext,discoveredHosts,responses,responseAdapter));
            tasks.add(new UPnPDiscovery(mContext,discoveredHosts,responses,responseAdapter,timeout));
            tasks.add(new PingDiscovery(ipAddress,cidr,mContext,discoveredHosts,responses,responseAdapter));


            for(AsyncTask aTask: tasks){
                aTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                //aTask.execute();
                while(aTask.getStatus() != android.os.AsyncTask.Status.FINISHED){
                    //Log.d(TAG,"Waiting for " + Integer.toString(i));
                    //wait until this task is done
                }
                i += 1;
                Log.d(TAG,"Done");
            }
            return 1;
        }catch (Exception ex){
            Log.d(TAG,ex.getMessage() + Integer.toString(i));
            return 0;
        }

    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);

        //if successfully completed host discovery
        if((int)o == 1){
            ((Activity) mContext).findViewById(R.id.scanPorts).setEnabled(true);
        }
    }
}
