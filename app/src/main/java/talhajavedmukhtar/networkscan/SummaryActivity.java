package talhajavedmukhtar.networkscan;

import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public class SummaryActivity extends AppCompatActivity {
    final String TAG = Tags.makeTag("Summary");

    private MacToVendorMap macToVendorMap;

    private String message;
    private ArrayList<String> uniqueIps;
    private ArrayList<String> devices;

    private TextView messageTV;
    private ListView uniqueIpsLV;
    private Button closeButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        macToVendorMap = new MacToVendorMap(this);
        devices = new ArrayList<>();

        uniqueIps = (ArrayList<String>) getIntent().getExtras().getStringArrayList("addressList");
        int totalDevices = uniqueIps.size();
        int additionalDevices = 0;

        uniqueIpsLV = (ListView) findViewById(R.id.deviceAddresses);
        ArrayAdapter adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, android.R.id.text1, devices);
        uniqueIpsLV.setAdapter(adapter);

        closeButton = (Button) findViewById(R.id.closeButton);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new FileReader("/proc/net/arp"));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                Log.d(TAG,"arpFile: " + line);
                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4) {
                    String ip = splitted[0];
                    String mac = splitted[3];
                    if (mac.matches("..:..:..:..:..:..")) {
                        if (!mac.equals("00:00:00:00:00:00")){
                            Log.d(TAG,">> " + ip + " : " + mac);
                            String vendor = macToVendorMap.findVendor(mac);

                            String deviceInfo = "";
                            if(!uniqueIps.contains(ip)){
                                deviceInfo += "(A) ";
                                additionalDevices += 1;
                            }else{
                                deviceInfo += "(S) ";
                            }

                            if(vendor != null){
                                deviceInfo += ip + ": " + mac + ": " + vendor;
                                devices.add(deviceInfo);
                            }else {
                                deviceInfo += ip + ": " + mac;
                                devices.add(deviceInfo);
                            }
                            adapter.notifyDataSetChanged();

                            if(uniqueIps.contains(ip)){
                                uniqueIps.remove(ip);
                            }
                        }
                    }
                }
            }

            for(String ip: uniqueIps){
                //Check if self
                try{
                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    String selfIp = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
                    if(ip.equals(selfIp)){
                        String mac = wifiManager.getConnectionInfo().getMacAddress();
                        String vendor = macToVendorMap.findVendor(mac);
                        String deviceInfo = "(S) ";
                        if(vendor != null){
                            deviceInfo += ip + ": " + mac + ": " + vendor;
                            devices.add(deviceInfo);
                        }else {
                            deviceInfo += ip + ": " + mac;
                            devices.add(deviceInfo);
                        }
                    }else{
                        devices.add("(S) " + ip);
                        adapter.notifyDataSetChanged();
                    }

                }catch (Exception ex){
                    Log.d(TAG,ex.getMessage());
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            try {
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        message = totalDevices + " device(s) discovered through scans (S) \n "
                    + additionalDevices + " additional device(s) discovered through ARP Table (A) ";

        messageTV = (TextView) findViewById(R.id.totalDevices);
        messageTV.setText(message);
    }


}
