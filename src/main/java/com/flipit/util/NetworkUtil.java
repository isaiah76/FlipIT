package com.flipit.util;

import java.net.InetSocketAddress;
import java.net.Socket;

public class NetworkUtil {
    public static boolean isOnline() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("8.8.8.8", 53), 1500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}