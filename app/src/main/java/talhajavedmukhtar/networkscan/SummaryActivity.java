package talhajavedmukhtar.networkscan;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
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

        message = totalDevices + " device(s) discovered: ";

        messageTV = (TextView) findViewById(R.id.totalDevices);
        messageTV.setText(message);

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
                            if(vendor != null){
                                devices.add(ip + ": " + mac + ": " + vendor);
                            }else {
                                devices.add(ip + ": " + mac);
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
                devices.add(ip);
                adapter.notifyDataSetChanged();
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

    }
}
