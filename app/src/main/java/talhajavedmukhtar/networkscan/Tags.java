package talhajavedmukhtar.networkscan;

import android.app.Activity;
import android.content.Context;

/**
 * Created by Talha on 4/7/18.
 */

public class Tags {
    public final static String application = "NetworkScan";

    public static String makeTag(String tag){
        return application + "." + tag;
    }
}
