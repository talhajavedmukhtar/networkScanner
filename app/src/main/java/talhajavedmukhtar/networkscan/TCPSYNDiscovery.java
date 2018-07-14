package talhajavedmukhtar.networkscan;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.security.auth.callback.Callback;

/**
 * Created by Talha on 4/4/18.
 */

public class TCPSYNDiscovery extends AsyncTask{
    final static String TAG = Tags.makeTag("TCPSYNDiscovery");
    String ipAddress;
    int cidr;
    int timeout;

    static Context mContext;

    private static ArrayList<Host> discoveredHosts;
    private static ArrayList<String> responses;
    private static ArrayAdapter<String> responseAdapter;

    private ArrayList<String> addresses;

    private ExecutorService executorService;

    int max;

    int tasksDone;
    int tasksAdded;

    private ProgressBar progressBar;


    TCPSYNDiscovery(String ipAd, int c, Context context, ArrayList<Host> hosts, ArrayList<String> resp, ArrayAdapter<String> adap, int tO){
        ipAddress = ipAd;
        cidr = c;

        mContext = context;

        discoveredHosts = hosts;
        responses = resp;
        responseAdapter = adap;
        timeout = tO;

        tasksDone = 0;
        tasksAdded = 0;

        progressBar = (ProgressBar) ((Activity)context).findViewById(R.id.pbLoading);
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
                Toast.makeText(mContext,"TCP Discovery started",Toast.LENGTH_SHORT).show();
            }
        });

        addresses = getAddressRange(ipAddress,cidr);

        max = addresses.size();
        progressBar.setMax(max+(int)(0.1*max));

        int noOfThreads = 200;

        executorService = Executors.newFixedThreadPool(noOfThreads);

        tasksAdded = 0;
        while(!addresses.isEmpty() && tasksAdded != (noOfThreads-1)){
            synchronized (this){
                final String ip = addresses.get(0);
                addresses.remove(0);
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        hostIsActive(ip,timeout);
                    }
                });
                tasksAdded++;
            }
        }

        while(!executorService.isShutdown()){
            //
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        progressBar.setProgress((int)values[0]);
        Log.d(TAG,"New progress: "+Integer.toString((int)values[0]));
    }

    private void hostIsActive(final String ip , final int timeout){
        Log.d(TAG,"Running for ip: "+ ip);

        Boolean open = false;
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, 7), timeout);
            socket.close();
            open = true;
        } catch (Exception ex) {
            Log.d(TAG+".SocketError",ex.getMessage() + " for ip: " + ip);
            if(ex.getMessage().contains("ECONNREFUSED")){
                open = true;
            }else{
                open = false;
            }
        } finally {
            if(open){
                final String add = ip + " : discovered through TCP SYN";
                MainActivity.runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        discoveredHosts.add(new Host(ip,"TCP SYN"));
                        responses.add(add);
                        responseAdapter.notifyDataSetChanged();

                    }
                });
            }


            progressBar.setProgress(tasksDone);
            synchronized (this){
                tasksDone += 1;
            }
            if(tasksDone == max){
                Log.d(TAG,tasksDone + " out of " + max + " done" + "shutting down executor now");
                executorService.shutdown();
            }else{
                Log.d(TAG,tasksDone + " out of " + max + " done");
                synchronized (this){
                    if(!addresses.isEmpty()){
                        final String nextIp = addresses.get(0);
                        addresses.remove(0);
                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                hostIsActive(nextIp,timeout);
                            }
                        });
                        tasksAdded++;
                    }
                }
            }
        }
    }

}

