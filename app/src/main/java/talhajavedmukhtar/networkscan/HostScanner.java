package talhajavedmukhtar.networkscan;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import org.pcap4j.core.PcapNativeException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Talha on 2/8/18.
 */
public class HostScanner {
    private String ip;
    private int cidr;
    private Context context;

    public ArrayList<String> openHosts;
    public ArrayList<String> allHosts;

    static ProgressBar progressBar;
    static int totalHosts;
    static int doneHosts;

    HostScanner(String ip, int cidr, Context c){
        this.ip = ip;
        this.cidr = cidr;
        context = c;
        openHosts = new ArrayList<>();
        allHosts = new ArrayList<>();
        progressBar = (ProgressBar) ((Activity)c).findViewById(R.id.pbLoading);
        doneHosts = 0;
    }

    public static Future<Boolean> hostIsActive(final ExecutorService es, final String ip , final int timeout, int method){
        switch(method){
            default:
                return es.submit(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        try {
                            Socket socket = new Socket();
                            socket.connect(new InetSocketAddress(ip, 7), timeout);
                            socket.close();
                            return true;
                        } catch (Exception ex) {
                            if(ex.getMessage().contains("ECONNREFUSED")){
                                return true;
                            }
                            Log.d("SocketError",ex.getMessage());
                            return false;
                        }
                    }
                });
            case 2:  //tries socket connection on port 7
                return es.submit(new Callable<Boolean>(){
                    @Override
                    public Boolean call() throws Exception {
                        try {
                            Socket socket = new Socket();
                            socket.connect(new InetSocketAddress(ip, 7), timeout);
                            socket.close();
                            return true;
                        } catch (Exception ex) {
                            Log.d("SocketError",ex.getMessage());
                            if(ex.getMessage().contains("ECONNREFUSED")){
                                return true;
                            }
                            return false;
                        } finally {
                            doneHosts += 1;
                            progressBar.setProgress((doneHosts/totalHosts)*100);
                        }
                    }
                });
            case 3:  //Try isReachable (also forms connection on port 7)
                return es.submit(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        try{
                            return InetAddress.getByName(ip).isReachable(timeout);
                        }catch (Exception e){
                            Log.d("isReachableError",e.getMessage());
                            return false;
                        }
                    }
                });
            case 4:
                return es.submit(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        Runtime runtime = Runtime.getRuntime();
                        Log.d("Ping", "About to ping using runtime.exec : "+ip);
                        Process proc = runtime.exec("ping -c 1 " + ip);
                        proc.waitFor();
                        int exit = proc.exitValue();
                        if (exit == 0) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
        }


    }

    public void execute(int timeout, int method){
        final ExecutorService es = Executors.newFixedThreadPool(20);

        ArrayList<String> addresses = getAddressRange(ip,cidr);
        totalHosts = addresses.size();
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(1);

        final ArrayList<Future<Boolean>> futures = new ArrayList<>();
        for (String addr : addresses) {
            futures.add(hostIsActive(es, addr, timeout,method));
        }
        es.shutdown();

        for (final Future<Boolean> f : futures) {
            try{
                if (f.get()) {
                    openHosts.add(addresses.get(futures.indexOf(f)));
                }
            }catch (Exception e){
                Log.d("futureError",e.getMessage());
            }
        }

        //progressBar.setVisibility(View.INVISIBLE);

    }

    public void executeArp(){
        ArrayList<String> addresses = getAddressRange(ip,cidr);

        /*for (String address : addresses){
            try{
                new ArpRequest(address).execute();
            }catch (Exception e){
                Log.d("ARPException",e.getMessage());
            }

        }*/

        try {
            new ArpRequest("192.168.100.4",context).execute(1000);
        } catch (PcapNativeException e) {
            Log.d("ARPException0",e.getMessage());
        }
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
        allHosts = addresses;
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

}
