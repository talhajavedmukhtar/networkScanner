package talhajavedmukhtar.networkscan;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by Talha on 4/4/18.
 */

public class scanHosts extends AsyncTask{
    final String TAG = Tags.makeTag("scanHosts");

    String ipAddress;
    int cidr;
    int timeout;

    static Context mContext;

    private static ArrayList<String> responses;
    private ListView responseView;
    private static ArrayAdapter<String> responseAdapter;

    scanHosts(String ipAd, int c, Context context, ListView view, int tO){
        ipAddress = ipAd;
        cidr = c;
        timeout = tO;

        mContext = context;

        responses = new ArrayList<>();
        responseView = view;
        responseAdapter = new ArrayAdapter<String>(context,android.R.layout.simple_list_item_1, android.R.id.text1, responses);
        responseView.setAdapter(responseAdapter);

    }


    @Override
    protected Object doInBackground(Object[] objects) {
        int i = 0;
        try{
            ArrayList<AsyncTask> tasks = new ArrayList<>();
            tasks.add(new TCPEchoDiscovery(ipAddress,cidr,mContext,responseView,responses,responseAdapter,timeout));
            tasks.add(new NsdClient(mContext,responseView,responses,responseAdapter));
            tasks.add(new UPnPDiscovery(mContext,responseView,responses,responseAdapter,timeout));
            tasks.add(new PingDiscovery(ipAddress,cidr,mContext,responseView,responses,responseAdapter));


            for(AsyncTask aTask: tasks){
                aTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                //aTask.execute();
                while(aTask.getStatus() != android.os.AsyncTask.Status.FINISHED){
                    Log.d(TAG,"Waiting for " + Integer.toString(i));
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
    }
}
