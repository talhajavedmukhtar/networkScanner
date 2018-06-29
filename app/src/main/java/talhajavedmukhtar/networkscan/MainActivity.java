package talhajavedmukhtar.networkscan;

import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
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

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    final String TAG = Tags.makeTag("Main");

    private EditText ip;
    private EditText cidr;
    private EditText ports;
    private EditText timeout;
    private Button scan;
    private Button scanPorts;
    private Button saveOutput;
    private Button viewSummary;

    private ListView openHostsView;

    private HostScanner hostScanner;
    private PortScanner portScanner;

    public ProgressBar progressBar;

    private Context context;

    private WifiManager wifiManager;
    private DhcpInfo dhcpInfo;

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
        //ports = (EditText) findViewById(R.id.ports);
        timeout = (EditText) findViewById(R.id.timeout);
        scan = (Button) findViewById(R.id.scan);
        //scanPorts = (Button) findViewById(R.id.scanPorts);
        //scanPorts.setEnabled(false);
        saveOutput = (Button) findViewById(R.id.saveOutput);
        saveOutput.setEnabled(false);
        viewSummary = (Button) findViewById(R.id.viewSummary);
        viewSummary.setEnabled(false);
        openHostsView = (ListView) findViewById(R.id.openHosts);

        progressBar = (ProgressBar) findViewById(R.id.pbLoading);

        context = this;

        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        try{
            dhcpInfo = wifiManager.getDhcpInfo();
            ip.setText(getGateway());
            String CIDR = Integer.toString(getCIDR());
            if (CIDR.equals("0")){
                cidr.setText("24");
            }else {
                cidr.setText(CIDR);
            }
        }catch (Exception ex){
            Log.d(TAG + " Error",ex.getMessage());
            deviceIp = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
            ip.setText(deviceIp);
            cidr.setText("24");
        }


        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //scanPorts.setEnabled(false);
                saveOutput.setEnabled(false);
                viewSummary.setEnabled(false);
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                String ipAd = ip.getText().toString();
                int cid = Integer.parseInt(cidr.getText().toString());

                int tO = Integer.parseInt(timeout.getText().toString());

                hostScanner = new HostScanner(ipAd,cid,context,openHostsView,tO*1000 /*from seconds to ms*/);
                hostScanner.execute();
            }
        });


        /*scanPorts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                String ipAd = ip.getText().toString();
                int cid = Integer.parseInt(cidr.getText().toString());
                String portsToScan = ports.getText().toString();


                portScanner = new PortScanner(context,hostScanner.discoveredHosts,portsToScan,openHostsView);
                portScanner.execute(10000);

            }
        });*/

        saveOutput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int tO = Integer.parseInt(timeout.getText().toString());

                String message = " ";
                message += "Running with a timeout of " + Integer.toString(tO) + " \n";
                if(hostScanner != null){
                    message += "Open Hosts Data: \n";
                    for(Host h: hostScanner.discoveredHosts){
                        message += h.getIpAd() + " discovered through " + h.getDiscoveredThrough() + "\n";
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
                    intent.setData(Uri.parse("mailto:tjaved.bscs15seecs@seecs.edu.pk")); // only email apps should handle this
                    intent.putExtra(Intent.EXTRA_TEXT,message);
                    intent.putExtra(Intent.EXTRA_SUBJECT, "NetworkScanner Data");
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }

            }
        });

        viewSummary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> uniqueIps = new ArrayList<>();
                for(Host h: hostScanner.discoveredHosts){
                    String ip = h.getIpAd();
                    if(!uniqueIps.contains(ip)){
                        uniqueIps.add(ip);
                    }
                }
                Intent intent = new Intent(getBaseContext(), SummaryActivity.class);
                intent.putExtra("addressList",uniqueIps);
                startActivity(intent);
            }
        });

    }



    private int getCIDR() {
        int i = dhcpInfo.netmask;

        ArrayList<String> octets = new ArrayList<>();
        octets.add(Integer.toBinaryString(i & 0xFF));

        for(int j = 1; j < 4; j++){
            octets.add(Integer.toBinaryString((i >>>= 8) & 0xFF));
        }

        int CIDR = 0;


        for (String octet: octets){
            if (octet.equals("11111111")){
                CIDR += 8;
            }else{
                int numOfOnes = 0;

                for(int j = 0; j < 8; j++){
                    if(octet.charAt(j) == '1'){
                        numOfOnes += 1;
                    }else {
                        break;
                    }
                }
                Log.d(TAG,octet + " : " + numOfOnes);
                CIDR += numOfOnes;
                break;
            }
            Log.d(TAG,octet);
        }

        Log.d(TAG + " Size", Integer.toString(CIDR));
        return CIDR;
    }

    private String getGateway(){
        int gatewayInt = dhcpInfo.gateway;
        return (gatewayInt & 0xFF) + "." +
                ((gatewayInt >>>= 8) & 0xFF) + "." +
                ((gatewayInt >>>= 8) & 0xFF) + "." +
                ((gatewayInt >>>= 8) & 0xFF);

    }


}
