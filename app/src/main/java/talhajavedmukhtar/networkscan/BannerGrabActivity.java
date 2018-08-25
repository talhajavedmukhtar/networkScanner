package talhajavedmukhtar.networkscan;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import talhajavedmukhtar.networkscan.BannerGrabbers.HTTPBannerGrabber;

public class BannerGrabActivity extends AppCompatActivity {
    private TextView ipsDoneView;
    private ListView grabbedBanners;
    private Button closeButton;

    private ArrayList<String> uniqueIps;
    private ArrayList<String> grabbedBannersList;

    HTTPBannerGrabber httpBannerGrabber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_banner_grab);

        uniqueIps = getIntent().getExtras().getStringArrayList("addressList");

        ipsDoneView = (TextView) findViewById(R.id.ipsDoneTV);
        grabbedBanners = (ListView) findViewById(R.id.grabbedBanners);
        closeButton = (Button) findViewById(R.id.closeButton);

        grabbedBannersList = new ArrayList<>();

        ArrayAdapter grabbedBannersAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, android.R.id.text1, grabbedBannersList);
        grabbedBanners.setAdapter(grabbedBannersAdapter);

        httpBannerGrabber = new HTTPBannerGrabber();

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        execute();
    }

    private void execute(){
        int total = uniqueIps.size();
        String messageString = "/"+total+" devices done";
        ipsDoneView.setText(0+messageString);
        int i = 0;
        for(String ip: uniqueIps){
            String banner = httpBannerGrabber.grabBanner(ip,10,10);
            grabbedBannersList.add(ip + " : " + banner);
            i += 1;
            ipsDoneView.setText(i+messageString);
        }
    }
}
