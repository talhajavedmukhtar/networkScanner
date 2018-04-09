package talhajavedmukhtar.networkscan;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by Talha on 4/4/18.
 */

public class scanHosts {
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

        try{
            //new TCPEchoDiscovery(ipAd,cidr,mContext,responseView,responses,responseAdapter,timeout).execute();
            //new NsdClient(mContext,responseView,responses,responseAdapter).execute();
            //new UPnPDiscovery(mContext,responseView,responses,responseAdapter).execute();
            new UDPEchoDiscovery(ipAd,cidr,mContext,responseView,responses,responseAdapter,timeout).execute();
            //new PingDiscovery(ipAd,cidr,mContext,responseView,responses,responseAdapter).execute();
        }catch (Exception ex){
            Log.d(TAG,ex.getMessage());
        }


    }



}
