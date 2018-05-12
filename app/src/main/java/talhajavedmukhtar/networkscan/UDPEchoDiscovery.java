package talhajavedmukhtar.networkscan;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static android.content.Context.POWER_SERVICE;
import static talhajavedmukhtar.networkscan.MainActivity.deviceIp;

/**
 * Created by Talha on 4/7/18.
 */

public class UDPEchoDiscovery extends AsyncTask {
    final static String TAG = Tags.makeTag("UDPEchoDiscovery");
    String ipAddress;
    int cidr;
    int timeout;

    static Context mContext;

    private static ArrayList<String> responses;
    private static ArrayAdapter<String> responseAdapter;

    UDPEchoDiscovery(String ipAd, int c, Context context, ArrayList<String> resp, ArrayAdapter<String> adap, int tO){
        ipAddress = ipAd;
        cidr = c;

        mContext = context;

        responses = resp;
        responseAdapter = adap;
        timeout = tO;
    }

    public static Future<Boolean> hostIsActive(final ExecutorService es, final String ip , final int timeout){
        return es.submit(new Callable<Boolean>(){
            @Override
            public Boolean call() throws Exception {
                try {
                    DatagramChannel channel = DatagramChannel.open();
                    channel.socket().setSoTimeout(timeout);
                    channel.connect(new InetSocketAddress(ip,7));
                    if(channel.isConnected()){
                        byte[] bytes = "hello".getBytes();
                        ByteBuffer buf = ByteBuffer.wrap(bytes);
                        channel.write(buf);
                        Log.d(TAG+".DatagramErr","written bytes to " + ip);
                        return false;
                    }else{
                        Log.d(TAG+".DatagramErr","not connected");
                        return false;
                    }
                } catch (PortUnreachableException pEx){
                    Log.d(TAG+".PortUnreachable",pEx.getMessage());
                    return true;
                } catch (Exception ex) {
                    Log.d(TAG+".DatagramErr",ex.getMessage());
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
        /*try {

            DatagramChannel channel = DatagramChannel.open();
            //channel.socket().setSoTimeout(timeout);
            channel.configureBlocking(false);
            //channel.socket().bind(new InetSocketAddress(10000));
            channel.connect(new InetSocketAddress("192.168.100.4",7));
            if(channel.isConnected()){
                byte[] bytes = "hello".getBytes();
                ByteBuffer buf = ByteBuffer.wrap(bytes);
                channel.write(buf);
                Log.d(TAG+".DatagramErr","written bytes");
            }else{
                Log.d(TAG+".DatagramErr","not connected");
            }

        } catch (Exception ex) {
            Log.d(TAG+".DatagramErr",ex.getMessage());
        } finally {
            Log.d(TAG+".DatagramErr","done executing");
        }*/
        final ExecutorService es = Executors.newFixedThreadPool(20);

        ArrayList<String> addresses = getAddressRange(ipAddress,cidr);

        final ArrayList<Future<Boolean>> futures = new ArrayList<>();
        for (String addr : addresses) {
            futures.add(hostIsActive(es, addr, timeout));
        }
        es.shutdown();

        for (final Future<Boolean> f : futures) {
            try{
                if (f.get()) {
                    final String add = addresses.get(futures.indexOf(f)) + " : discovered through UDP Echo";
                    MainActivity.runOnUI(new Runnable() {
                        @Override
                        public void run() {
                            responses.add(add);
                            responseAdapter.notifyDataSetChanged();
                        }
                    });
                    //openHosts.add(addresses.get(futures.indexOf(f)));
                }
            }catch (Exception e){
                e.printStackTrace();
                Log.d(TAG,e.getMessage());
            }
        }

        return null;
    }

}