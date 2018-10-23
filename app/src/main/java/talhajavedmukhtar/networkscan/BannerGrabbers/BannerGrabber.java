package talhajavedmukhtar.networkscan.BannerGrabbers;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Trace;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import talhajavedmukhtar.networkscan.BannerGrabActivity;
import talhajavedmukhtar.networkscan.PortScanActivity;
import talhajavedmukhtar.networkscan.Tags;

/**
 * Created by Talha on 8/4/18.
 */

public class BannerGrabber extends AsyncTask{
    private final String TAG = Tags.makeTag("BannerGrabber");

    private int connectionTimeout;
    private int grabTimeout;

    private ArrayList<String> ips;
    private ArrayList<String> bannerMessagesRaw;
    private ArrayList<String> bannerMessagesDisplay;
    private ArrayAdapter<String> adapter;

    private BannerGrabActivity bannerGrabActivity;

    public BannerGrabber(Context ctx, ArrayList<String> ipAds, ArrayList<String> bMD, ArrayAdapter<String> adap, ArrayList<String> bMR, int cTO, int gTO){
        ips = ipAds;

        Collections.sort(ips, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                Log.d(TAG,o1 + " and " + o2);
                int last1 = Integer.parseInt((o1.split("\\."))[3]);
                int last2 = Integer.parseInt((o2.split("\\."))[3]);

                if (last1 > last2) {
                    return 1;
                }else if(last1 < last2){
                    return -1;
                }else{
                    return 0;
                }
            }
        });

        connectionTimeout = cTO;
        grabTimeout = gTO;

        bannerMessagesRaw = bMR;
        bannerMessagesDisplay = bMD;
        adapter = adap;

        bannerGrabActivity = (BannerGrabActivity) ctx;
    }

    public ArrayList<String> grabForIP(String ip){
        HTTPBannerGrabber httpBannerGrabber = new HTTPBannerGrabber();
        String httpBanner = httpBannerGrabber.execute(ip,connectionTimeout,grabTimeout);
        SSHBannerGrabber sshBannerGrabber = new SSHBannerGrabber();
        String sshBanner = sshBannerGrabber.execute(ip,connectionTimeout,grabTimeout);

        ArrayList<String> banners = new ArrayList<>();

        banners.add(sshBanner);
        banners.add(httpBanner);

        return banners;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        for(int i = 0; i < ips.size(); i++){
            String currentIp = ips.get(i);

            Log.d(TAG,currentIp + "start");
            ArrayList<String> banners = grabForIP(currentIp);
            Log.d(TAG,currentIp + "end");

            bannerMessagesRaw.add(currentIp + " : SSH : " + banners.get(0));
            bannerMessagesDisplay.add(currentIp + " : SSH : " + getBannerDisplay(banners.get(0)));

            bannerMessagesRaw.add(currentIp + " : HTTP : " + banners.get(1));
            bannerMessagesDisplay.add(currentIp + " : HTTP : " + getBannerDisplay(banners.get(1)));


            BannerGrabActivity.runOnUI(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });


            publishProgress(i+1);
        }

        return null;
    }

    private String getBannerDisplay(String rawBanner){
        int limit = 40;
        if (rawBanner.length() <= limit){
            return rawBanner;
        }else{
            return (rawBanner.substring(0,limit) + "...");
        }
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        super.onProgressUpdate(values);
        bannerGrabActivity.updateIPsDone((int)values[0]);
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        bannerGrabActivity.enableSave();
    }
}
