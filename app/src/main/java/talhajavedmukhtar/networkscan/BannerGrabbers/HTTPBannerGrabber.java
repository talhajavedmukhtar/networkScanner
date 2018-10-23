package talhajavedmukhtar.networkscan.BannerGrabbers;

import android.nfc.Tag;
import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import talhajavedmukhtar.networkscan.Tags;

/**
 * Created by Talha on 8/4/18.
 */

public class HTTPBannerGrabber {
    private int portNo;
    private final String TAG;
    private String bannerGrabString;

    private Socket connectionSocket;

    public String result;

    public HTTPBannerGrabber(){
        portNo = 80;
        TAG = Tags.makeTag("HTTPBannerGrabber");
        bannerGrabString = "GET / HTTP/1.1\r\n\r\n";

        connectionSocket = new Socket();
    }

    public String execute(String ipAd, int cTO, int gTO){
        final String ip = ipAd;
        final int connectionTimeout = cTO;
        int grabTimeout = (int) gTO;

        ExecutorService service = Executors.newSingleThreadExecutor();

        Future<String> f = service.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return grabBanner(ip,connectionTimeout);
            }
        });

        String banner;
        try {
            banner = f.get(grabTimeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.d(TAG,e.getMessage());
            banner = "";
        }

        result = banner;

        return banner;
    }

    /*protected String doInBackground(Object[] objects) {
        final String ip = (String) objects[0];
        final int connectionTimeout = (int) objects[1];
        int grabTimeout = (int) objects[2];

        ExecutorService service = Executors.newSingleThreadExecutor();

        Future<String> f = service.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return grabBanner(ip,connectionTimeout);
            }
        });

        String banner;
        try {
            banner = f.get(grabTimeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.d(TAG,e.getMessage());
            banner = "";
        }

        return banner;
    }*/


    private Boolean formConnection(String ip, int timeout){
        try{
            connectionSocket.connect(new InetSocketAddress(ip,portNo),timeout);
            return true;
        }catch (Exception ex){
            ex.printStackTrace();
            Log.d(TAG,"Could not connect to "+ ip + " at port "+ Integer.toString(portNo) + " " + ex.getMessage());
            return false;
        }
    }

    private String getResponse(){
        String response = "";

        try{
            PrintWriter out = new PrintWriter(connectionSocket.getOutputStream(), true);
            out.print(bannerGrabString);
            out.flush();
            InputStream is = connectionSocket.getInputStream();
            byte[] buffer = new byte[1024];
            int read;
            Log.d(TAG,"initiating reading...");

            while((read = is.read(buffer)) != -1) {
                String output = new String(buffer, 0, read);
                Log.d(TAG,output);

                if(output.length() > 0){
                    response += output;
                }
            };

        }catch (Exception ex){
            Log.d(TAG,ex.getMessage());
        }


        return response;
    }

    private String grabBanner(String ip,int connectionTimeout){
        String message = "";
        if(formConnection(ip,connectionTimeout)){
            Log.d(TAG,"Connected to host "+ ip);
            String response = getResponse();
            if(response.equals("")){
                message += "Banner could not be grabbed";
            }else{
                message += response;
            }
        }else{
            message += "Banner could not be grabbed";
        }

        try{
            connectionSocket.close();
        }catch (Exception ex){
            Log.d(TAG,ex.getMessage());
        }

        Log.d(TAG,"For ip "+ip+" , message: "+message);

        return message;
    }

    /*@Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        result = (String)o;
    }*/
}
