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
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static talhajavedmukhtar.networkscan.MainActivity.deviceIp;

/**
 * Created by Talha on 4/7/18.
 */

public class UDPEchoDiscovery extends AsyncTask {
    final static String TAG = Tags.makeTag("UDPEchoDiscovery");
    String ipAddress;
    int cidr;
    int timeout;

    static DatagramSocket rcvSocket;
    static DatagramSocket sendSocket;
    private final static Object syncToken = new Object();

    static Context mContext;

    private static ArrayList<String> responses;
    //private ListView responseView;
    private static ArrayAdapter<String> responseAdapter;

    UDPEchoDiscovery(String ipAd, int c, Context context, ListView view, ArrayList<String> resp, ArrayAdapter<String> adap, int tO){
        ipAddress = ipAd;
        cidr = c;

        mContext = context;

        responses = resp;
        //responseView = view;
        responseAdapter = adap;
        //responseView.setAdapter(responseAdapter);
        timeout = tO;

        try {
            rcvSocket = new DatagramSocket(9999);
            rcvSocket.setSoTimeout(timeout);

            sendSocket = new DatagramSocket();
        }catch (Exception ex){
            Log.d(TAG+".ReceiveSocketError",ex.getMessage());
        }

    }

    @Override
    protected Object doInBackground(Object[] objects) {

        ArrayList<String> addresses = getAddressRange(ipAddress,cidr);

        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName("10.99.27.74");

            // send request
            byte[] buf = new byte[256];

            DatagramPacket packet =
                    new DatagramPacket(buf, buf.length, address, 8070);
            socket.send(packet);

            Log.d(TAG, "Sent!!");


            // get response
            packet = new DatagramPacket(buf, buf.length);


            socket.receive(packet);
            String line = new String(packet.getData(), 0, packet.getLength());


        } catch (SocketException e) {
            Log.e(TAG, "Socket Error:", e);
        } catch (IOException e) {
            Log.e(TAG, "IO Error:", e);
        } catch (Exception e){
            Log.e(TAG, "Error:", e);
        }

        /*try {
            DatagramSocket udpSocket = new DatagramSocket();
            udpSocket.setSoTimeout(100);
            InetAddress serverAddr = InetAddress.getByName("10.99.27.74");
            byte[] buf = ("The String to Send").getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length,serverAddr, 8070);
            udpSocket.send(packet);
            Log.d(TAG,"Done sending!");

            byte[] buf2 = new byte[100];
            DatagramPacket pkt = new DatagramPacket(buf2,buf2.length);
            udpSocket.receive(pkt);
            Log.d(TAG,"Done receiving: " + buf2.toString());

            udpSocket.close();
        } catch (SocketException e) {
            Log.e(TAG, "Socket Error:", e);
        } catch (IOException e) {
            Log.e(TAG, "IO Error:", e);
        } catch (Exception e){
            Log.e(TAG, "Error:", e);
        }*/



        /*for(String adr: addresses){
            byte[] msg = adr.getBytes();
            try{
                DatagramPacket toSend = new DatagramPacket(msg,msg.length,InetAddress.getByName(adr),7);
                sendSocket.send(toSend);
            }catch (Exception ex){
                ex.printStackTrace();
                Log.d(TAG,ex.getMessage());
            }
        }
        Log.d(TAG,"Packets sent out to all addresses");

        byte[] buf = new byte[2048];
        DatagramPacket reply = new DatagramPacket(buf, buf.length);

        while (true){
            try {
                rcvSocket.receive(reply);
                String receivedMsg = new String(buf, 0, reply.getLength());
                Log.d(TAG+".Received Message: ",receivedMsg);

                reply.setLength(buf.length);
            } catch (SocketTimeoutException sockEx){
                Log.d(TAG,"Timeout was reached");
                break;
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG,e.getMessage());
            }
        }

        Log.d(TAG,"here2");*/

        return null;
    }


    /*public static Future<Boolean> hostIsActive(final ExecutorService es, final String ip, final int timeout){
        return es.submit(new Callable<Boolean>(){
            @Override
            public Boolean call() throws Exception {
                try {

                    DatagramSocket sendSocket = new DatagramSocket();
                    //Log.d(TAG+".MyIP",deviceIp);
                    //DatagramSocket rcvSocket = new DatagramSocket(9999);

                    byte[] msg = "echo".getBytes();

                    DatagramPacket toSend = new DatagramPacket(msg,msg.length,InetAddress.getByName(ip),7);
                    sendSocket.send(toSend);

                    byte[] buf = new byte[40];
                    DatagramPacket reply = new DatagramPacket(buf, buf.length);

                    rcvSocket.receive(reply);
                    //rcvSocket.close();
                    sendSocket.close();
                    Log.d(TAG+".Done","executed a thread");
                    Log.d(TAG+".ReplyReceived","Reply: "+new String(buf));
                    return true;
                } catch (SocketTimeoutException sockEx) {
                    Log.d(TAG+".SocketTimeoutError","timeout occured for " + ip);
                    return false;
                } catch (Exception ex){
                    Log.d(TAG+".SocketError",ex.getMessage());
                    return false;
                }
            }
        });
    }*/

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

    /*
    @Override
    protected Object doInBackground(Object[] objects) {
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
    }*/

}