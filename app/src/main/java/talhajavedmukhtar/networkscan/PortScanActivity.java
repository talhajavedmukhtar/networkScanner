package talhajavedmukhtar.networkscan;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PortScanActivity extends AppCompatActivity {
    static final String TAG = Tags.makeTag("PortScan");

    private ArrayList<String> selectedIps;

    private ProgressBar progressBar;
    private TextView devicesDoneView;
    private ListView openPortsView;
    private Button saveButton;
    private Button closeButton;

    private ArrayList<String> openPortMessages;
    private ArrayAdapter openPortsAdapter;

    private String devicesDoneMessage;

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
        setContentView(R.layout.activity_port_scan);

        int maxPort = 65536;

        progressBar = (ProgressBar) findViewById(R.id.psLoading);
        devicesDoneView = (TextView) findViewById(R.id.devicesDoneView);
        openPortsView = (ListView) findViewById(R.id.openPortsView);
        saveButton = (Button) findViewById(R.id.pSSaveButton);
        closeButton = (Button) findViewById(R.id.psCloseButton);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = " ";
                data += "------------------\n";
                for(Host h: SummaryActivity.discoveredHosts){
                    data += "****\n";

                    data += h.getIpAd() + "\n";

                    if(h.getMacAddress() != null){
                        data += h.getMacAddress() + "\n";
                    }

                    if(h.getVendor() != null){
                        data += h.getVendor() + "\n";
                    }

                    data += "Open ports: { ";

                    ArrayList<Integer> openPorts = h.getOpenPorts();

                    for(int i: openPorts){
                        data += Integer.toString(i) + ", ";
                    }

                    data += "} \n";

                    data += "****\n";
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

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        saveButton.setEnabled(false);
        progressBar.setMax(maxPort);

        selectedIps = getIntent().getExtras().getStringArrayList("selectedIps");
        devicesDoneMessage = "/" + selectedIps.size() + " devices done";

        ArrayList<String> openPortMessages = new ArrayList<>();

        for(String ip: selectedIps){
            openPortMessages.add(ip + " Open Ports Found: 0");
        }

        openPortsAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, android.R.id.text1, openPortMessages);
        openPortsView.setAdapter(openPortsAdapter);

        AsyncTask portScan = new PortScanner(this,selectedIps,openPortMessages,openPortsAdapter,maxPort);
        portScan.execute();
    }

    public void updateDevicesDone(int done){
        devicesDoneView.setText(Integer.toString(done)+devicesDoneMessage);
    }

    public void enableSaveButton(){
        saveButton.setEnabled(true);
    }


}
