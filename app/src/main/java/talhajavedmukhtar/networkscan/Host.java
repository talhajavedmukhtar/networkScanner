package talhajavedmukhtar.networkscan;

/**
 * Created by Talha on 4/19/18.
 */

public class Host {
    private String ipAd;
    private String discoveredThrough;

    Host(){

    }

    Host(String ip, String dT){
        ipAd = ip;
        discoveredThrough = dT;
    }

    public String getIpAd() {
        return ipAd;
    }

    public void setIpAd(String ipAd) {
        this.ipAd = ipAd;
    }

    public String getDiscoveredThrough() {
        return discoveredThrough;
    }

    public void setDiscoveredThrough(String discoveredThrough) {
        this.discoveredThrough = discoveredThrough;
    }
}
