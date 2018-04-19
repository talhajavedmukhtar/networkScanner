package talhajavedmukhtar.networkscan;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private EditText ip;
    private EditText cidr;
    private EditText ports;
    private EditText timeout;
    private Button scan;
    private Button scanPorts;
    private Button saveOutput;
    private Button home;
    private Button upnpDiscover;
    private Button mdnsDiscover;
    private ListView openHostsView;

    private HostScanner hostScanner;
    private PortScanner portScanner;

    public ProgressBar progressBar;

    private Context context;

    public static String deviceIp;

    public static Handler UIHandler;

    static
    {
        UIHandler = new Handler(Looper.getMainLooper());
    }
    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ip = (EditText) findViewById(R.id.ip);
        cidr = (EditText) findViewById(R.id.cidr);
        ports = (EditText) findViewById(R.id.ports);
        timeout = (EditText) findViewById(R.id.timeout);
        scan = (Button) findViewById(R.id.scan);
        scanPorts = (Button) findViewById(R.id.scanPorts);
        saveOutput = (Button) findViewById(R.id.saveOutput);
        home = (Button) findViewById(R.id.googleHome);
        upnpDiscover = (Button) findViewById(R.id.upnpDiscover);
        mdnsDiscover = (Button) findViewById(R.id.mdnsDiscover);
        openHostsView = (ListView) findViewById(R.id.openHosts);

        progressBar = (ProgressBar) findViewById(R.id.pbLoading);

        context = this;

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        deviceIp = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        ip.setText(deviceIp);

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                String ipAd = ip.getText().toString();
                int cid = Integer.parseInt(cidr.getText().toString());

                int tO = Integer.parseInt(timeout.getText().toString());

                /*hostScanner = new HostScanner(ipAd,cid,context);
                hostScanner.execute(Integer.parseInt(timeout.getText().toString()),2);
                //hostScanner.executeArp();

                ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(getApplicationContext(),
                        android.R.layout.simple_list_item_1, android.R.id.text1, hostScanner.openHosts){
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView textView = (TextView) super.getView(position, convertView, parent);
                        textView.setTextColor(R.color.colorPrimaryDark);
                        return textView;
                    }
                };

                openHostsView.setAdapter(myAdapter);*/

                scanHosts scanner = new scanHosts(ipAd,cid,context,openHostsView,tO);
                scanner.execute();
            }
        });


        scanPorts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                String ipAd = ip.getText().toString();
                int cid = Integer.parseInt(cidr.getText().toString());
                String portsToScan = ports.getText().toString();

                hostScanner = new HostScanner(ipAd,cid,getApplicationContext());
                hostScanner.execute(500,2);

                portScanner = new PortScanner(hostScanner.openHosts,portsToScan);
                portScanner.execute(10000);

                ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(getApplicationContext(),
                        android.R.layout.simple_list_item_1, android.R.id.text1, portScanner.openPortsHosts){
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        TextView textView = (TextView) super.getView(position, convertView, parent);
                        textView.setTextColor(R.color.colorPrimaryDark);
                        return textView;
                    }
                };

                openHostsView.setAdapter(myAdapter);
            }
        });

        saveOutput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = " ";
                if(hostScanner != null){
                    message += "Open Hosts Data: \n";
                    for(String s: hostScanner.openHosts){
                        message += s + "\n";
                    }
                    message += "------------\n";
                }

                if(portScanner != null){
                    message += "Open Ports Data: \n";
                    for(String s: portScanner.openPortsHosts){
                        message += s + "\n";
                    }
                    message += "------------\n";
                }

                if(portScanner == null && hostScanner == null){
                    Toast.makeText(getApplicationContext(),"Nothing to save!",Toast.LENGTH_SHORT).show();
                }else{
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:")); // only email apps should handle this
                    intent.putExtra(Intent.EXTRA_TEXT,message);
                    intent.putExtra(Intent.EXTRA_SUBJECT, "NetworkScanner Data");
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }

            }
        });

        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ipAd = ip.getText().toString();
                int cid = Integer.parseInt(cidr.getText().toString());
                String portsToScan = ports.getText().toString();

                hostScanner = new HostScanner(ipAd,cid,getApplicationContext());
                hostScanner.execute(500,2);

                portScanner = new PortScanner(hostScanner.openHosts,portsToScan);
                portScanner.execute(500);

                for(String ipPort: portScanner.openPortsHosts){
                    new GHRequest().execute("http://"+ipPort+"/setup/eureka_info?params=build_info",ipPort);
                }
            }
        });

        upnpDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //new UPnPDiscovery(context,openHostsView).execute();
            }
        });

        mdnsDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //new multicastDNSDiscovery(context,openHostsView).execute();
                //new NsdClient(context,openHostsView).execute();
            }
        });

    }

    public class GHRequest extends AsyncTask<String, Void, String> {
        public static final String REQUEST_METHOD = "GET";
        public static final int READ_TIMEOUT = 15000;
        public static final int CONNECTION_TIMEOUT = 15000;

        String ipAndPort;

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
                    Toast.makeText(getApplicationContext(),"Google Home found at " + ip + " , Firmware Version: " + firmwareVersion,Toast.LENGTH_LONG).show();
                    Log.d("GoogleHomeResult",map.get("cast_build_revision"));

                }catch (Exception e){
                    Log.d("GoogleHomeResult",e.getMessage());
                }
                //Log.d("GoogleHome",result);
            }
        }
    }


}
