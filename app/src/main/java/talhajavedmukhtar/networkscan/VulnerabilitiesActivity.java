package talhajavedmukhtar.networkscan;

import android.content.Intent;
import android.net.Uri;
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

import java.util.ArrayList;
import java.util.HashMap;

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
        private String product;
        private String version;

        Target(String t, String p, String v){
            type = t;
            product = p;
            version = v;
        }

        private String getType(){
            return type;
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
        private ArrayList<String> vulnerabilitiesFound;

        public FoundDetails(String ip, ArrayList<Target> targetsFound, ArrayList<String> vulnerabilitiesFound) {
            this.ip = ip;
            this.targetsFound = targetsFound;
            this.vulnerabilitiesFound = vulnerabilitiesFound;
        }

        public String getIp() {
            return ip;
        }

        public ArrayList<Target> getTargetsFound() {
            return targetsFound;
        }

        public ArrayList<String> getVulnerabilitiesFound() {
            return vulnerabilitiesFound;
        }

        public String toString(){
            String message = ip + "\n";
            message += "****************\n";
            for(Target t: targetsFound){
                message += "type: "+t.getType() + ", product: " + t.getProduct() + ", version: " + t.getVersion() + "\n";
            }
            message += "****************\n";
            for(String vuln: vulnerabilitiesFound){
                message += vuln + "\n";
            }
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


        @Override
        protected Object doInBackground(Object[] objects) {
            progressBar.setProgress(progressBar.getMax()/40);

            int i = 0;
            int done = 0;
            for(String ip: ipList){
                BannerGrabber bannerGrabber = new BannerGrabber();
                ArrayList<BannerGrabber.Banner> banners = bannerGrabber.grab(ip,10000,10);

                ArrayList<Target> targets = new ArrayList<>();
                if (banners.size() != 0){
                    for(BannerGrabber.Banner b: banners){
                        String product = Utils.getProductFromBanner(b);
                        String version = Utils.getVersionFromBanner(b);

                        if(product != null){
                            targets.add(new Target(b.getProtocol(),product,version));
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

            while(!app.isFinderReady()){
                //we play the waiting game
            }

            vulnerabilityFinder = app.getFinder();

            i = 0;
            for(String ip: savedTargets.keySet()){
                ArrayList<Target> targets = savedTargets.get(ip);
                ArrayList<String> allVulnerabilities = new ArrayList<>();

                for(Target t: targets){
                    String product = t.getProduct();
                    String version = t.getVersion();

                    ArrayList<String> vulns = vulnerabilityFinder.getVulnerabilities(product,version);

                    for (String vuln: vulns){
                        if(!allVulnerabilities.contains(vuln)){
                            allVulnerabilities.add(vuln);
                        }
                    }
                }

                //For this ip,
                //update messages view to show 1)how many targets found 2)how many vulns found

                String message = ip + " : " + Integer.toString(targets.size()) + " targets found, "
                        + Integer.toString(allVulnerabilities.size()) + " vulnerabilities found";

                messagesList.add(message);
                details.add(new FoundDetails(ip,targets,allVulnerabilities));
                runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        messagesAdapter.notifyDataSetChanged();
                    }
                });

                i += 1;
                int newProgress = (int) ((progressBar.getMax()/2) + (i * progressBar.getMax())/(2*ipList.size()));
                progressBar.setProgress(newProgress);
            }

            done += 1;
            updateIPsDone(done);

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
