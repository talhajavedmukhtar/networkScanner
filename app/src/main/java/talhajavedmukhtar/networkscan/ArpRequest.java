package talhajavedmukhtar.networkscan;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.pcap4j.core.BpfProgram;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.ArpPacket;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.namednumber.ArpHardwareType;
import org.pcap4j.packet.namednumber.ArpOperation;
import org.pcap4j.packet.namednumber.EtherType;
import org.pcap4j.util.ByteArrays;
import org.pcap4j.util.MacAddress;
import org.pcap4j.util.NifSelector;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Talha on 2/17/18.
 */
public class ArpRequest {
    private String srcAddress;
    private String destAddress;
    private MacAddress resolvedAddr;
    private MacAddress srcMacAddress;
    private Context context;

    ArpRequest(String destAddress, Context c){
        this.srcAddress = getIPAddress(true);
        Log.d("ARPsrcIP",srcAddress);
        this.destAddress = destAddress;
        this.context = c;
        this.srcMacAddress = MacAddress.getByName(getMAC(c));
        Log.d("ARPsrcMAC",srcMacAddress.toString());

    }

    public void execute(int timeout) throws PcapNativeException {
        PcapNetworkInterface nif;
        try {
            nif = new NifSelector().selectNetworkInterface();
        } catch (IOException e) {
            Log.d("ARPException1",e.toString());
            e.printStackTrace();
            return;
        }

        if (nif == null) {
            return;
        }

        //System.out.println(nif.getName() + "(" + nif.getDescription() + ")");

        PcapHandle handle
                = nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, timeout);
        PcapHandle sendHandle
                = nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, timeout);
        ExecutorService pool = Executors.newSingleThreadExecutor();

        try {
            handle.setFilter(
                    "arp and src host " + srcAddress
                            + " and dst host " + destAddress
                            + " and ether dst " + Pcaps.toBpfString(srcMacAddress),
                    BpfProgram.BpfCompileMode.OPTIMIZE
            );

            PacketListener listener
                    = new PacketListener() {
                @Override
                public void gotPacket(Packet packet) {
                    if (packet.contains(ArpPacket.class)) {
                        ArpPacket arp = packet.get(ArpPacket.class);
                        if (arp.getHeader().getOperation().equals(ArpOperation.REPLY)) {
                            resolvedAddr = arp.getHeader().getSrcHardwareAddr();
                        }
                    }
                    System.out.println(packet);
                    Log.d("ARPreply",packet.toString());
                }
            };

            Task t = new Task(handle, listener);
            pool.execute(t);

            ArpPacket.Builder arpBuilder = new ArpPacket.Builder();
            try {
                arpBuilder
                        .hardwareType(ArpHardwareType.ETHERNET)
                        .protocolType(EtherType.IPV4)
                        .hardwareAddrLength((byte) MacAddress.SIZE_IN_BYTES)
                        .protocolAddrLength((byte) ByteArrays.INET4_ADDRESS_SIZE_IN_BYTES)
                        .operation(ArpOperation.REQUEST)
                        .srcHardwareAddr(srcMacAddress)
                        .srcProtocolAddr(InetAddress.getByName(srcAddress))
                        .dstHardwareAddr(MacAddress.ETHER_BROADCAST_ADDRESS)
                        .dstProtocolAddr(InetAddress.getByName(destAddress));
            } catch (UnknownHostException e) {
                Log.d("ARPException2",e.toString());
                throw new IllegalArgumentException(e);
            }

            EthernetPacket.Builder etherBuilder = new EthernetPacket.Builder();
            etherBuilder.dstAddr(MacAddress.ETHER_BROADCAST_ADDRESS)
                    .srcAddr(srcMacAddress)
                    .type(EtherType.ARP)
                    .payloadBuilder(arpBuilder)
                    .paddingAtBuild(true);

            for (int i = 0; i < 1; i++) {
                Packet p = etherBuilder.build();
                System.out.println(p);
                sendHandle.sendPacket(p);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.d("ARPException3",e.toString());
                    break;
                }
            }
        } catch (Exception e) {
            Log.d("ARPException4",e.toString());
        }finally {
            if (handle != null && handle.isOpen()) {
                handle.close();
            }
            if (sendHandle != null && sendHandle.isOpen()) {
                sendHandle.close();
            }
            if (pool != null && !pool.isShutdown()) {
                pool.shutdown();
            }

            System.out.println(srcAddress + " was resolved to " + resolvedAddr);
        }
    }


    public static String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac==null) return "";
                StringBuilder buf = new StringBuilder();
                for (int idx=0; idx<mac.length; idx++)
                    buf.append(String.format("%02X:", mac[idx]));
                if (buf.length()>0) buf.deleteCharAt(buf.length()-1);
                return buf.toString();
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
        /*try {
            // this is so Linux hack
            return loadFileAsString("/sys/class/net/" +interfaceName + "/address").toUpperCase().trim();
        } catch (IOException ex) {
            return null;
        }*/
    }

    public String getMAC(Context c){
        WifiManager wifiManager = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        return wInfo.getMacAddress();
    }

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    private static class Task implements Runnable {

        private PcapHandle handle;
        private PacketListener listener;

        public Task(PcapHandle handle, PacketListener listener) {
            this.handle = handle;
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                handle.loop(1, listener);
            } catch (PcapNativeException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (NotOpenException e) {
                e.printStackTrace();
            }
        }

    }
}
