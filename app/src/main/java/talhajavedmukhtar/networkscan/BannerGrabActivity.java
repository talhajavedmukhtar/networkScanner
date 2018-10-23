package talhajavedmukhtar.networkscan;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import talhajavedmukhtar.networkscan.BannerGrabbers.BannerGrabHelper;

import static talhajavedmukhtar.networkscan.MainActivity.UIHandler;

public class BannerGrabActivity extends AppCompatActivity {
    private TextView ipsDoneView;
    private ListView grabbedBanners;
    private Button closeButton;
    private Button saveButton;

    private ArrayList<String> uniqueIps;
    private ArrayList<String> grabbedBannersList;
    private ArrayList<String> grabbedBannersFull;

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
        setContentView(R.layout.activity_banner_grab);

        uniqueIps = getIntent().getExtras().getStringArrayList("selectedIps");

        ipsDoneView = (TextView) findViewById(R.id.ipsDoneTV);
        grabbedBanners = (ListView) findViewById(R.id.grabbedBanners);
        closeButton = (Button) findViewById(R.id.closeButton);
        saveButton = (Button) findViewById(R.id.saveButton);

        int total = uniqueIps.size();
        String messageString = "/"+total+" devices done";
        ipsDoneView.setText(0+messageString);

        grabbedBannersList = new ArrayList<>();
        grabbedBannersFull = new ArrayList<>();

        ArrayAdapter grabbedBannersAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, android.R.id.text1, grabbedBannersList);
        grabbedBanners.setAdapter(grabbedBannersAdapter);

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

        new BannerGrabHelper(this,uniqueIps,grabbedBannersList,grabbedBannersAdapter,grabbedBannersFull,10000,10).execute();
    }

    public void updateIPsDone(int i){
        int total = uniqueIps.size();
        String messageString = "/"+total+" devices done";

        ipsDoneView.setText(i+messageString);
    }

    private void saveData(){
        String nullMessage = "Banner could not be grabbed";
        String data = " ";
        data += "------------------\n";
        for(String banner: grabbedBannersFull){
            if(!banner.split(" : ")[2].equals(nullMessage)){
                data += "****\n";

                data += banner + "\n";

                data += "****\n";
            }
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

    public void enableSave(){
        saveButton.setEnabled(true);
    }
}
