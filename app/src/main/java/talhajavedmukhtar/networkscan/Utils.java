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

        if(protocol.equals("http")){
            return extractHTTPProduct(banner);
        }

        return null;
    }

    public static String getVersionFromBanner(BannerGrabber.Banner b){
        String protocol = b.getProtocol();
        String banner = b.getBanner();

        if(protocol.equals("ssh")){
            return extractSSHVersion(banner);
        }

        if(protocol.equals("http")){
            return extractHTTPVersion(banner);
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
    private static String extractHTTPProduct(String banner){
        Pattern p = Pattern.compile("Server:\\s.*");

        Matcher m = p.matcher(banner);

        if (m.find()){
            String product = m.group(0);
            product = product.split("\\s\\(")[0];

            //remove version identifiers
            product = product.replace("/?\\d+(\\.\\d+)?","");

            //remove ending space
            product = product.replace("\\s+$","");

            return product;
        }else{
            return null;
        }
    }

    private static String extractHTTPVersion(String banner){
        Pattern p = Pattern.compile("Server:\\s.*");

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
}
