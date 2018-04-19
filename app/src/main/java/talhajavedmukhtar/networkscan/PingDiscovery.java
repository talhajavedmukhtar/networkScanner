package talhajavedmukhtar.networkscan;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Talha on 4/7/18.
 */

public class PingDiscovery extends AsyncTask {
    final static String TAG = Tags.makeTag("PingDiscovery");
    String ipAddress;
    int cidr;

    static Context mContext;

    private static ArrayList<Host> discoveredHosts;
    private static ArrayList<String> responses;
    private static ArrayAdapter<String> responseAdapter;

    private ProgressBar progressBar;

    PingDiscovery(String ipAd, int c, Context context, ArrayList<Host> hosts, ArrayList<String> resp, ArrayAdapter<String> adap){
        ipAddress = ipAd;
        cidr = c;

        mContext = context;

        discoveredHosts = hosts;
        responses = resp;
        responseAdapter = adap;

        progressBar = (ProgressBar) ((Activity)context).findViewById(R.id.pbLoading);
    }


    public static Future<Boolean> hostIsActive(final ExecutorService es, final String ip){
        final Runtime runtime = Runtime.getRuntime();
        return es.submit(new Callable<Boolean>(){
            @Override
            public Boolean call() throws Exception {
                try {
                    Process  mIpAddrProcess = runtime.exec("/system/bin/ping -c 1 "+ip);
                    int mExitValue = mIpAddrProcess.waitFor();

                    Log.d(TAG,"Exit Message: " + Integer.toString(mExitValue));
                    if(mExitValue==0){
                        return true;
                    }else{
                        return false;
                    }
                } catch (Exception ex) {
                    Log.d(TAG,ex.getMessage());
                    return false;
                }
            }
        });
    }

    public ArrayList getAddressRange(String ip, int cidr){
        //double numHosts = Math.pow(2,(32 - cidr));
        int hostBits = 32 - cidr;
        ArrayList<String> addresses = new ArrayList<>();

        int octet = 0;
        while(hostBits > 8){
            hostBits -= 8;
            octet += 1;
        }

        String[] octetArray = ip.split("\\.");

        String oct = Integer.toBinaryString(Integer.parseInt(octetArray[3 - octet]));


        //initial octet after applying subnet mask
        int pointer = oct.length() - 1;
        for(int i = 0; i < hostBits && pointer != -1; i++){
            char[] octChars = oct.toCharArray();
            octChars[pointer] = '0';
            oct = String.valueOf(octChars);
            pointer--;
        }


        for(int i = 0; i <= Math.pow(2,hostBits)-1; i++){
            String hostPart = Integer.toBinaryString(i);
            //adding this host to the masked ip
            String updatedOctet;
            try{
                updatedOctet = oct.substring(0,oct.length()-hostPart.length()) + hostPart;
            }catch (Exception ex){
                updatedOctet = hostPart;
            }

            int intFromOctet = Integer.parseInt(updatedOctet,2);
            octetArray[3-octet] = Integer.toString(intFromOctet);

            if(octet == 0){
                addresses.add(TextUtils.join(".",octetArray));
            }else{
                addresses.addAll(generateAddresses(octetArray,octet,intFromOctet));
            }

        }
        //allHosts = addresses;
        return addresses;

    }

    ArrayList<String> generateAddresses(String[] octetArray, int octetNo, int intFromOctet){
        ArrayList<String> adds = new ArrayList<>();

        switch (octetNo){
            case 1:
                for(int i = 0; i < 256; i++){
                    octetArray[3] = Integer.toString(i);
                    adds.add(TextUtils.join(".",octetArray));
                }
                return adds;
            case 2:
                for(int i = 0; i < 256; i++){
                    for(int j = 0; j < 256; j++){
                        octetArray[2] = Integer.toString(i);
                        octetArray[3] = Integer.toString(j);
                        adds.add(TextUtils.join(".",octetArray));
                    }
                }
                return adds;
            case 3:
                for(int i = 0; i < 256; i++){
                    for(int j = 0; j < 256; j++){
                        for(int k = 0; k < 256; k++){
                            octetArray[1] = Integer.toString(i);
                            octetArray[2] = Integer.toString(j);
                            octetArray[3] = Integer.toString(k);
                            adds.add(TextUtils.join(".",octetArray));
                        }
                    }
                }
                return adds;
            default:
                return adds;
        }
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        MainActivity.runOnUI(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext,"Ping Discovery started",Toast.LENGTH_SHORT).show();
            }
        });

        final ExecutorService es = Executors.newFixedThreadPool(20);

        ArrayList<String> addresses = getAddressRange(ipAddress,cidr);
        int max = addresses.size();
        progressBar.setMax(max);

        final ArrayList<Future<Boolean>> futures = new ArrayList<>();
        for (String addr : addresses) {
            futures.add(hostIsActive(es, addr));
        }
        es.shutdown();

        int  i = 0;
        for (final Future<Boolean> f : futures) {
            try{
                if (f.get()) {
                    final String add = addresses.get(futures.indexOf(f));
                    final String resp = add + " : discovered through ICMP Ping";
                    MainActivity.runOnUI(new Runnable() {
                        @Override
                        public void run() {
                            discoveredHosts.add(new Host(add,"ICMP Ping"));
                            responses.add(resp);
                            responseAdapter.notifyDataSetChanged();
                        }
                    });
                    //openHosts.add(addresses.get(futures.indexOf(f)));
                }
            }catch (Exception e){
                Log.d(TAG,e.getMessage());
            }finally {
                i += 1;
                publishProgress(i);
            }
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        progressBar.setProgress((int)values[0]);
        //Log.d(TAG,"New progress: "+Integer.toString((int)values[0]));
    }
}
