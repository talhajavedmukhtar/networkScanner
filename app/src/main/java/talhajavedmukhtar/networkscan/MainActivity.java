package talhajavedmukhtar.networkscan;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private EditText ip;
    private EditText cidr;
    private EditText ports;
    private Button scan;
    private Button scanPorts;
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
        openHostsView = (ListView) findViewById(R.id.openHosts);

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ipAd = ip.getText().toString();
                int cid = Integer.parseInt(cidr.getText().toString());

                hostScanner = new HostScanner(ipAd,cid);
                hostScanner.execute(200);

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
                hostScanner.execute(200);

                portScanner = new PortScanner(hostScanner.openHosts,portsToScan);
                portScanner.execute(200);

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

    }
}
