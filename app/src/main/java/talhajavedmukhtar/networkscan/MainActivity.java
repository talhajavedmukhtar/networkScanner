package talhajavedmukhtar.networkscan;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
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
    private Button scan;
    private Button scanPorts;
    private Button saveOutput;
    private Button home;
    private ListView openHostsView;

    private HostScanner hostScanner;
    private PortScanner portScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ip = (EditText) findViewById(R.id.ip);
        cidr = (EditText) findViewById(R.id.cidr);
        ports = (EditText) findViewById(R.id.ports);
        scan = (Button) findViewById(R.id.scan);
        scanPorts = (Button) findViewById(R.id.scanPorts);
        saveOutput = (Button) findViewById(R.id.saveOutput);
        home = (Button) findViewById(R.id.googleHome);
        openHostsView = (ListView) findViewById(R.id.openHosts);

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ipAd = ip.getText().toString();
                int cid = Integer.parseInt(cidr.getText().toString());

                hostScanner = new HostScanner(ipAd,cid);
                hostScanner.execute(500);

                ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(getApplicationContext(),
                        android.R.layout.simple_list_item_1, android.R.id.text1, hostScanner.openHosts){
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

        scanPorts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ipAd = ip.getText().toString();
                int cid = Integer.parseInt(cidr.getText().toString());
                String portsToScan = ports.getText().toString();

                hostScanner = new HostScanner(ipAd,cid);
                hostScanner.execute(500);

                portScanner = new PortScanner(hostScanner.openHosts,portsToScan);
                portScanner.execute(500);

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

                hostScanner = new HostScanner(ipAd,cid);
                hostScanner.execute(500);

                portScanner = new PortScanner(hostScanner.openHosts,portsToScan);
                portScanner.execute(500);

                for(String ipPort: portScanner.openPortsHosts){
                    new GHRequest().execute("http://"+ipPort+"/setup/eureka_info?params=build_info",ipPort);
                }
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
