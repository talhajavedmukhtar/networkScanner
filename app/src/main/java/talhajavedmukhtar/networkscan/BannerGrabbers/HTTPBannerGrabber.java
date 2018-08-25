package talhajavedmukhtar.networkscan.BannerGrabbers;

import android.nfc.Tag;

import talhajavedmukhtar.networkscan.Tags;

/**
 * Created by Talha on 8/4/18.
 */

public class HTTPBannerGrabber extends BannerGrabber {
    public HTTPBannerGrabber(){
        portNo = 80;
        TAG = Tags.makeTag("HTTPBannerGrabber");
        bannerGrabString = "'GET / HTTP/1.1";
    }
}
