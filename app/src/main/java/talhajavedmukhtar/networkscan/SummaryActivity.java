package talhajavedmukhtar.networkscan;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class SummaryActivity extends AppCompatActivity {
    final String TAG = Tags.makeTag("Summary");

    private String message;
    private ArrayList<String> uniqueIps;

    private TextView messageTV;
    private ListView uniqueIpsLV;
    private Button closeButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        uniqueIps = (ArrayList<String>) getIntent().getExtras().getStringArrayList("addressList");
        int totalDevices = uniqueIps.size();

        message = totalDevices + " device(s) discovered: ";

        messageTV = (TextView) findViewById(R.id.totalDevices);
        messageTV.setText(message);

        uniqueIpsLV = (ListView) findViewById(R.id.deviceAddresses);
        ArrayAdapter adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, android.R.id.text1, uniqueIps);
        uniqueIpsLV.setAdapter(adapter);

        closeButton = (Button) findViewById(R.id.closeButton);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        BufferedReader bufferedReader = null;

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
                        }
                    }
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

    }
}
