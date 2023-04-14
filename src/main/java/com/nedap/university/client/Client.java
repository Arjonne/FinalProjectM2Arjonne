package com.nedap.university.client;

import com.nedap.university.FileProtocol;
import com.nedap.university.PacketProtocol;
import com.nedap.university.StopAndWaitProtocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Represents the client for the file transfer.
 */
public class Client implements Runnable {
    ClientTUI clientTUI;
    private DatagramSocket clientSocket;
    private boolean quit;
    private boolean tryToReceive;
    int totalFileSize;
    String fileName;
    String oldFileName;
    String newFileName;
    int requestFlag;
    int receivedFlag;
    int receivedSeqNr;
    int lastSentSeqNr;
    DatagramPacket requestPacket;

    /**
     * Create a new client that uses the textual user interface for file transmission.
     */
    public Client(ClientTUI clientTUI) {
        this.clientTUI = clientTUI;
    }

    /**
     * Start the client on a separate thread to be able to receive input from the server via the Client and from the TUI
     * via the ClientTUI.
     *
     * @return true if client has successfully started, false if not.
     */
    public boolean startClient() {
        try {
            clientSocket = new DatagramSocket();
            Thread clientThread = new Thread(this);
            clientThread.start();
            return true;
        } catch (IOException e) {
            System.out.println("Connection could not be established.");
            return false;
        }
    }

    /**
     * Stop the client and by doing so, close the TUI.
     */
    public void stopClient() {
        quit = true;
        clientSocket.close();
        System.out.println("Client has stopped, application is closed.");
    }

