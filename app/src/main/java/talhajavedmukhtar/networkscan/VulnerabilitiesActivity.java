package talhajavedmukhtar.networkscan;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;

import talhajavedmukhtar.networkscan.BannerGrabbers.Banner;
import talhajavedmukhtar.networkscan.BannerGrabbers.BannerGrabHelper;
import talhajavedmukhtar.networkscan.BannerGrabbers.BannerGrabber;

import static talhajavedmukhtar.networkscan.MainActivity.UIHandler;

public class VulnerabilitiesActivity extends AppCompatActivity {
    private final String TAG = Tags.makeTag("VulnerabilityActivity");

    private ProgressBar progressBar;

    private TextView ipsDoneView;
    private ListView messagesView;
    private Button closeButton;
    private Button saveButton;

    private ArrayList<String> uniqueIps;

    private ArrayList<String> messages;

    private ArrayList<ArrayList<String>> vulnerabilitiesFound;

    private class Target {
        private String type;
        private String banner;
        private String product;
        private String version;

        Target(String t, String b, String p, String v){
            type = t;
            banner = b;
            product = p;
            version = v;
        }


        private String getType(){
            return type;
        }

        private String getBanner(){
            if(banner.length() > 300) {
                return banner.substring(0, 300) + "......";
            }else{
                return banner;
            }
        }

        private String getProduct(){
            return product;
        }

        private String getVersion(){
            return version;
        }
    }

    private class FoundDetails{
        private String ip;
        private ArrayList<Target> targetsFound;
        //private ArrayList<String> vulnerabilitiesFound;
        //private ArrayList<String> vulnerabilityDescriptions;
        private HashMap<String,String> vulnerabilities;

        public FoundDetails(String ip, ArrayList<Target> targetsFound, HashMap<String,String> vulns) {
            this.ip = ip;
            this.targetsFound = targetsFound;
            //this.vulnerabilitiesFound = vulnerabilitiesFound;
            //this.vulnerabilityDescriptions = vulnerabilityDescriptions;
            this.vulnerabilities = vulns;
        }

        public String getIp() {
            return ip;
        }

        public String toString(){
            String message = ip + "\n";
            message += "****************\n";
            message += "Targets:\n";
            for(Target t: targetsFound){
                message += "*******\n";
                message += "type: "+t.getType() + ", \nbanner: " + t.getBanner() + ", \nproduct: "
                        + t.getProduct() + ", \nversion: " + t.getVersion() + "\n";
                message += "*******\n";
            }
            message += "****************\n";
            message += "Vulnerabilities:\n";
            for(String ident: vulnerabilities.keySet()){
                message += ident + " : " + vulnerabilities.get(ident) + "\n";
            }
            /*for(int i = 0; i < vulnerabilitiesFound.size(); i++){
                message += vulnerabilitiesFound.get(i) + " : " + vulnerabilityDescriptions.get(i) + "\n";
            }*/
            message += "****************\n";
            return message;
        }
    }

    private ArrayList<FoundDetails> details;

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
        setContentView(R.layout.activity_vulnerabilities);

        uniqueIps = getIntent().getExtras().getStringArrayList("selectedIps");

        progressBar = (ProgressBar)findViewById(R.id.vProgress);

        ipsDoneView = (TextView) findViewById(R.id.ipsDoneTV);
        messagesView = (ListView) findViewById(R.id.messagesView);
        closeButton = (Button) findViewById(R.id.closeButton);
        saveButton = (Button) findViewById(R.id.saveButton);

        int total = uniqueIps.size();
        String messageString = "/"+total+" devices done";
        ipsDoneView.setText(0+messageString);

        messages = new ArrayList<>();
        details = new ArrayList<>();

