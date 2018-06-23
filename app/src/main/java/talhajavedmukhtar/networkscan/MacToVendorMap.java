package talhajavedmukhtar.networkscan;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Created by Talha on 6/17/18.
 */

public class MacToVendorMap {
    final String TAG = Tags.makeTag("MacToVendorMap");

    private HashMap<String,String> small;
    private HashMap<String,String> medium;
    private HashMap<String,String> large;

    MacToVendorMap(final Context c){
        initializeMap(c);
    }

    private void initializeMap(Context c){
        //store entries from the small chunks file
        small = new HashMap<>();

        BufferedReader bufferedReader = null;
        try{
            bufferedReader = new BufferedReader(new InputStreamReader(c.getAssets().open("macToVendor/small.csv")));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] fields = line.split(",");
                if(fields[0].equals("Registry")) continue;

                String MAC = fields[1];

                int i = 2;
                String manufacturer = fields[i];
                if(fields[i].startsWith("\"")){
                    while(!fields[i].endsWith("\"")){
                        i += 1;
                        manufacturer += "," + fields[i];
                    }
                }

                small.put(MAC,manufacturer);
            }
        }catch (Exception ex){
            Log.d(TAG,ex.getMessage());
        }finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Exception ex) {
                    Log.d(TAG,ex.getMessage());
                }
            }
        }

        //store entries from the medium chunks file
        medium = new HashMap<>();
        try{
            bufferedReader = new BufferedReader(new InputStreamReader(c.getAssets().open("macToVendor/medium.csv")));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] fields = line.split(",");
                if(fields[0].equals("Registry")) continue;
                String MAC = fields[1];

                int i = 2;
                String manufacturer = fields[i];
                if(fields[i].startsWith("\"")){
                    while(!fields[i].endsWith("\"")){
                        i += 1;
                        manufacturer += "," + fields[i];
                    }
                }
                medium.put(MAC,manufacturer);
            }
        }catch (Exception ex){
            Log.d(TAG,ex.getMessage());
        }finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Exception ex) {
                    Log.d(TAG,ex.getMessage());
                }
            }
        }

        //store entries from the large chunks file
        large = new HashMap<>();

        try{
            bufferedReader = new BufferedReader(new InputStreamReader(c.getAssets().open("macToVendor/large.csv")));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] fields = line.split(",");
                if(fields[0].equals("Registry")) continue;
                String MAC = fields[1];

                int i = 2;
                String manufacturer = fields[i];
                if(fields[i].startsWith("\"")){
                    while(!fields[i].endsWith("\"")){
                        i += 1;
                        manufacturer += "," + fields[i];
                    }
                }
                large.put(MAC,manufacturer);
            }
        }catch (Exception ex){
            Log.d(TAG,ex.getMessage());
        }finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Exception ex) {
                    Log.d(TAG,ex.getMessage());
                }
            }
        }
    }

    public String findVendor(String MACAddress){
        String[] pieces = MACAddress.toUpperCase().split(":");
        String MACportion = pieces[0]+pieces[1]+pieces[2];
        String vendor = large.get(MACportion);

        if(vendor == null){
            vendor = medium.get(MACportion + pieces[3].charAt(0));
        }
        
        if(vendor == null){
            vendor = small.get(MACportion + pieces[3] + pieces[4].charAt(0));
        }

        return vendor;
    }
}
