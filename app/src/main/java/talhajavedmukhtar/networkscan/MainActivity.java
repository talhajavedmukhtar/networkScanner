package talhajavedmukhtar.networkscan;

import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;

import talhajavedmukhtar.networkscan.BannerGrabbers.BannerGrabber;
import talhajavedmukhtar.networkscan.BannerGrabbers.HTTPBannerGrabber;
import talhajavedmukhtar.networkscan.BannerGrabbers.SSHBannerGrabber;

public class MainActivity extends AppCompatActivity {
    final String TAG = Tags.makeTag("Main");

    private EditText ip;
    private EditText cidr;
    private EditText timeout;
    private Button scan;
    private Button viewSummary;
    private Button grabBanners;

    private ListView openHostsView;

    private HostScanner hostScanner;

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

    MyApp applicationData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ip = (EditText) findViewById(R.id.ip);
        cidr = (EditText) findViewById(R.id.cidr);
        timeout = (EditText) findViewById(R.id.timeout);
        scan = (Button) findViewById(R.id.scan);
        viewSummary = (Button) findViewById(R.id.viewSummary);
        viewSummary.setEnabled(false);
        //grabBanners = (Button) findViewById(R.id.grabBanners);
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
                viewSummary.setEnabled(false);
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                String ipAd = ip.getText().toString();
                int cid = Integer.parseInt(cidr.getText().toString());

                int tO = Integer.parseInt(timeout.getText().toString());

                hostScanner = new HostScanner(ipAd,cid,context,openHostsView,tO*1000 /*from seconds to ms*/);
                hostScanner.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

        /*grabBanners.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> uniqueIps = new ArrayList<>();
                for(Host h: hostScanner.discoveredHosts){
                    String ip = h.getIpAd();
                    if(!uniqueIps.contains(ip)){
                        uniqueIps.add(ip);
                    }
                }
                Intent intent = new Intent(getBaseContext(), BannerGrabActivity.class);
                intent.putExtra("addressList",uniqueIps);
                startActivity(intent);
                //SSHBannerGrabber sshBannerGrabber = new SSHBannerGrabber();
                //sshBannerGrabber.execute("192.168.100.1",10000,10000);



            }
        });*/

        applicationData = (MyApp) getApplication();
        applicationData.initTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        applicationData.initFinder.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