        ArrayAdapter messagesAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, android.R.id.text1, messages);
        messagesView.setAdapter(messagesAdapter);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData();
            }
        });

        saveButton.setEnabled(false);

        new vulnerabiltiesSearchTask(uniqueIps,messages,messagesAdapter).execute();

        //new BannerGrabHelper(this,uniqueIps,grabbedBannersList,grabbedBannersAdapter,grabbedBannersFull,10000,10).execute();
    }

    public void updateIPsDone(final int i){
        int total = uniqueIps.size();
        final String messageString = "/"+total+" devices done";

        runOnUI(new Runnable() {
            @Override
            public void run() {
                ipsDoneView.setText(i+messageString);
            }
        });
    }

    private void saveData(){
        String nullMessage = "Banner could not be grabbed";
        String data = " ";
        data += "------------------\n";
        for(FoundDetails d: details){
            data += d.toString();
        }
        data += "------------------\n";


        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:tjaved.bscs15seecs@seecs.edu.pk")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_TEXT,data);
        intent.putExtra(Intent.EXTRA_SUBJECT, "NetworkScanner Banner Data");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private class vulnerabiltiesSearchTask extends AsyncTask {
        private ArrayList<String> ipList;

        private ArrayList<String> messagesList;
        private ArrayAdapter messagesAdapter;

        private VulnerabilityFinder vulnerabilityFinder;

        //ip to (p,v) list
        private HashMap<String,ArrayList<Target>> savedTargets;

        vulnerabiltiesSearchTask(ArrayList<String> ips, ArrayList<String> messages, ArrayAdapter adap){
            ipList = ips;

            messagesList = messages;
            messagesAdapter = adap;

            savedTargets = new HashMap<>();
        }

        //this messes up the architecture; needs to be handled
        private HashMap<String,String> getUPNPBanners(int timeout){
            HashMap<String,String> ipToBanner = new HashMap<>();

            String DEFAULT_IP = "239.255.255.250";
            int DEFAULT_PORT = 1900;
            String DISCOVERY_QUERY = "M-SEARCH * HTTP/1.1" + "\r\n" +
                    "HOST: 239.255.255.250:1900" + "\r\n" +
                    "MAN: \"ssdp:discover\"" + "\r\n" +
                    "MX: 1"+ "\r\n" +
                    "ST: ssdp:all" + "\r\n" + // Use this for all UPnP Devices
                    "\r\n";

            Log.d(TAG,"Waiting for UPnP");
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if(wifi != null) {
                WifiManager.MulticastLock lock = wifi.createMulticastLock("The Lock");
                lock.acquire();
                DatagramSocket socket = null;
                try {
                    InetAddress group = InetAddress.getByName(DEFAULT_IP);

                    socket = new DatagramSocket(null); // <-- create an unbound socket first
                    socket.setReuseAddress(true);
                    socket.setSoTimeout(timeout);
                    socket.bind(new InetSocketAddress(DEFAULT_PORT)); // <-- now bind it


                    DatagramPacket datagramPacketRequest = new DatagramPacket(DISCOVERY_QUERY.getBytes(), DISCOVERY_QUERY.length(), group, DEFAULT_PORT);
                    socket.send(datagramPacketRequest);

                    long time = System.currentTimeMillis();
                    long curTime = System.currentTimeMillis();

                    while (curTime - time < timeout) {
                        DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024);
                        socket.receive(datagramPacket);
                        String response = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                        Log.d(TAG,response);
                        if (response.substring(0, 12).toUpperCase().equals("HTTP/1.1 200")) {
                            String ip = datagramPacket.getAddress().getHostAddress();
                            ipToBanner.put(ip,response);
                            Log.d(TAG,"Putting >"+ip+" :"+response);
                        }

                        curTime = System.currentTimeMillis();
                        Log.d(TAG,"Difference is: " + Long.toString(curTime-time));
                    }

                } catch (final Exception e) {
                    e.printStackTrace();
                    Log.d(TAG,e.toString() + " : " + e.getMessage());
                } finally {
                    if (socket != null) {
                        socket.close();
                    }
                    Log.d(TAG,"done");
                }
                lock.release();

            }

            return ipToBanner;
        }


        @Override
        protected Object doInBackground(Object[] objects) {
            progressBar.setProgress(progressBar.getMax()/40);

            HashMap<String,String> upnpBanners = getUPNPBanners(10000 /*milliseconds*/);

            int i = 0;
            int done = 0;
            for(String ip: ipList){
                BannerGrabber bannerGrabber = new BannerGrabber();
                ArrayList<Banner> banners = bannerGrabber.grab(ip,10000,10);

                if(upnpBanners.containsKey(ip)){
                    Log.d(TAG,"Putting a UPNP banner for "+ip);
                    banners.add(new Banner(ip,"upnp",upnpBanners.get(ip)));
                }

                ArrayList<Target> targets = new ArrayList<>();
                if (banners.size() != 0){
                    for(Banner b: banners){
                        String product = Utils.getProductFromBanner(b);
                        String version = Utils.getVersionFromBanner(b);

                        if(product != null){
                            targets.add(new Target(b.getProtocol(),b.getBanner(),product,version));
                        }else{
                            targets.add(new Target(b.getProtocol(),b.getBanner(),"",version));
                        }
                    }
                }

                if (targets.size() != 0){
                    savedTargets.put(ip,targets);
                }else{
                    done += 1;
                    updateIPsDone(done);
                }

                i += 1;
                int newProgress = (int)(i * progressBar.getMax())/(2*ipList.size());
                progressBar.setProgress(newProgress);
            }
            int noOfTargets = savedTargets.size();
            Log.d(TAG,"Got " + Integer.toString(noOfTargets) + " targets; now waiting for vulnerability finder");

            MyApp app = (MyApp) getApplication();

            vulnerabilityFinder = app.getFinder();

            i = 0;
            for(String ip: savedTargets.keySet()){
                ArrayList<Target> targets = savedTargets.get(ip);
                ArrayList<String> allVulnerabilities = new ArrayList<>();
                //ArrayList<String> vulnerabilityDescs = new ArrayList<>();

                for(Target t: targets){
                    String product = t.getProduct();
                    String version = t.getVersion();

                    ArrayList<String> vulns = vulnerabilityFinder.getIdents(product,version);

                    for (String vuln: vulns){
                        if(!allVulnerabilities.contains(vuln)){
                            allVulnerabilities.add(vuln);

                            //String description = vulnerabilityFinder.getCVEDescription(vuln);
                            //Log.d(TAG,"a vulnerability found!");
                            //vulnerabilityDescs.add(description);
                        }
                    }
                }

                HashMap<String,String> vulnerabilityDescs = vulnerabilityFinder.getCVEDescriptions(allVulnerabilities);

                //For this ip,
                //update messages view to show 1)how many targets found 2)how many vulns found

                String message = ip + " : " + Integer.toString(targets.size()) + " targets found, "
                        + Integer.toString(allVulnerabilities.size()) + " vulnerabilities found";

                messagesList.add(message);
                details.add(new FoundDetails(ip,targets,vulnerabilityDescs));
                runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        messagesAdapter.notifyDataSetChanged();
                    }
                });

                i += 1;
                int newProgress = (int) ((progressBar.getMax()/2) + (i * progressBar.getMax())/(2*ipList.size()));
                progressBar.setProgress(newProgress);

                done += 1;
                updateIPsDone(done);
            }

            if(messagesList.size() == 0){
                messagesList.add("Nothing Found");
                messagesAdapter.notifyDataSetChanged();
            }

            progressBar.setProgress(progressBar.getMax());
            enableSave();

            return null;
        }
    }

    public void enableSave(){
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                saveButton.setEnabled(true);
            }
        },100);
    }

}
