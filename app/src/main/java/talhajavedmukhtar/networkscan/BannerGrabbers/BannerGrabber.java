package talhajavedmukhtar.networkscan.BannerGrabbers;

import java.util.ArrayList;

/**
 * Created by Talha on 10/23/18.
 */

public class BannerGrabber {
    private SSHBannerGrabber sshBannerGrabber;
    private HTTPBannerGrabber httpBannerGrabber;


    public BannerGrabber(){
        sshBannerGrabber = new SSHBannerGrabber();
        httpBannerGrabber = new HTTPBannerGrabber();
    }

    public ArrayList<Banner> grab(String ip, int connectionTimeout, int grabTimeout){
        String nullMsg = "Banner could not be grabbed";

        ArrayList<Banner> banners = new ArrayList<>();

        String sshBanner = sshBannerGrabber.execute(ip,connectionTimeout,grabTimeout);
        String httpBanner = httpBannerGrabber.execute(ip,connectionTimeout,grabTimeout);

        if (!sshBanner.equals(nullMsg)){
            banners.add(new Banner(ip,"ssh",sshBanner));
        }

        if(!httpBanner.equals(nullMsg)){
            banners.add(new Banner(ip,"http",httpBanner));
        }

        return banners;
    }
}
