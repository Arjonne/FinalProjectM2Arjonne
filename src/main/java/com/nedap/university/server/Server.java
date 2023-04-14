package com.nedap.university.server;

import com.nedap.university.FileProtocol;
import com.nedap.university.PacketProtocol;
import com.nedap.university.StopAndWaitProtocol;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.List;

/**
 * Represents the server on the Raspberry Pi.
 */

public class Server {
    private int port;
    private boolean isOpen;
    private DatagramSocket serverSocket;
    private String fileName;
    private String oldFileName;
    private String newFileName;
    private int lastSentSeqNr;
    private int lastReceivedSeqNr;

    /**
     * Create the server with the port and address of the PI. Besides, a list is created
     */
    public Server() {
        // the port on which the server is listening on:
        port = PacketProtocol.PI_PORT;
        // after creating the server, it is not opened yet:
        isOpen = false;
    }

    /**
     * Start the server. Can only be done when server is not open for connections yet. A new DatagramSocket should start
     * with port of PI as input. This socket can be connected to the client to only accept packets that are sent by that
     * client. The boolean isOpen should be set on true as the server is now open for connections.
     */
    public void start() {
        if (isOpenForConnection()) {
            System.out.println("Server is already in use.");
            return;
        } else {
            try {
                serverSocket = new DatagramSocket(port);
                ClientHandler clientHandler = new ClientHandler(serverSocket, this);
                clientHandler.start();
            } catch (SocketException e) {
                System.out.println("Not able to start the server with this port.");
            }
        }
        isOpen = true;
    }

    /**
     * Check whether the server is currently open for accepting connections.
     *
     * @return true if the server is open, false if not.
     */
    public boolean isOpenForConnection() {
        return isOpen;
    }

    /**
     * Set fileName to the name of the file that is requested by the client.
     *
     * @param fileName is the name of the file.
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Set oldFileName to the name of the file that is requested to be replaced by the client.
     *
     * @param oldFileName is the name of the old (= to be replaced) file.
     */
    public void setOldFileName(String oldFileName) {
        this.oldFileName = oldFileName;
    }

    /**
     * Set newFileName to the name of the file that is requested by the client.
     *
     * @param newFileName is the name of the new file.
     */
    public void setNewFileName(String newFileName) {
        this.newFileName = newFileName;
    }

    public void setLastSentSeqNr(int lastSentSeqNr) {
        this.lastSentSeqNr = lastSentSeqNr;
    }

    public int getLastSentSeqNr() {
        return lastSentSeqNr;
    }

    public void setLastReceivedSeqNr(int lastReceivedSeqNr) {
        this.lastReceivedSeqNr = lastReceivedSeqNr;
    }

    public int getLastReceivedSeqNr() {
        return lastReceivedSeqNr;
    }

    /**
     * Receive the request packet from the client in order to be able to execute the request.
     *
     * @return the Datagram Packet that is received.
     */
    public DatagramPacket receiveRequest() {
        byte[] requestPacket = new byte[256];
        DatagramPacket packetToReceive = new DatagramPacket(requestPacket, requestPacket.length);
        try {
            serverSocket.receive(packetToReceive);
        } catch (IOException e) {
            e.printStackTrace(); //todo
            return null;
        }
        return packetToReceive;
    }

    /**
     * Respond to the request from the client. This can either be an acknowledgement, or an acknowledgement with an
     * additional flag that can be used to inform the client that either the requested file does already exist, or the
     * requested file does not exist.
     *
     * @param inetAddress     is the address to which the acknowledgement needs to be sent.
     * @param port            is the port to which the acknowledgement needs to be sent.
     * @param serverSocket    is the socket via which the acknowledgement needs to be sent.
     * @param responseMessage is the message that needs to  be sent.
     * @param flag            is the flag that needs to be set.
     */
    public void respondToRequestOfClient(InetAddress inetAddress, int port, DatagramSocket serverSocket, String responseMessage, int receivedSeqNr, int flag) {
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        int acknowledgementNumber = receivedSeqNr;
        byte[] response = PacketProtocol.createPacketWithHeader(0, sequenceNumber, acknowledgementNumber, flag, responseMessage.getBytes());
        DatagramPacket responsePacket = new DatagramPacket(response, response.length, inetAddress, port);
        try {
            serverSocket.send(responsePacket);
        } catch (IOException e) {
            throw new RuntimeException(e); //todo
        }
    }

