package talhajavedmukhtar.networkscan;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
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

public class MainActivity extends AppCompatActivity {
    private EditText ip;
    private EditText cidr;
    private EditText ports;
    private EditText timeout;
    private Button scan;
    private Button scanPorts;
    private Button saveOutput;

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
        scanPorts.setEnabled(false);
        saveOutput = (Button) findViewById(R.id.saveOutput);
        saveOutput.setEnabled(false);
        openHostsView = (ListView) findViewById(R.id.openHosts);

        progressBar = (ProgressBar) findViewById(R.id.pbLoading);

        context = this;

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        deviceIp = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        ip.setText(deviceIp);

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanPorts.setEnabled(false);
                saveOutput.setEnabled(false);
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                String ipAd = ip.getText().toString();
                int cid = Integer.parseInt(cidr.getText().toString());

                int tO = Integer.parseInt(timeout.getText().toString());

                hostScanner = new HostScanner(ipAd,cid,context,openHostsView,tO*1000 /*from seconds to ms*/);
                hostScanner.execute();
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


                portScanner = new PortScanner(context,hostScanner.discoveredHosts,portsToScan,openHostsView);
                portScanner.execute(10000);

            }
        });

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

    }


}
