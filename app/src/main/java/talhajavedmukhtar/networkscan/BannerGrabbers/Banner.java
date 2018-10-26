package talhajavedmukhtar.networkscan.BannerGrabbers;

/**
 * Created by Talha on 10/25/18.
 */

public class Banner {
    String ip;
    String protocol;
    String banner;

    public Banner(String i, String p, String b){
        ip = i;
        protocol = p;
        banner = b;
    }

    public String getIp(){
        return ip;
    }

    public String getProtocol(){
        return protocol;
    }

    public String getBanner(){
        return banner;
    }
}
