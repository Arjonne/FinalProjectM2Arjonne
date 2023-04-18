package com.nedap.university.server;

import com.nedap.university.*;

import java.io.File;
import java.io.IOException;
import java.net.*;

/**
 * Represents the server on the Raspberry Pi.
 */
public class Server {
    private final int port;
    private boolean isOpen;
    private DatagramSocket serverSocket;
    private final File filePath;

    /**
     * Create the server with the port and address of the Raspberry Pi.
     */
    public Server() {
        // the port on which the server is listening on:
        port = PacketProtocol.PI_PORT;
        // after creating the server, it is not opened yet:
        isOpen = false;
        filePath = FileProtocol.createFilePath(FileProtocol.SERVER_FILEPATH);
    }

    /**
     * Start the server. Can only be done when server is not open for connections yet. A new DatagramSocket should start
     * with the port of the Raspberry Pi as input. This socket can be connected to the client to only accept packets
     * that are sent by that client. The boolean isOpen should be set on true as the server is now open for connections.
     * Furthermore, the filepath is created where files are stored on the Raspberry Pi.
     */
    public void start() {
        if (isOpenForConnection()) {
            System.out.println("Server on raspberry Pi is already in use.");
        } else {
            try {
                serverSocket = new DatagramSocket(port);
                ClientHandler clientHandler = new ClientHandler(serverSocket, this);
                clientHandler.start();
                isOpen = true;
            } catch (SocketException e) {
                System.out.println("Raspberry Pi already uses this port; try another port.");
            }
        }
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
     * Receive the request packet from the client in order to be able to execute the request.
     *
     * @return the Datagram Packet that is received. Return null if the server could not receive a packet.
     */
    public DatagramPacket receiveRequest() {
        byte[] requestPacket = new byte[PacketProtocol.PACKET_WITH_MESSAGE_SIZE];
        DatagramPacket packetToReceive = new DatagramPacket(requestPacket, requestPacket.length);
        try {
            serverSocket.receive(packetToReceive);
        } catch (IOException e) {
            return null;
        }
        return packetToReceive;
    }

    /**
     * Receive a file from the client on the server (Raspberry Pi).
     *
     * @param fileName          is the name of the file to be received.
     * @param totalFileSize     is the total size of the file to be received.
     * @param lastReceivedSeqNr is the last sequence number received from the client.
     * @param inetAddress       is the address of the client that sent the request.
     * @param port              is the port the client that sent the request uses to connect to the Raspberry Pi.
     * @param serverSocket      is the socket via which the server and client are connected.
     */
    public void receiveFile(String fileName, int totalFileSize, int lastReceivedSeqNr, InetAddress inetAddress, int port, DatagramSocket serverSocket) {
        if (!FileProtocol.checkIfFileExists(fileName, filePath)) {
            // if the file not exists on the server yet, it can be uploaded. Create a message that can be sent in the
            // acknowledgement and send this acknowledgement to the server:
            String responseMessage = ("Server successfully received the request for uploading " + fileName);
            Acknowledgement.sendInitialAcknowledgementWithMessage(0, 0, lastReceivedSeqNr, responseMessage, serverSocket, inetAddress, port);
            // receive the file that the client wants to upload:
            StopAndWaitProtocol.receiveFile(serverSocket, totalFileSize);
            File uploadedFile = FileProtocol.bytesToFile(FileProtocol.SERVER_FILEPATH, fileName, StopAndWaitProtocol.getFileInBytes());
            // perform check on integrity.
            DataIntegrityCheck.receiveAndPerformTotalChecksum(serverSocket, inetAddress, port, uploadedFile);
        } else {
            String responseMessage = (fileName + " is already stored on the server. You can therefore not upload this file.");
            Acknowledgement.sendInitialAcknowledgementWithMessage(PacketProtocol.DOESALREADYEXIST, 0, lastReceivedSeqNr, responseMessage, serverSocket, inetAddress, port);
        }
    }

    /**
     * Send a file to the client from the server (Raspberry Pi).
     *
     * @param fileName          is the name of the file to be sent.
     * @param lastReceivedSeqNr is the last sequence number received from the client.
     * @param inetAddress       is the address of the client that sent the request.
     * @param port              is the port the client that sent the request uses to connect to the Raspberry Pi.
     * @param serverSocket      is the socket via which the server and client are connected.
     */
    public void sendFile(String fileName, int lastReceivedSeqNr, InetAddress inetAddress, int port, DatagramSocket serverSocket) {
        if (!FileProtocol.checkIfFileExists(fileName, filePath)) {
            String responseMessage = (fileName + " does not exist on the server and can therefore not be downloaded.");
            Acknowledgement.sendInitialAcknowledgementWithMessage(PacketProtocol.DOESNOTEXIST, 0, lastReceivedSeqNr, responseMessage, serverSocket, inetAddress, port);
        } else {
            // get the size of the file to send and create a response message. Try to send an acknowledgement to the
            // client with this information in it, and try to receive an acknowledgement as sign that the server can
            // start sending the file. If the acknowledgement is not received in time, the acknowledgement of the server
            // (with fileSize and message) will be sent again.
            int fileSize = FileProtocol.getFileSize(FileProtocol.SERVER_FILEPATH, fileName);
            String responseMessage = ("Server successfully received the request for downloading " + fileName);
            Acknowledgement.sendAckWithFileSizeAndReceiveAck(0, fileSize, lastReceivedSeqNr, responseMessage, inetAddress, port, serverSocket);
            byte[] ackReceived = Acknowledgement.getAcknowledgement();
            if (PacketProtocol.getFlag(ackReceived) == PacketProtocol.ACK) {
                // first, get some information from the acknowledgement that is received:
                lastReceivedSeqNr = PacketProtocol.getSequenceNumber(ackReceived);
                int lastReceivedAckNr = PacketProtocol.getAcknowledgementNumber(ackReceived);
                // create a byte representation from the (new) file that needs to be uploaded to the server:
                byte[] fileToSendInBytes = FileProtocol.fileToBytes(FileProtocol.SERVER_FILEPATH, fileName);
                // send the byte representation of the file to the client:
                if (fileToSendInBytes != null) {
                    StopAndWaitProtocol.sendFile(fileToSendInBytes, lastReceivedAckNr, lastReceivedSeqNr, serverSocket, inetAddress, port);
                    // calculate the checksum of the original file and send it to the server:
                    int checksumOfTotalFile = DataIntegrityCheck.calculateChecksum(fileToSendInBytes);
                    lastReceivedSeqNr = StopAndWaitProtocol.getLastReceivedSeqNr();
                    lastReceivedAckNr = StopAndWaitProtocol.getLastReceivedAckNr();
                    // create packet with checksum of total file in it, send it to the server and try to receive an ACK:
                    DatagramPacket checksumToSend = DataIntegrityCheck.createChecksumPacket(checksumOfTotalFile, lastReceivedSeqNr, lastReceivedAckNr, inetAddress, port);
                    if (Acknowledgement.sendChecksumAndReceiveAck(serverSocket, checksumToSend)) {
                        System.out.println(fileName + " is successfully downloaded by the client.");
                    } else {
                        System.out.println("The download of " + fileName + " was not successful.");
                    }
                }
            }
        }
    }

    /**
     * Remove a file from the server (Raspberry Pi).
     *
     * @param fileName          is the name of the file to be sent.
     * @param lastReceivedSeqNr is the last sequence number received from the client.
     * @param inetAddress       is the address of the client that sent the request.
     * @param port              is the port the client that sent the request uses to connect to the Raspberry Pi.
     * @param serverSocket      is the socket via which the server and client are connected.
     */
    public void removeFile(String fileName, int lastReceivedSeqNr, InetAddress inetAddress, int port, DatagramSocket
            serverSocket) {
        if (isFileRemoved(fileName)) {
            String responseMessage = ("Server successfully received the request for removing " + fileName + ". File is removed.");
            Acknowledgement.sendInitialAcknowledgementWithMessage(0, 0, lastReceivedSeqNr, responseMessage, serverSocket, inetAddress, port);
        } else {
            String responseMessage = (fileName + " cannot be removed by the server as it does not exist.");
            Acknowledgement.sendInitialAcknowledgementWithMessage(PacketProtocol.DOESNOTEXIST, 0, lastReceivedSeqNr, responseMessage, serverSocket, inetAddress, port);
        }
    }

    /**
     * Replace a file on the server (Raspberry Pi)
     *
     * @param oldFileName       is the file to be replaced.
     * @param newFileName       is the new file to be received.
     * @param totalFileSize     is the total size of the file to be received.
     * @param lastReceivedSeqNr is the last sequence number received from the client.
     * @param inetAddress       is the address of the client that sent the request.
     * @param port              is the port the client that sent the request uses to connect to the Raspberry Pi.
     * @param serverSocket      is the socket via which the server and client are connected.
     */
    public void replaceFile(String oldFileName, String newFileName, int totalFileSize,
                            int lastReceivedSeqNr, InetAddress inetAddress,
                            int port, DatagramSocket serverSocket) {
        // if the old file exists on the server, first try to remove it. Then, try to receive the new file from the client.
        if (isFileRemoved(oldFileName)) {
            String responseMessage = ("Server successfully received the request for replacing " + oldFileName + " by " + newFileName + ".");
            Acknowledgement.sendInitialAcknowledgementWithMessage(0, 0, lastReceivedSeqNr, responseMessage, serverSocket, inetAddress, port);
            StopAndWaitProtocol.receiveFile(serverSocket, totalFileSize);
            File replacingFile = FileProtocol.bytesToFile(FileProtocol.SERVER_FILEPATH, newFileName, StopAndWaitProtocol.getFileInBytes());
            DataIntegrityCheck.receiveAndPerformTotalChecksum(serverSocket, inetAddress, port, replacingFile);
        } else {
            String responseMessage = (oldFileName + " cannot be replaced by the server as it does not exist.");
            Acknowledgement.sendInitialAcknowledgementWithMessage(PacketProtocol.DOESNOTEXIST, 0, lastReceivedSeqNr, responseMessage, serverSocket, inetAddress, port);
        }
    }


    /**
     * List the files that are located on the server (Raspberry Pi).
     *
     * @param lastReceivedSeqNr is the last sequence number received from the client.
     * @param inetAddress       is the address of the client that sent the request.
     * @param port              is the port the client that sent the request uses to connect to the Raspberry Pi.
     * @param serverSocket      is the socket via which the server and client are connected.
     */
    public void listFiles(int lastReceivedSeqNr, InetAddress inetAddress, int port, DatagramSocket serverSocket) {
        if (!FileProtocol.areFilesStoredOnServer(filePath)) {
            String responseMessage = ("There are no files stored on the server yet.");
            Acknowledgement.sendInitialAcknowledgementWithMessage(PacketProtocol.DOESNOTEXIST, 0, lastReceivedSeqNr, responseMessage, serverSocket, inetAddress, port);
        } else {
            // get the size of the list to send and create a response message. Try to send an acknowledgement to the
            // client with this information in it, and try to receive an acknowledgement as sign that the server can
            // start sending the file. If the acknowledgement is not received in time, the acknowledgement of the server
            // (with fileSize and message) will be sent again.
            String listOfFileNames = FileProtocol.createListOfFileNames(filePath);
            byte[] listOfFilesInBytes = listOfFileNames.getBytes();
            int fileSize = listOfFilesInBytes.length;
            String responseMessage = ("Server successfully received the request for listing all files.");
            Acknowledgement.sendAckWithFileSizeAndReceiveAck(0, fileSize, lastReceivedSeqNr, responseMessage, inetAddress, port, serverSocket);
            // get information from the received acknowledgement and send the list of stored files:
            byte[] ackReceived = Acknowledgement.getAcknowledgement();
            int lastReceivedAckNr = PacketProtocol.getAcknowledgementNumber(ackReceived);
            lastReceivedSeqNr = PacketProtocol.getSequenceNumber(ackReceived);
            StopAndWaitProtocol.sendFile(listOfFilesInBytes, lastReceivedAckNr, lastReceivedSeqNr, serverSocket, inetAddress, port);
        }
    }

    /**
     * Let the client know that the server received the message that the client is disconnecting.
     *
     * @param lastReceivedSeqNr is the last sequence number received from the client.
     * @param inetAddress       is the address of the client that sent the request.
     * @param port              is the port the client that sent the request uses to connect to the Raspberry Pi.
     * @param serverSocket      is the socket via which the server and client are connected.
     */
    public void respondToClosingClient(int lastReceivedSeqNr, InetAddress inetAddress, int port, DatagramSocket
            serverSocket) {
        String responseMessage = ("Server successfully received that you are closing the application.");
        Acknowledgement.sendInitialAcknowledgementWithMessage(0, 0, lastReceivedSeqNr, responseMessage, serverSocket, inetAddress, port);
    }

    /**
     * Remove the file if this file actually exists.
     *
     * @param fileName is the name of the file.
     * @return true if the file existed and could be removed, false if not.
     */
    public boolean isFileRemoved(String fileName) {
        File[] listOfFiles = filePath.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                return file.getName().equals(fileName) && file.delete();
            }
        }
        return false;
    }
}
