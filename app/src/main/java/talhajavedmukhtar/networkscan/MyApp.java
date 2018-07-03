package talhajavedmukhtar.networkscan;

import android.app.Application;
import android.os.AsyncTask;

/**
 * Created by Talha on 7/3/18.
 */

public class MyApp extends Application {
    public static MacToVendorMap map;

    AsyncTask initTask = new AsyncTask() {
        @Override
        protected Object doInBackground(Object[] objects) {
            map = new MacToVendorMap(getApplicationContext());
            return null;
        }
    };

    public Boolean isMapReady(){
        if(initTask.getStatus() != AsyncTask.Status.FINISHED){
            return false;
        }else{
            return true;
        }
    }

    public MacToVendorMap getMap(){
        return map;
    }

}
