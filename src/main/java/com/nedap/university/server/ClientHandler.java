package com.nedap.university.server;

import com.nedap.university.PacketProtocol;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Represents the handler of the connected client for the server.
 */
public class ClientHandler {
    private Server server;
    private DatagramSocket serverSocket;

    /**
     * Create a clientHandler to be able to handle the input from the client that is connected to the server on the PI.
     *
     * @param serverSocket is the socket that is needed to be able to listen if data is coming in.
     * @param server       is the server (PI).
     */
    public ClientHandler(DatagramSocket serverSocket, Server server) {
        this.serverSocket = serverSocket;
        this.server = server;
    }

    public void start() {
        boolean connected = true;
        while (connected) {
            DatagramPacket receivedPacket = server.receiveRequest();
            if (receivedPacket != null) {
                // get the address and port number of the received packet:
                InetAddress inetAddress = receivedPacket.getAddress();
                int port = receivedPacket.getPort();
                // put the data (including header) of the received packet into a byte array:
                byte[] receivedPacketData = receivedPacket.getData();
                // read the flag that is set in the packet that is received as a specific response needs to be sent:
                int flag = PacketProtocol.getFlag(receivedPacketData);
                // read the file size of the file to be transmitted (if applicable):
                int totalFileSize = PacketProtocol.getFileSizeInPacket(receivedPacketData);
                // get the sequence number of the received packet
                int receivedSeqNr = PacketProtocol.getSequenceNumber(receivedPacketData);
                // read the data (filename) that is sent in the packet (if applicable):
                String fileNameInData = new String(receivedPacket.getData(), PacketProtocol.HEADER_SIZE, (receivedPacket.getLength() - PacketProtocol.HEADER_SIZE));
                String[] split = fileNameInData.split("\\s+");
                String fileName = null;
                String oldFileName = null;
                String newFileName = null;
                if (split.length == 1) {
                    fileName = split[0];
                } else {
                    oldFileName = split[0];
                    newFileName = split[1];
                }
                switch (flag) {
                    case PacketProtocol.UPLOAD:
                        System.out.println("Client sent request for uploading " + fileName + ".");
                        server.receiveFile(fileName, totalFileSize, receivedSeqNr, inetAddress, port, serverSocket);
                        break;
                    case PacketProtocol.DOWNLOAD:
                        System.out.println("Client sent request for downloading " + fileName + ".");
                        server.sendFile(fileName, receivedSeqNr, inetAddress, port, serverSocket);
                        break;
                    case PacketProtocol.REMOVE:
                        System.out.println("Client sent request for removing " + fileName + ".");
                        server.removeFile(fileName, receivedSeqNr, inetAddress, port, serverSocket);
                        break;
                    case PacketProtocol.REPLACE:
                        System.out.println("Client sent request for replacing " + oldFileName + " by " + newFileName + ".");
                        server.replaceFile(oldFileName, newFileName, totalFileSize, receivedSeqNr, inetAddress, port, serverSocket);
                        break;
                    case PacketProtocol.LIST:
                        System.out.println("Client sent request for listing all available files.");
                        server.listFiles(receivedSeqNr, inetAddress, port, serverSocket);
                        break;
                    case PacketProtocol.CLOSE:
                        System.out.println("Client closed the application. If you want to close the server on the " +
                                "Raspberry Pi too, use the following command: sudo shutdown -h now");
                        server.respondToClosingClient(receivedSeqNr, inetAddress, port, serverSocket);
                        break;
                }
            }
        }
    }
}
