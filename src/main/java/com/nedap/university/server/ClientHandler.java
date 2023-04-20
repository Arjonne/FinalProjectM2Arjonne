package com.nedap.university.server;

import com.nedap.university.PacketProtocol;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Represents the handler of the connected client for the server.
 */
public class ClientHandler {
    private final Server server;
    private final DatagramSocket serverSocket;
    private String lastFileDownload;
    private int lastFlag;
    private String lastRemovedFile;

    /**
     * Create a clientHandler to be able to handle the input from the client that is connected to the server (on the PI).
     *
     * @param serverSocket is the socket via which the client and server are connected.
     * @param server       is the server (on the PI).
     */
    public ClientHandler(DatagramSocket serverSocket, Server server) {
        this.serverSocket = serverSocket;
        this.server = server;
    }

    /**
     * Start the clientHandler: as long as the server and client are connected via the socket, the input from the client
     * can be processed.
     */
    public void start() {
        boolean connected = true;
        while (connected) {
            // try to receive a request from the client:
            DatagramPacket receivedPacket = server.receiveRequest();
            if (receivedPacket == null) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    System.out.println("Not able to sleep due to interruption.");
                }
            } else {
                // if a request is received, get the byte representation of the request packet (including header) and
                // get some information from this packet:
                byte[] dataOfReceivedPacket = receivedPacket.getData();
                InetAddress inetAddress = receivedPacket.getAddress();
                int port = receivedPacket.getPort();
                int flag = PacketProtocol.getFlag(dataOfReceivedPacket);
                // check if the received packet is a new request. If not, it is probably some acknowledgement from the
                // last request that is retransmitted as the acknowledgement could have been lost. In that case, try to
                // receive a new request:
                if (flag != PacketProtocol.UPLOAD && flag != PacketProtocol.DOWNLOAD && flag != PacketProtocol.REMOVE && flag != PacketProtocol.REPLACE && flag != PacketProtocol.LIST && flag != PacketProtocol.CLOSE) {
                    continue;
                }
                int totalFileSize = PacketProtocol.getFileSizeInPacket(dataOfReceivedPacket);
                int lastReceivedSeqNr = PacketProtocol.getSequenceNumber(dataOfReceivedPacket);
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
                // execute the correct tasks that correspond to the request of the client:
                switch (flag) {
                    case PacketProtocol.UPLOAD:
                        System.out.println("Client sent request for uploading " + fileName + ".");
                        server.receiveFile(fileName, totalFileSize, lastReceivedSeqNr, inetAddress, port, serverSocket);
                        setLastFlag(PacketProtocol.UPLOAD);
                        break;
                    case PacketProtocol.DOWNLOAD:
                        System.out.println("Client sent request for downloading " + fileName + ".");
                        server.sendFile(fileName, lastReceivedSeqNr, inetAddress, port, serverSocket);
                        setLastFlag(PacketProtocol.DOWNLOAD);
                        break;
                    case PacketProtocol.REMOVE:
                        if (lastFlag == PacketProtocol.REMOVE && lastRemovedFile.equals(fileName)) {
                            continue;
                        }
                        System.out.println("Client sent request for removing " + fileName + ".");
                        server.removeFile(fileName, lastReceivedSeqNr, inetAddress, port, serverSocket);
                        setLastRemovedFile(fileName);
                        setLastFlag(PacketProtocol.REMOVE);
                        break;
                    case PacketProtocol.REPLACE:
                        System.out.println("Client sent request for replacing " + oldFileName + " by " + newFileName + ".");
                        server.replaceFile(oldFileName, newFileName, totalFileSize, lastReceivedSeqNr, inetAddress, port, serverSocket);
                        setLastFlag(PacketProtocol.REPLACE);
                        break;
                    case PacketProtocol.LIST:
                        System.out.println("Client sent request for listing all available files.");
                        server.listFiles(lastReceivedSeqNr, inetAddress, port, serverSocket);
                        setLastFlag(PacketProtocol.LIST);
                        break;
                    case PacketProtocol.CLOSE:
                        if (lastFlag == PacketProtocol.CLOSE) {
                            continue;
                        }
                        System.out.println("Client closed the application. If you want to close the server on the Raspberry Pi too, use the following commands: \n\n" +
                                "sudo systemctl stop num2.service \n" +
                                "sudo shutdown -h now");
                        server.respondToClosingClient(lastReceivedSeqNr, inetAddress, port, serverSocket);
                        setLastFlag(PacketProtocol.CLOSE);
                        break;
                }
            }
        }
    }

    /**
     * Set the last flag used.
     * @param flag is the received flag.
     */
    public void setLastFlag(int flag) {
        this.lastFlag = flag;
    }

    /**
     * Set the last removed file.
     * @param fileName is the name of the file that is removed.
     */
    public void setLastRemovedFile(String fileName) {
        this.lastRemovedFile = fileName;
    }
}
