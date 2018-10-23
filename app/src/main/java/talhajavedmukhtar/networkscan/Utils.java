package talhajavedmukhtar.networkscan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import talhajavedmukhtar.networkscan.BannerGrabbers.BannerGrabber;

/**
 * Created by Talha on 10/23/18.
 */

public class Utils {

    public static String getProductFromBanner(BannerGrabber.Banner b){
        String protocol = b.getProtocol();
        String banner = b.getBanner();

        if(protocol.equals("ssh")){
            return extractSSHProduct(banner);
        }

        return null;
    }

    public static String getVersionFromBanner(BannerGrabber.Banner b){
        String protocol = b.getProtocol();
        String banner = b.getBanner();

        if(protocol.equals("ssh")){
            return extractSSHVersion(banner);
        }

        return null;
    }

    //SSH
    private static String extractSSHProduct(String banner){
        String product = banner.split("\n")[0];

        //remove SSH-x.x
        product = product.replace("(SSH-\\d+(\\.\\d+)?-)","");

        //remove version
        product = product.replace("(v\\s?)?\\d+(\\.\\d+)*","");

        //remove ending underscores or hyphens
        product = product.replace("(_|-)+$","");

        //replace in between underscores or hypens with space
        product = product.replace("(_|-)"," ");

        return product;
    }

    private static String extractSSHVersion(String banner){
        String product = banner.split("\n")[0];

        //remove SSH-x.x
        product = product.replace("(SSH-\\d+(\\.\\d+)?-)","");

        Pattern p = Pattern.compile("(v\\s?)?\\d+(\\.\\d+)*");

        Matcher m = p.matcher(product);

        if (m.find()){
            return m.group(0);
        }else{
            return "*";
        }
    }

    //HTTP
}
