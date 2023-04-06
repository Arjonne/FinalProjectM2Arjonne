package com.nedap.university.server;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Scanner;

/**
 * Represents the server on the Raspberry Pi.
 */

public class Server {
    private DatagramSocket socket;

    public Server(int port) throws SocketException {
        socket = new DatagramSocket(port);
    }
}
