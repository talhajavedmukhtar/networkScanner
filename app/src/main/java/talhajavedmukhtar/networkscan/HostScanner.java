package talhajavedmukhtar.networkscan;

import android.text.TextUtils;
import android.util.Log;

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

    public ArrayList<String> openHosts;
    public ArrayList<String> allHosts;

    HostScanner(String ip, int cidr){
        this.ip = ip;
        this.cidr = cidr;
        openHosts = new ArrayList<>();
        allHosts = new ArrayList<>();
    }

    public static Future<Boolean> hostIsActive(final ExecutorService es, final String ip , final int timeout){
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
    }

    public void execute(int timeout){
        final ExecutorService es = Executors.newFixedThreadPool(20);

        ArrayList<String> addresses = getAddressRange(ip,cidr);

        final ArrayList<Future<Boolean>> futures = new ArrayList<>();
        for (String addr : addresses) {
            futures.add(hostIsActive(es, addr, timeout));
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


        for(int i = 0; i < Math.pow(2,hostBits)-1; i++){
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
                //TODO
                /*int numBits = octet*8;
                double largest = Math.pow(2,numBits) - 1;

                for(int j = 0; j < largest; j++){
                    String toAppend = Integer.toBinaryString(j);
                    for(int k = 0; k < octet; k++){
                        octetArray[3-k] = "0";

                    }
                }*/
            }

        }
        allHosts = addresses;
        return addresses;

    }



}
