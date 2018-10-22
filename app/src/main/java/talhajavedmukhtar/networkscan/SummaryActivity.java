package talhajavedmukhtar.networkscan;

import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import talhajavedmukhtar.networkscan.BannerGrabbers.SSHBannerGrabber;

public class SummaryActivity extends AppCompatActivity implements PScanParametersDialog.ParametersDialogListener{
    final String TAG = Tags.makeTag("Summary");

    private MacToVendorMap macToVendorMap;

    private String message;
    private ArrayList<String> uniqueIps;
    private ArrayList<String> devices;

    private TextView messageTV;
    private ListView uniqueIpsLV;
    private Button closeButton;
    private Button saveButton;
    private Button portScanButton;
    private Button bannerGrabButton;

    private int timeout;
    private int noOfThreads;
    private ArrayList<String> selectedIps;

    public static ArrayList<Host> discoveredHosts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        devices = new ArrayList<>();
        discoveredHosts = new ArrayList<>();

        message = "";

        uniqueIps = (ArrayList<String>) getIntent().getExtras().getStringArrayList("addressList");
        int totalDevices = uniqueIps.size();
        int additionalDevices = 0;

        uniqueIpsLV = (ListView) findViewById(R.id.deviceAddresses);
        ArrayAdapter adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice, android.R.id.text1, devices);
        uniqueIpsLV.setAdapter(adapter);

        closeButton = (Button) findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = " ";
                data += "------------------\n";
                for(String deviceInfo: devices){
                    data += deviceInfo + "\n";
                }
                data += "------------------\n";


                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:tjaved.bscs15seecs@seecs.edu.pk")); // only email apps should handle this
                intent.putExtra(Intent.EXTRA_TEXT,data);
                intent.putExtra(Intent.EXTRA_SUBJECT, "NetworkScanner Data");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });

        BufferedReader bufferedReader = null;

        MyApp app = (MyApp) getApplication();

        //wait until map is ready
        while (!app.isMapReady()){

        }

        macToVendorMap = app.getMap();

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
                                deviceInfo += ip + " || " + mac + " || " + vendor;
                                devices.add(deviceInfo);
                                discoveredHosts.add(new Host(ip,mac,vendor));
                            }else {
                                deviceInfo += ip + " || " + mac;
                                devices.add(deviceInfo);
                                discoveredHosts.add(new Host(ip,mac,null));
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
                            deviceInfo += ip + " || " + mac + " || " + vendor;
                            devices.add(deviceInfo);
                            discoveredHosts.add(new Host(ip,mac,vendor));
                        }else {
                            deviceInfo += ip + " || " + mac;
                            devices.add(deviceInfo);
                            discoveredHosts.add(new Host(ip,mac,null));
                        }
                    }else{
                        devices.add("(S) " + ip);
                        discoveredHosts.add(new Host(ip,null,null));
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

        //initially all items are checked
        uniqueIpsLV.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        for(int i = 0; i < devices.size(); i++){
            uniqueIpsLV.setItemChecked(i,true);
        }

        portScanButton = (Button) findViewById(R.id.portScanButton);
        portScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SparseBooleanArray arr = uniqueIpsLV.getCheckedItemPositions();

                selectedIps = new ArrayList<>();

                //pass list of ips to next activity
                for(int i = 0; i < arr.size(); i++){
                    if(arr.valueAt(i)){
                        selectedIps.add(discoveredHosts.get(i).getIpAd());
                    }
                }

                PScanParametersDialog paraDialog = new PScanParametersDialog();
                Bundle bundle = new Bundle();
                bundle.putInt("noOfIPs",selectedIps.size());
                paraDialog.setArguments(bundle);
                paraDialog.show(getFragmentManager(),"paraDialog");
            }
        });

        bannerGrabButton = (Button) findViewById(R.id.bannerGrab);
        bannerGrabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*for (Host h: discoveredHosts) {
                    String ip = h.getIpAd();
                    Log.d(TAG,"ip: "+ip);
                    SSHBannerGrabber sshBannerGrabber = new SSHBannerGrabber();
                    sshBannerGrabber.execute(ip,10000,10000);
                }*/
                SparseBooleanArray arr = uniqueIpsLV.getCheckedItemPositions();

                selectedIps = new ArrayList<>();

                //pass list of ips to next activity
                for(int i = 0; i < arr.size(); i++){
                    if(arr.valueAt(i)){
                        selectedIps.add(discoveredHosts.get(i).getIpAd());
                    }
                }

                Intent intent = new Intent(getBaseContext(), BannerGrabActivity.class);
                intent.putExtra("selectedIps",selectedIps);
                startActivity(intent);
            }
        });

        message += totalDevices + " device(s) discovered through scans (S) \n "
                    + additionalDevices + " additional device(s) discovered through ARP Table (A) ";

        messageTV = (TextView) findViewById(R.id.totalDevices);
        messageTV.setText(message);
    }


    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        timeout = ((PScanParametersDialog)dialog).getTimeout() * 1000;
        noOfThreads = ((PScanParametersDialog)dialog).getNoOfThreads();

        Intent intent = new Intent(getBaseContext(), PortScanActivity.class);
        intent.putExtra("selectedIps",selectedIps);
        intent.putExtra("timeout",timeout);
        intent.putExtra("noOfThreads",noOfThreads);
        startActivity(intent);
    }
}
