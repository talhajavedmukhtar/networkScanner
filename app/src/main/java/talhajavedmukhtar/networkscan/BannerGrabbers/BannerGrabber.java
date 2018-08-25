package talhajavedmukhtar.networkscan.BannerGrabbers;

import android.provider.ContactsContract;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by Talha on 8/4/18.
 */

public class BannerGrabber {
    protected int portNo;
    protected String bannerGrabString;
    private Socket connectionSocket;
    protected String TAG;

    BannerGrabber(){
        connectionSocket = new Socket();
    }

    private Boolean formConnection(String ip, int timeout){
        try{
            connectionSocket.connect(new InetSocketAddress(ip,portNo),timeout);
            return true;
        }catch (Exception ex){
            Log.d(TAG,"Could not connect to "+ ip + " at port "+ Integer.toString(portNo));
            return false;
        }
    }

    private String getResponse(int timeout){
        DataOutputStream dOut;
        DataInputStream dIn;
        String response = "";
        try{
            dOut= new DataOutputStream(connectionSocket.getOutputStream());
            dIn = new DataInputStream(connectionSocket.getInputStream());

            dOut.writeUTF(bannerGrabString);
            response += dIn.readUTF();
        }catch (Exception ex){
            Log.d(TAG,ex.getMessage());
        }

        return response;
    }

    public String grabBanner(String ip,int connectionTimeout, int grabTimeout){
        String message = "";
        if(formConnection(ip,connectionTimeout)){
            String response = getResponse(grabTimeout);
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
}
