package com.nedap.university.server;

import com.nedap.university.*;

import java.io.File;
import java.io.IOException;
import java.net.*;

/**
 * Represents the server on the Raspberry Pi.
 */

public class Server {
    private int port;
    private boolean isOpen;
    private DatagramSocket serverSocket;

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
            return null;
        }
        return packetToReceive;
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
            Acknowledgement.sendInitialAcknowledgementWithMessage(0, 0, receivedSeqNr, responseMessage, serverSocket, inetAddress, port);
            StopAndWaitProtocol.receiveFile(serverSocket, totalFileSize);
            File uploadedFile = FileProtocol.bytesToFile(FileProtocol.SERVER_FILEPATH, fileName, StopAndWaitProtocol.getFileInBytes());
            System.out.println("Try to receive hash code");

            DatagramPacket packetWithChecksum = DataIntegrityCheck.receiveChecksum(serverSocket);
            int receivedFlag = PacketProtocol.getFlag(packetWithChecksum.getData());
            int receivedChecksum = DataIntegrityCheck.getChecksum(packetWithChecksum);
            byte[] receivedFileInBytes = StopAndWaitProtocol.getFileInBytes();
            int checksumOfReceivedFile = DataIntegrityCheck.calculateChecksum(receivedFileInBytes);
            if (receivedFlag == PacketProtocol.CHECK) {
                int lastReceivedSeqNr = PacketProtocol.getSequenceNumber(packetWithChecksum.getData());
                int lastReceivedAck = PacketProtocol.getAcknowledgementNumber(packetWithChecksum.getData());
                System.out.println("Hashcode is received");
                if (receivedChecksum == checksumOfReceivedFile) {
                    System.out.println("The file is successfully downloaded.");
                    Acknowledgement.sendAcknowledgement(0, lastReceivedSeqNr, lastReceivedAck, serverSocket, inetAddress, port);
                } else {
                    uploadedFile.delete();
                    System.out.println("The file that you downloaded is not the same as the original file on the server and is therefore not saved.");
                    Acknowledgement.sendAcknowledgement(PacketProtocol.INCORRECT, lastReceivedSeqNr, lastReceivedAck, serverSocket, inetAddress, port);
                }
            }
            // todo: else?
        } else {
            responseMessage = ("The server already has a file " + fileName + ". Therefore, upload cannot take place.");
            Acknowledgement.sendInitialAcknowledgementWithMessage(PacketProtocol.DOESALREADYEXIST, 0, receivedSeqNr, responseMessage, serverSocket, inetAddress, port);
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
            Acknowledgement.sendInitialAcknowledgementWithMessage(PacketProtocol.DOESNOTEXIST, 0, receivedSeqNr, responseMessage, serverSocket, inetAddress, port);
        } else {
            int fileSize = FileProtocol.getFileSize(FileProtocol.SERVER_FILEPATH, fileName);
            String responseMessage = ("Server successfully received the request for downloading " + fileName);
            // create packet with file size, try to send it and try to receive ACK; if ack not received in time, resend the packet:
            Acknowledgement.receiveAckOnFileSize(0, fileSize, receivedSeqNr, responseMessage, inetAddress, port, serverSocket);
            // get information from this packet:
            byte[] ackReceived = Acknowledgement.getAcknowledgement();
            int lastReceivedAckNr = PacketProtocol.getAcknowledgementNumber(ackReceived);
            int lastReceivedSeqNr = PacketProtocol.getSequenceNumber(ackReceived);
            byte[] fileToSendInBytes = FileProtocol.fileToBytes(FileProtocol.SERVER_FILEPATH, fileName);
            StopAndWaitProtocol.sendFile(fileToSendInBytes, lastReceivedAckNr, lastReceivedSeqNr, serverSocket, inetAddress, port);
            System.out.println("Send hash code");

            int checksumOfTotalFile = DataIntegrityCheck.calculateChecksum(fileToSendInBytes);
            receivedSeqNr = StopAndWaitProtocol.getLastReceivedSeqNr();
            lastReceivedAckNr = StopAndWaitProtocol.getLastReceivedAckNr();

            DatagramPacket checksumToSend = DataIntegrityCheck.createChecksumPacket(checksumOfTotalFile, receivedSeqNr, lastReceivedAckNr, inetAddress, port);
            DatagramPacket ackPacket = Acknowledgement.createAckPacketToReceive();
            Acknowledgement.tryToReceiveAck(serverSocket, ackPacket, checksumToSend);
            ackReceived = Acknowledgement.getAcknowledgement();
            int flag = PacketProtocol.getFlag(ackReceived);
            if (flag == PacketProtocol.ACK) {
                System.out.println(fileName + " is successfully downloaded by the client.");
            } else {
                System.out.println("The download of " + fileName + " was not successful.");
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
                    break;
                }
            }
            Acknowledgement.sendInitialAcknowledgementWithMessage(0, 0, receivedSeqNr, responseMessage, serverSocket, inetAddress, port);
        } else {
            responseMessage = (fileName + " cannot be removed by the server as it does not exist.");
            Acknowledgement.sendInitialAcknowledgementWithMessage(PacketProtocol.DOESNOTEXIST, 0, receivedSeqNr, responseMessage, serverSocket, inetAddress, port);
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
                    break;
                }
            }
            Acknowledgement.sendInitialAcknowledgementWithMessage(0, 0, receivedSeqNr, responseMessage, serverSocket, inetAddress, port);
            StopAndWaitProtocol.receiveFile(serverSocket, totalFileSize);
            File replacingFile = FileProtocol.bytesToFile(FileProtocol.SERVER_FILEPATH, newFileName, StopAndWaitProtocol.getFileInBytes());
            System.out.println("Try to receive hash code");

            DatagramPacket packetWithChecksum = DataIntegrityCheck.receiveChecksum(serverSocket);
            int receivedFlag = PacketProtocol.getFlag(packetWithChecksum.getData());
            int receivedChecksum = DataIntegrityCheck.getChecksum(packetWithChecksum);
            byte[] receivedFileInBytes = StopAndWaitProtocol.getFileInBytes();
            int checksumOfReceivedFile = DataIntegrityCheck.calculateChecksum(receivedFileInBytes);
            if (receivedFlag == PacketProtocol.CHECK) {
                int lastReceivedSeqNr = PacketProtocol.getSequenceNumber(packetWithChecksum.getData());
                int lastReceivedAck = PacketProtocol.getAcknowledgementNumber(packetWithChecksum.getData());
                System.out.println("Hashcode is received");
                if (receivedChecksum == checksumOfReceivedFile) {
                    System.out.println("The file is successfully downloaded.");
                    Acknowledgement.sendAcknowledgement(0, lastReceivedSeqNr, lastReceivedAck, serverSocket, inetAddress, port);
                } else {
                    replacingFile.delete();
                    System.out.println("The file that you downloaded is not the same as the original file on the server and is therefore not saved.");
                    Acknowledgement.sendAcknowledgement(PacketProtocol.INCORRECT, lastReceivedSeqNr, lastReceivedAck, serverSocket, inetAddress, port);
                }
            }
            // todo: else?
        } else {
            responseMessage = (oldFileName + " cannot be replaced by the server as it does not exist.");
            Acknowledgement.sendInitialAcknowledgementWithMessage(PacketProtocol.DOESNOTEXIST, 0, receivedSeqNr, responseMessage, serverSocket, inetAddress, port);
        }
    }

    /**
     * List the files that are located on the server (PI).
     */
    public void listFiles(int receivedSeqNr, InetAddress inetAddress, int port, DatagramSocket serverSocket) {
        String responseMessage = ("Server successfully received the request for listing all files.");
        File filePath = FileProtocol.createFilePath(FileProtocol.SERVER_FILEPATH);
        if (!FileProtocol.areFilesStoredOnServer(filePath)) {
            responseMessage = ("There are no files stored on the server yet.");
            Acknowledgement.sendInitialAcknowledgementWithMessage(PacketProtocol.DOESNOTEXIST, 0, receivedSeqNr, responseMessage, serverSocket, inetAddress, port);
        } else {
            String listOfFileNames = FileProtocol.createListOfFileNames(filePath);
            byte[] listOfFilesInBytes = listOfFileNames.getBytes();
            int fileSize = listOfFilesInBytes.length;
            // create packet with file size, try to send it and try to receive ACK; if ack not received in time, resend the packet:
            Acknowledgement.receiveAckOnFileSize(0, fileSize, receivedSeqNr, responseMessage, inetAddress, port, serverSocket);
            byte[] ackReceived = Acknowledgement.getAcknowledgement();
            int lastSentSeqNr = PacketProtocol.getAcknowledgementNumber(ackReceived);
            int lastReceivedSeqNr = PacketProtocol.getSequenceNumber(ackReceived);
            System.out.println("Start Stop and Wait protocol - send file here.");
            StopAndWaitProtocol.sendFile(listOfFilesInBytes, lastSentSeqNr, lastReceivedSeqNr, serverSocket, inetAddress, port);
        }
    }

    /**
     * Close the connection between server and client as response to the close request by the client.
     */
    public void respondToClosingClient(int receivedSeqNr, InetAddress inetAddress, int port, DatagramSocket serverSocket) {
        String responseMessage = ("Server successfully received that you are closing the application.");
        Acknowledgement.sendInitialAcknowledgementWithMessage(0, 0, receivedSeqNr, responseMessage, serverSocket, inetAddress, port);
    }
}
