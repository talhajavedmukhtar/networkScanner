package talhajavedmukhtar.networkscan;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Talha on 4/19/18.
 */

public class GHRequest extends AsyncTask <String, Void, String> {
    public static final String REQUEST_METHOD = "GET";
    public static final int READ_TIMEOUT = 15000;
    public static final int CONNECTION_TIMEOUT = 15000;

    String ipAndPort;
    private Context mContext;

    GHRequest(Context context){
        mContext = context;
    }

    @Override
    protected String doInBackground(String... strings) {
        String stringUrl = strings[0];
        ipAndPort = strings[1];
        String result = ""+ipAndPort.split(":")[0]+"@#";

        try {
            Log.d("GoogleHome",stringUrl);

            //Create a URL object holding our url
            URL myUrl = new URL(stringUrl);
            //Create a connection
            HttpURLConnection connection =(HttpURLConnection)
                    myUrl.openConnection();

            //Set methods and timeouts
            connection.setRequestMethod(REQUEST_METHOD);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setConnectTimeout(CONNECTION_TIMEOUT);

            //Connect to our url
            connection.connect();

            //Create a new InputStreamReader
            InputStreamReader streamReader = new
                    InputStreamReader(connection.getInputStream());

            //Create a new buffered reader and String Builder
            BufferedReader reader = new BufferedReader(streamReader);
            StringBuilder stringBuilder = new StringBuilder();
            //Check if the line we are reading is not null
            String inputLine;
            while((inputLine = reader.readLine()) != null){
                stringBuilder.append(inputLine);
            }
            //Close our InputStream and Buffered reader
            reader.close();
            streamReader.close();
            //Set our result equal to our stringBuilder
            result += stringBuilder.toString();

        }catch (Exception e){
            e.printStackTrace();
            Log.d("GoogleHome",e.getMessage());
            result = null;
        }

        return result;
    }

    @Override
    protected void onPostExecute(String result){
        if(result != null){
            try{
                Log.d("GoogleHomeResult",result);
                String ip = result.split("@#")[0];
                String result2 = result.split("@#")[1];
                Log.d("GoogleHomeResult",result2);
                JSONObject obj = new JSONObject(result2);
                JSONObject build_info = obj.getJSONObject("build_info");

                Map<String,String> map = new HashMap<String,String>();
                Iterator iter = build_info.keys();
                while(iter.hasNext()){
                    String key = (String)iter.next();
                    String value = build_info.getString(key);
                    map.put(key,value);
                }
                String firmwareVersion = map.get("cast_build_revision");
                Toast.makeText(mContext.getApplicationContext(),"Google Home found at " + ip + " , Firmware Version: " + firmwareVersion,Toast.LENGTH_LONG).show();
                Log.d("GoogleHomeResult",map.get("cast_build_revision"));

            }catch (Exception e){
                Log.d("GoogleHomeResult",e.getMessage());
            }
            //Log.d("GoogleHome",result);
        }
    }
}
