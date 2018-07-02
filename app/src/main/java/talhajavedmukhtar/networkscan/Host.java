package talhajavedmukhtar.networkscan;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by Talha on 4/19/18.
 */

public class Host{
    private String ipAd;
    private String discoveredThrough;
    private String macAddress;
    private String vendor;
    private ArrayList<Integer> openPorts;

    Host(){

    }

    Host(String ip, String dT){
        ipAd = ip;
        discoveredThrough = dT;
        macAddress = null;
        openPorts = new ArrayList<>();
    }



    Host(String ip, String mA, String v){
        ipAd = ip;
        macAddress = mA;
        vendor = v;
        openPorts = new ArrayList<>();
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

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public void addOpenPort(int port){
        this.openPorts.add(port);
    }

    public ArrayList<Integer> getOpenPorts(){
        return openPorts;
    }
}
