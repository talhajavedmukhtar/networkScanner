package talhajavedmukhtar.networkscan;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class SummaryActivity extends AppCompatActivity {
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

    }
}
