package com.allsender.app;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

final class NetUtil {
    /** IP local (Wi-Fi, hotspot ou Ethernet). Não depende de internet. */
    static String localIp() {
        String best = null; int bestScore = 0;
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                String n = ni.getName().toLowerCase();
                if (n.startsWith("rmnet") || n.startsWith("ccmni") || n.startsWith("tun")
                        || n.startsWith("dummy") || n.startsWith("v4-")) continue;
                int score = (n.startsWith("wlan") || n.startsWith("ap") || n.startsWith("swlan")
                        || n.startsWith("eth")) ? 2 : 1;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && a.isSiteLocalAddress() && score > bestScore) {
                        best = a.getHostAddress(); bestScore = score;
                    }
                }
            }
        } catch (Exception ignored) { }
        return best;
    }
}