    /**
     * Send a response to a list or download request from the client with the total file size set in the header.
     *
     * @param inetAddress is the address to which the acknowledgement needs to be sent.
     * @param port        is the port to which the acknowledgement needs to be sent.
     * @param fileSize    is the total size of the file to be received by the client (and sent by the server).
     */
    public void respondWithTotalFileSize(InetAddress inetAddress, int port, int fileSize, int receivedSeqNr) {
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        int acknowledgementNumber = receivedSeqNr;
        byte[] response = PacketProtocol.createHeader(fileSize, sequenceNumber, acknowledgementNumber, PacketProtocol.ACK);
        DatagramPacket responsePacket = new DatagramPacket(response, response.length, inetAddress, port);
        try {
            serverSocket.send(responsePacket);
        } catch (IOException e) {
            throw new RuntimeException(e); //todo
        }
    }

    /**
     * Receive an acknowledgement on the file size that is sent from the client.
     *
     * @return true if the acknowledgement is received, false if not.
     */
    public boolean receiveAckFromClient() {
        byte[] responsePacket = new byte[PacketProtocol.HEADER_SIZE];
        DatagramPacket packetToReceive = new DatagramPacket(responsePacket, responsePacket.length);
        try {
            serverSocket.receive(packetToReceive);
        } catch (IOException e) {
            e.printStackTrace(); // todo
            return false;
        }
        if (PacketProtocol.getFlag(packetToReceive.getData()) == (PacketProtocol.ACK)) {
            // get information from this packet.
            int lastSentSeqNr = PacketProtocol.getAcknowledgementNumber(packetToReceive.getData());
            setLastSentSeqNr(lastSentSeqNr);
            int lastReceivedSeqNr = PacketProtocol.getSequenceNumber(packetToReceive.getData());
            setLastReceivedSeqNr(lastReceivedSeqNr);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Receive a file from the client on the server (PI).
     *
     * @param fileName is the file to be received.
     */
    public void receiveFile(String fileName, int totalFileSize, int receivedSeqNr, InetAddress inetAddress, int port, DatagramSocket serverSocket) {
        String responseMessage = ("Server successfully received the request for uploading " + fileName);
        File filePath = FileProtocol.createFilePath(FileProtocol.SERVER_FILEPATH);
        if (!FileProtocol.checkIfFileExists(fileName, filePath)) {
            respondToRequestOfClient(inetAddress, port, serverSocket, responseMessage, receivedSeqNr, PacketProtocol.ACK);
                StopAndWaitProtocol.receiveFile(serverSocket, totalFileSize, fileName);
            System.out.println("Activate Stop and Wait protocol - receive file here.");
        } else {
            responseMessage = ("The server already has a file " + fileName + ". Therefore, upload cannot take place.");
            respondToRequestOfClient(inetAddress, port, serverSocket, responseMessage, receivedSeqNr, (PacketProtocol.DOESALREADYEXIST + PacketProtocol.ACK));
        }
    }

    /**
     * Send a file to the client from the server (PI).
     *
     * @param fileName is the file to be sent.
     */
    public void sendFile(String fileName, int receivedSeqNr, InetAddress inetAddress, int port, DatagramSocket serverSocket) {
        File filePath = FileProtocol.createFilePath(FileProtocol.SERVER_FILEPATH);
        if (!FileProtocol.checkIfFileExists(fileName, filePath)) {
            String responseMessage = ("The file " + fileName + " does not exist on the server and can therefore not be downloaded.");
            respondToRequestOfClient(inetAddress, port, serverSocket, responseMessage, receivedSeqNr, (PacketProtocol.ACK + PacketProtocol.DOESNOTEXIST));
        } else {
            respondWithTotalFileSize(inetAddress, port, FileProtocol.getFileSize(FileProtocol.SERVER_FILEPATH, fileName), receivedSeqNr);
            if (receiveAckFromClient()) {
                byte[] fileToSendInBytes = FileProtocol.fileToBytes(FileProtocol.SERVER_FILEPATH, fileName);
                int lastUsedSeqNr = getLastSentSeqNr();
                int lastReceivedSeqNr = getLastReceivedSeqNr();
                StopAndWaitProtocol.sendFile(fileToSendInBytes, lastUsedSeqNr, lastReceivedSeqNr, serverSocket, inetAddress, port);
                System.out.println("Activate Stop and Wait protocol - send list of files here if files are available.");
            }
        }
    }

    /**
     * Remove a file from the server (PI).
     *
     * @param fileName is the file to be removed.
     */
    public void removeFile(String fileName, int receivedSeqNr, InetAddress inetAddress, int port, DatagramSocket serverSocket) {
        String responseMessage = ("Server successfully received the request for removing " + fileName + ". File is removed.");
        File filePath = FileProtocol.createFilePath(FileProtocol.SERVER_FILEPATH);
        if (FileProtocol.checkIfFileExists(fileName, filePath)) {
            File[] listOfFiles = filePath.listFiles();
            for (File file : listOfFiles) {
                if (file.getName().equals(fileName)) {
                    file.delete();
                }
            }
            respondToRequestOfClient(inetAddress, port, serverSocket, responseMessage, receivedSeqNr, PacketProtocol.ACK);
        } else {
            responseMessage = (fileName + " cannot be removed by the server as it does not exist.");
            respondToRequestOfClient(inetAddress, port, serverSocket, responseMessage, receivedSeqNr, (PacketProtocol.DOESNOTEXIST + PacketProtocol.ACK));
        }
    }

    /**
     * Replace a file on the server (PI).
     *
     * @param oldFileName is the file to be replaced.
     * @param newFileName is the new file to be uploaded.
     */
    public void replaceFile(String oldFileName, String newFileName, int totalFileSize, int receivedSeqNr, InetAddress inetAddress,
                            int port, DatagramSocket serverSocket) {
        String responseMessage = ("Server successfully received the request for replacing " + oldFileName + " by " + newFileName + ".");
        File filePath = FileProtocol.createFilePath(FileProtocol.SERVER_FILEPATH);
        if (FileProtocol.checkIfFileExists(oldFileName, filePath)) {
            File[] listOfFiles = filePath.listFiles();
            for (File file : listOfFiles) {
                if (file.getName().equals(oldFileName)) {
                    file.delete();
                }
            }
            respondToRequestOfClient(inetAddress, port, serverSocket, responseMessage, receivedSeqNr, PacketProtocol.ACK);
                StopAndWaitProtocol.receiveFile(serverSocket, totalFileSize, newFileName);
        } else {
            responseMessage = (oldFileName + " cannot be replaced by the server as it does not exist.");
            respondToRequestOfClient(inetAddress, port, serverSocket, responseMessage, receivedSeqNr, (PacketProtocol.DOESNOTEXIST + PacketProtocol.ACK));
        }
    }

    /**
     * List the files that are located on the server (PI).
     */
    public void listFiles(int receivedSeqNr, InetAddress inetAddress, int port, DatagramSocket serverSocket) {
        File filePath = FileProtocol.createFilePath(FileProtocol.SERVER_FILEPATH);
        List<String> listOfFileNames = FileProtocol.createListOfFileNames(filePath);
        if (listOfFileNames.isEmpty()) {
            String responseMessage = ("There are no files stored on the server yet.");
            respondToRequestOfClient(inetAddress, port, serverSocket, responseMessage, receivedSeqNr, (PacketProtocol.ACK + PacketProtocol.DOESNOTEXIST));
        } else {
            byte[] byteArrayOfFileNameList = FileProtocol.createByteArrayOfFileNameList(listOfFileNames);
            int fileSize = byteArrayOfFileNameList.length;
            respondWithTotalFileSize(inetAddress, port, fileSize, receivedSeqNr);
            if (receiveAckFromClient()) {
                int lastUsedSeqNr = getLastSentSeqNr();
                int lastReceivedSeqNr = getLastReceivedSeqNr();
                StopAndWaitProtocol.sendFile(byteArrayOfFileNameList, lastUsedSeqNr, lastReceivedSeqNr, serverSocket, inetAddress, port);
                System.out.println("Activate Stop and Wait protocol - send list of files here if files are available.");
            }
        }
    }

    /**
     * Close the connection between server and client as response to the close request by the client.
     */
    public void respondToClosingClient(int receivedSeqNr, InetAddress inetAddress, int port, DatagramSocket serverSocket) {
        String responseMessage = ("Server successfully received that you are closing the application.");
        respondToRequestOfClient(inetAddress, port, serverSocket, responseMessage, receivedSeqNr, PacketProtocol.ACK);
    }
}
