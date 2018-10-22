package talhajavedmukhtar.networkscan.BannerGrabbers;

import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import talhajavedmukhtar.networkscan.Tags;

/**
 * Created by Talha on 10/21/18.
 */

public class SSHBannerGrabber{
    private int portNo;
    private final String TAG;
    //private String bannerGrabString;
    private Socket connectionSocket;

    public String result;

    public SSHBannerGrabber(){
        portNo = 22;
        TAG = Tags.makeTag("SSHBannerGrabber");

        //working without the need to send a banner grab string
        //bannerGrabString = "SSH-2.0-NS\r\n";

        connectionSocket = new Socket();
    }

    public String execute(final String ip, final int cTO, int gTO){
        ExecutorService service = Executors.newSingleThreadExecutor();

        Future<String> f = service.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return grabBanner(ip,cTO);
            }
        });

        String banner;
        try {
            banner = f.get(gTO, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.d(TAG,e.getMessage());
            banner = "";
        }

        return banner;
    }


    private Boolean formConnection(String ip, int timeout){
        try{
            connectionSocket.connect(new InetSocketAddress(ip,portNo),timeout);
            return true;
        }catch (Exception ex){
            Log.d(TAG,"Could not connect to "+ ip + " at port "+ Integer.toString(portNo) + " " + ex.getMessage());
            return false;
        }
    }

    private String getResponse(){
        String response = "";

        try{
            InputStream is = connectionSocket.getInputStream();
            byte[] buffer = new byte[1024];
            int read;

            while((read = is.read(buffer)) != -1) {
                String output = new String(buffer, 0, read);
                Log.d(TAG,output);

                if(output.length() > 0){
                    response += output;
                    break;
                }
                /*if(output.length() > 0){
                    response += output.split("\\n")[0];
                    break;
                }*/
            };

        }catch (Exception ex){
            Log.d(TAG,ex.getMessage());
        }

        return response;
    }

    public String grabBanner(String ip,int connectionTimeout){
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

        return message;
    }

    /*
    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        result = (String)o;
    }*/
}
