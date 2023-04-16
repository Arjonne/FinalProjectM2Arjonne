package com.nedap.university.server;

import com.nedap.university.FileProtocol;
import com.nedap.university.PacketProtocol;
import com.nedap.university.StopAndWaitProtocol;

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
    private byte[] ackPacket;

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
     * Receive a file from the client on the server (PI).
     *
     * @param fileName is the file to be received.
     */
    public void receiveFile(String fileName, int totalFileSize, int receivedSeqNr, InetAddress inetAddress, int port, DatagramSocket serverSocket) {
        String responseMessage = ("Server successfully received the request for uploading " + fileName);
        File filePath = FileProtocol.createFilePath(FileProtocol.SERVER_FILEPATH);
        if (!FileProtocol.checkIfFileExists(fileName, filePath)) {
            respondToRequestOfClient(inetAddress, port, serverSocket, responseMessage, receivedSeqNr, PacketProtocol.ACK);
            System.out.println("Activate Stop and Wait protocol - receive file here.");
            StopAndWaitProtocol.receiveFile(serverSocket, totalFileSize, fileName);
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
            if (receiveAcknowledgement(serverSocket)) {
                // get information from this packet:
                byte[] ackReceived = getAckPacket();
                int lastSentSeqNr = PacketProtocol.getAcknowledgementNumber(ackReceived);
//                setLastSentSeqNr(lastSentSeqNr);
                int lastReceivedSeqNr = PacketProtocol.getSequenceNumber(ackReceived);
//                setLastReceivedSeqNr(lastReceivedSeqNr);
                byte[] fileToSendInBytes = FileProtocol.fileToBytes(FileProtocol.SERVER_FILEPATH, fileName);
                System.out.println("Start Stop and Wait protocol - send file here.");
                StopAndWaitProtocol.sendFile(fileToSendInBytes, lastSentSeqNr, lastReceivedSeqNr, serverSocket, inetAddress, port);
                // todo eerst send Hash
                System.out.println("Send hash code");
                // todo dan wacht op ACK of INCORRECT.
                System.out.println("Wait for ACK or INCORRECT");
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
            System.out.println(fileName + " is successfully removed.");
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
                    break;
                }
            }
            System.out.println(oldFileName + " is successfully removed.");
            respondToRequestOfClient(inetAddress, port, serverSocket, responseMessage, receivedSeqNr, PacketProtocol.ACK);
            System.out.println("start uploading new file by using receive function of sw");
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
        if (!FileProtocol.areFilesStoredOnServer(filePath)) {
            String responseMessage = ("There are no files stored on the server yet.");
            respondToRequestOfClient(inetAddress, port, serverSocket, responseMessage, receivedSeqNr, (PacketProtocol.ACK + PacketProtocol.DOESNOTEXIST));
        } else {
            String listOfFileNames = FileProtocol.createListOfFileNames(filePath);
            byte[] listOfFilesInBytes = listOfFileNames.getBytes();
            int fileSize = listOfFilesInBytes.length;
            respondWithTotalFileSize(inetAddress, port, fileSize, receivedSeqNr);
            if (receiveAcknowledgement(serverSocket)) {
                byte[] ackReceived = getAckPacket();
                int lastSentSeqNr = PacketProtocol.getAcknowledgementNumber(ackReceived);
                int lastReceivedSeqNr = PacketProtocol.getSequenceNumber(ackReceived);
                System.out.println("Start Stop and Wait protocol - send file here.");
                StopAndWaitProtocol.sendFile(listOfFilesInBytes, lastSentSeqNr, lastReceivedSeqNr, serverSocket, inetAddress, port);
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

    /**
     * Try to receive an acknowledgement.
     *
     * @return true if the acknowledgement is received, false if not.
     */
    public boolean receiveAcknowledgement(DatagramSocket socket) {
        byte[] ackPacket = new byte[PacketProtocol.HEADER_SIZE];
        DatagramPacket ackToReceive = new DatagramPacket(ackPacket, ackPacket.length);
        try {
            socket.receive(ackToReceive);
            setAckPacket(ackPacket);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void setAckPacket(byte[] ackReceived) {
        ackPacket = new byte[PacketProtocol.HEADER_SIZE];
        for (int i = 0; i < PacketProtocol.HEADER_SIZE; i++) {
            ackPacket[i] = ackReceived[i];
        }
    }

    public byte[] getAckPacket() {
        return ackPacket;
    }
}