    /**
     * As long as the client thread is running, first wait for input from the TUI, communicate the correct command to
     * the server and wait for a response of the server.
     */
    @Override
    public void run() {
        quit = false;
        while (!quit) {
            while (!tryToReceive) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e); // todo
                }
            }
            if (receiveAckFromServer()) {
                if (receivedFlag == PacketProtocol.ACK) {
                    if (requestFlag == PacketProtocol.DOWNLOAD || requestFlag == PacketProtocol.LIST) {
                        System.out.println("first try to receive file size OR list size");
                        int totalFileSize = getTotalFileSize();
                        System.out.println("then, start SW protocol receive file.");
                        StopAndWaitProtocol.receiveFile(clientSocket, totalFileSize, fileName);
                        System.out.println("Give the command you want to execute next:");
                    } else if (requestFlag == PacketProtocol.UPLOAD || requestFlag == PacketProtocol.REPLACE) {
                        int lastSentSeqNr = getLastSentSeqNr();
                        int lastReceivedSeqNr = getReceivedSeqNr();
                        byte[] dataOfFileToSend = FileProtocol.fileToBytes(FileProtocol.CLIENT_FILEPATH, fileName);
                        System.out.println("Try to send file.");
                        try {
                            StopAndWaitProtocol.sendFile(dataOfFileToSend, lastSentSeqNr, lastReceivedSeqNr, clientSocket, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e); // todo
                        }
                        System.out.println("Give the command you want to execute next:");
                    } else if (requestFlag == PacketProtocol.CLOSE) {
                        stopClient();
                    }
                } else if ((receivedFlag == (PacketProtocol.ACK + PacketProtocol.DOESNOTEXIST)) || (receivedFlag == (PacketProtocol.ACK + PacketProtocol.DOESALREADYEXIST))) {
                    System.out.println("Give the command you want to execute next:");
                }
            } else {
                System.out.println("Send request again: packet with ACK was not received so server might not have received the request.");
                resendRequest(getRequestPacket());
            }
            tryToReceive = false;
        }
    }

    /**
     * Set the boolean sendToServer to true when user has given input via TUI that needs to be communicated to the
     * server.
     */
    public void activateTryToReceive() {
        tryToReceive = true;
    }

    /**
     * Send request to server.
     *
     * @param fileNameFromRequest is the name of the file (from the request of the user) with which the server needs
     *                            to do something.
     * @param flag                represents the action the server needs to do.
     */
    public void sendRequest(String fileNameFromRequest, int flag, int fileSize) {
        byte[] fileData = FileProtocol.createRequestFile(fileNameFromRequest);
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        byte[] request = PacketProtocol.createPacketWithHeader(fileSize, sequenceNumber, 0, flag, fileData);
        try {
            DatagramPacket requestPacket = new DatagramPacket(request, request.length, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
            setRequestPacket(requestPacket);
            clientSocket.send(requestPacket);
        } catch (IOException e) {
            e.printStackTrace(); //todo
        }
        setLastSentSeqNr(sequenceNumber);
        requestFlag = flag;
        fileName = fileNameFromRequest;
        activateTryToReceive();
    }

    /**
     * Send replace request to server (flag is internally set).
     *
     * @param oldFileNameFromRequest is the name of the file (from the request of the user) that the server needs to
     *                               replace.
     * @param newFileNameFromRequest is the name of the new file (from the request of the user) that the server needs
     *                               to upload.
     */
    public void sendReplaceRequest(String oldFileNameFromRequest, String newFileNameFromRequest, int fileSize) {
        byte[] fileData = FileProtocol.createRequestFile(oldFileNameFromRequest + " " + newFileNameFromRequest);
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        byte[] request = PacketProtocol.createPacketWithHeader(fileSize, sequenceNumber, 0, PacketProtocol.REPLACE, fileData);
        try {
            DatagramPacket requestPacket = new DatagramPacket(request, request.length, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
            clientSocket.send(requestPacket);
        } catch (IOException e) {
            e.printStackTrace(); //todo
        }
        setLastSentSeqNr(sequenceNumber);
        requestFlag = PacketProtocol.REPLACE;
        oldFileName = oldFileNameFromRequest;
        newFileName = newFileNameFromRequest;
        activateTryToReceive();
    }

    /**
     * Send list request to server (flag is internally set and no file name(s) are needed here).
     */
    public void sendListOrCloseRequest(int flag) {
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        byte[] request = PacketProtocol.createHeader(0, sequenceNumber, 0, flag);
        try {
            DatagramPacket requestPacket = new DatagramPacket(request, request.length, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
            clientSocket.send(requestPacket);
        } catch (IOException e) {
            e.printStackTrace(); //todo
        }
        setLastSentSeqNr(sequenceNumber);
        requestFlag = flag;
        activateTryToReceive();
    }

    /**
     * Set the request DatagramPacket to the last request.
     *
     * @param requestPacket is the DatagramPacket that includes the last request.
     */
    public void setRequestPacket(DatagramPacket requestPacket) {
        this.requestPacket = requestPacket;
    }

    /**
     * Get the request DatagramPacket of the last request (in case a request is not received by the server and needs
     * to be retransmitted).
     *
     * @return the request DatagramPacket.
     */
    public DatagramPacket getRequestPacket() {
        return requestPacket;
    }

    /**
     * Send request DatagramPacket again in case acknowledgement was not received in time from the server (which might
     * indicate that request never arrived as well).
     *
     * @param requestPacket is the DatagramPacket that includes the request that was last sent.
     */
    public void resendRequest(DatagramPacket requestPacket) {
        try {
            clientSocket.send(requestPacket);
        } catch (IOException e) {
            e.printStackTrace(); //todo boolean van maken true/false? blijven proberen...
        }
    }

    /**
     * Set the variable totalFileSize to the actual total file size of the file to be transmitted.
     *
     * @param totalFileSize is the total size of the file to be transmitted.
     */
    public void setTotalFileSize(int totalFileSize) {
        this.totalFileSize = totalFileSize;
    }

    /**
     * Get the total file size of the file to be transmitted.
     *
     * @return the total file size.
     */
    public int getTotalFileSize() {
        return totalFileSize;
    }

    /**
     * Set the receivedFlag to the value of the flag(s) that are received in the acknowledgement.
     *
     * @param receivedFlag is the value of the received flag(s).
     */
    public void setReceivedFlag(int receivedFlag) {
        this.receivedFlag = receivedFlag;
    }

    /**
     * Get the flag(s) that is/are set in the acknowledgement packet that is received.
     *
     * @return the flag(s) that is/are set.
     */
    public int getReceivedFlag() {
        return receivedFlag;
    }

    /**
     * Get the last received sequence number in order to respond properly with an acknowledgement.
     *
     * @return the last received sequence number.
     */
    public int getReceivedSeqNr() {
        return receivedSeqNr;
    }

    /**
     * Set the last received sequence number in order to respond properly with an acknowledgement.
     *
     * @param receivedSeqNr is the received sequence number.
     */
    public void setReceivedSeqNr(int receivedSeqNr) {
        this.receivedSeqNr = receivedSeqNr;
    }

    /**
     * Get the last sequence number sent in order to properly increase this number in the next packet to send.
     *
     * @return the last sequence number sent.
     */
    public int getLastSentSeqNr() {
        return lastSentSeqNr;
    }

    /**
     * Set the last sequence number sent in order to properly increase this number in the next packet to send.
     *
     * @param lastSentSeqNr is the sequence number sent.
     */
    public void setLastSentSeqNr(int lastSentSeqNr) {
        this.lastSentSeqNr = lastSentSeqNr;
    }

    /**
     * Check if client has received an acknowledgement from the server (as response to the request).
     *
     * @return true if acknowledgement is received, false if not.
     */
    public boolean receiveAckFromServer() {
        byte[] responsePacket = new byte[256];
        DatagramPacket packetToReceive = new DatagramPacket(responsePacket, responsePacket.length);
        try {
            clientSocket.receive(packetToReceive);
        } catch (IOException e) {
            e.printStackTrace(); // todo
            return false;
        }
        setReceivedFlag(PacketProtocol.getFlag(packetToReceive.getData()));
        if (getReceivedFlag() == (PacketProtocol.ACK)) {
            String messageFromServer = new String(packetToReceive.getData(), PacketProtocol.HEADER_SIZE, (packetToReceive.getLength() - PacketProtocol.HEADER_SIZE));
            // get information from this acknowledgement and store it to be able to reuse it in the sending/receiving protocol.
            int totalFileSize = PacketProtocol.getFileSizeInPacket(packetToReceive.getData());
            setTotalFileSize(totalFileSize);
            int receivedSeqNr = PacketProtocol.getSequenceNumber(packetToReceive.getData());
            setReceivedSeqNr(receivedSeqNr);
            System.out.println(messageFromServer);
            return true;
        } else if ((getReceivedFlag() == (PacketProtocol.DOESALREADYEXIST + PacketProtocol.ACK)) || getReceivedFlag() == (PacketProtocol.DOESNOTEXIST + PacketProtocol.ACK)) {
            String messageFromServer = new String(packetToReceive.getData(), PacketProtocol.HEADER_SIZE, (packetToReceive.getLength() - PacketProtocol.HEADER_SIZE));
            System.out.println(messageFromServer);
            return true;
        } else {
            return false;
        }
    }
}
