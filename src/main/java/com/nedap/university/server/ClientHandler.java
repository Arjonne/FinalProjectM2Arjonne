package com.nedap.university.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Represents the handler of the connected client for the server.
 */
public class ClientHandler {
    private Server server;
    private DatagramSocket serverSocket;
    private final String UPLOAD = "UPLOAD";
    private final String DOWNLOAD = "DOWNLOAD";
    private final String REMOVE = "REMOVE";
    private final String REPLACE = "REPLACE";
    private final String LIST = "LIST";
    private final String CLOSE = "CLOSE";


    /**
     * Create a clientHandler to be able to handle the input from the client that is connected to the server on the PI.
     *
     * @param serverSocket is the socket that is needed to be able to listen if data is coming in.
     * @param server is the server (PI).
     */
    public ClientHandler(DatagramSocket serverSocket, Server server) {
        this.serverSocket = serverSocket;
        this.server = server;
    }

    public void start() {
        boolean connected = true;
        while(connected) {
            try {
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(packet);
                InetAddress inetAddress = packet.getAddress();
                int port = packet.getPort();
                String messageFromClient = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Message from client: " + messageFromClient);
                // respond with same message:
                packet = new DatagramPacket(buffer, buffer.length, inetAddress, port);
                serverSocket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

//        boolean connected = true;
//        while (connected) {
//            String input = ;//todo 'read' flag from packet van maken!
//            String[] split = input.split("\\s+");
//            String command = split[0];
//            String fileName = null;
//            String oldFileName = null;
//            String newFileName = null;
//            if (split.length == 2) {
//                fileName = split[1];
//            } else if (split.length > 2) {
//                oldFileName = split[1];
//                newFileName = split[2];
//            }
//            switch (command) {
//                case UPLOAD:
//                    server.receiveFile(fileName);
//                    break;
//                case DOWNLOAD:
//                    server.sendFile(fileName);
//                    break;
//                case REMOVE:
//                    server.removeFile(fileName);
//                    break;
//                case REPLACE:
//                    server.replaceFile(oldFileName, newFileName);
//                    break;
//                case LIST:
//                    server.listFiles();
//                    break;
//                case CLOSE:
//                    server.stop();
//                    System.out.println("Application is closing.");
//                default:
//                    server.respondToUnknownRequest();
//                    break;
//            }
//        }
    }
}
