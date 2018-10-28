package talhajavedmukhtar.networkscan;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import talhajavedmukhtar.networkscan.BannerGrabbers.Banner;
import talhajavedmukhtar.networkscan.BannerGrabbers.BannerGrabber;

/**
 * Created by Talha on 10/23/18.
 */

public class Utils {
    private static final String TAG = Tags.makeTag("Utils");

    public static String getProductFromBanner(Banner b){
        String protocol = b.getProtocol();
        String banner = b.getBanner();

        if(protocol.equals("ssh")){
            return extractSSHProduct(banner);
        }

        if(protocol.equals("http")){
            return extractHTTPProduct(banner);
        }

        if(protocol.equals("upnp")){
            return extractUPNPProduct(banner);
        }

        return null;
    }

    public static String getVersionFromBanner(Banner b){
        String protocol = b.getProtocol();
        String banner = b.getBanner();

        if(protocol.equals("ssh")){
            return extractSSHVersion(banner);
        }

        if(protocol.equals("http")){
            return extractHTTPVersion(banner);
        }

        if(protocol.equals("upnp")){
            return extractUPNPVersion(banner);
        }

        return null;
    }

    //SSH
    private static String extractSSHProduct(String banner){
        String product = banner.split("\n")[0];

        Log.d(TAG,"Doing regex on: "+product);

        //remove SSH-x.x
        product = product.replaceAll("(SSH-\\d+(\\.\\d+)?-)","");

        Log.d(TAG,"Doing regex on: "+product);

        //remove version
        product = product.replaceAll("(v\\s?)?\\d+(\\.\\d+)*","");

        //remove ending underscores or hyphens
        product = product.replaceAll("(_|-)+$","");

        //replace in between underscores or hypens with space
        product = product.replaceAll("(_|-)"," ");

        product = product.toLowerCase();

        if(product.trim().equals("dropbear")){
            Log.d(TAG,"This equaled dropbear: "+ product);
            return "dropbear_ssh";
        }else{
            Log.d(TAG,"This did not equal dropbear: "+ product);
        }

        return product;
    }

    private static String extractSSHVersion(String banner){
        String product = banner.split("\n")[0];

        //remove SSH-x.x
        product = product.replaceAll("(SSH-\\d+(\\.\\d+)?-)","");

        Pattern p = Pattern.compile("(v\\s?)?\\d+(\\.\\d+)*");

        Matcher m = p.matcher(product);

        if (m.find()){
            String version = m.group(0);

            return version;
        }else{
            return "*";
        }
    }

    //HTTP
    private static String extractHTTPProduct(String banner){
        Pattern p = Pattern.compile("(Server:)|(SERVER:)\\s.*");

        Matcher m = p.matcher(banner);

        if (m.find()){
            String product = m.group(0);
            product = product.split("\\s\\(")[0];

            //remove version identifiers
            product = product.replaceAll("/?\\d+(\\.\\d+)?","");

            //remove ending space
            product = product.replaceAll("\\s+$","");

            return product;
        }else{
            return null;
        }
    }

    private static String extractHTTPVersion(String banner){
        Pattern p = Pattern.compile("(Server:)|(SERVER:)\\s.*");

        Matcher m = p.matcher(banner);

        if (m.find()){
            String product = m.group(0);

            //Remove the initial Server:
            product = product.substring(8);

            product = product.split("\\s\\(")[0];

            Pattern p2 = Pattern.compile("/?\\d+(\\.\\d+)?");
            Matcher m2 = p2.matcher(product);

            if (m2.find()){
                String version = m2.group(0);

                if(version.charAt(0) == '/'){
                    return version.substring(1);
                }else{
                    return version;
                }
            }else{
                return "*";
            }
        }else{
            return null;
        }
    }

    private static String extractUPNPProduct(String banner){
        Pattern p = Pattern.compile("(Server:)|(SERVER:)\\s.*");

        Matcher m = p.matcher(banner);

        if (m.find()){
            String product = m.group(0);
            product = product.substring(8);
            product = product.split("(, )|(\\s)")[0];

            //remove version identifiers
            product = product.split("/")[0];

            if (product.toLowerCase().equals("linux")){
                return "linux_kernel";
            }else{
                return product;
            }
        }else{
            return null;
        }
    }

    private static String extractUPNPVersion(String banner){
        Pattern p = Pattern.compile("(Server:)|(SERVER:)\\s.*");

        Matcher m = p.matcher(banner);

        if (m.find()){
            String product = m.group(0);
            product = product.split("(, )|(\\s)")[0];

            String version =  product.split("/")[1];

            return version.split("_")[0];
        }else{
            return null;
        }
    }

}
