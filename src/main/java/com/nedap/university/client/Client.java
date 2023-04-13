package com.nedap.university.client;

import com.nedap.university.FileProtocol;
import com.nedap.university.PacketProtocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Represents the client for the file transfer.
 */
public class Client implements Runnable {
    private ClientTUI clientTUI;
    private DatagramSocket clientSocket;
    private Thread clientThread;
    private boolean quit;
    private boolean tryToReceive;
    int requestFlag;
    DatagramPacket requestPacket;

    /**
     * Create a new client that uses the textual user interface for file transmission.
     *
     * @param clientTUI
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
            clientThread = new Thread(this);
            clientThread.start();
            return true;
        } catch (IOException e) {
            System.out.println("Connection could not be established.");
            return false;
        }
    }

    /**
     * Stop the client thread and join the main thread.
     */
    public void stopClient() {
        quit = true;
        clientSocket.close();
        System.out.println("Client is stopped.");
        try {
            clientThread.join();
        } catch (InterruptedException e) {
            System.out.println("Cannot join main thread.");
        }
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
            if (requestFlag == PacketProtocol.DOWNLOAD || requestFlag == PacketProtocol.LIST) {
                System.out.println("Try to receive file or list.");
//                StopAndWaitProtocol.receiveFile(clientSocket);
            } else {
                if (receiveAcknowledgement()) {
                    if (requestFlag == PacketProtocol.UPLOAD || requestFlag == PacketProtocol.REPLACE) {
                        System.out.println("Try to send file.");
//                        StopAndWaitProtocol.sendFile();
                    } else if (requestFlag == PacketProtocol.CLOSE) {
                        System.out.println("Client will be closed.");
                        stopClient();
                    } else if (receiveError()) {
                        System.out.println("Try again: ");
                    }
                } else {
                    System.out.println("Send request again: ACK was not received so server might not have received the request.");
                    resendRequest(getRequestPacket());
                }
            }
            tryToReceive = false;
        }
        System.out.println("Connection is closed (Message from Client).");
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
     * @param fileName is the name of the file with which the server needs to do something.
     * @param flag     represents the action the server needs to do.
     */
    public void sendRequest(String fileName, int flag) {
        byte[] fileData = FileProtocol.createRequestFile(fileName);
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        byte[] request = PacketProtocol.createPacketWithHeader(sequenceNumber, flag, fileData);
        try {
            DatagramPacket requestPacket = new DatagramPacket(request, request.length, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
            setRequestPacket(requestPacket);
            clientSocket.send(requestPacket);
        } catch (IOException e) {
            e.printStackTrace(); //todo
        }
        requestFlag = flag;
        activateTryToReceive();
    }

    /**
     * Send replace request to server (flag is internally set).
     *
     * @param oldFileName is the name of the file the server needs to replace.
     * @param newFileName is the name of the new file the server needs to upload.
     */
    public void sendReplaceRequest(String oldFileName, String newFileName) {
        byte[] fileData = FileProtocol.createRequestFile(oldFileName + " " + newFileName);
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        byte[] request = PacketProtocol.createPacketWithHeader(sequenceNumber, PacketProtocol.REPLACE, fileData);
        try {
            DatagramPacket requestPacket = new DatagramPacket(request, request.length, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
            clientSocket.send(requestPacket);
        } catch (IOException e) {
            e.printStackTrace(); //todo
        }
        requestFlag = PacketProtocol.REPLACE;
        activateTryToReceive();
    }

    /**
     * Send list request to server (flag is internally set and no file name(s) are needed here).
     */
    public void sendListOrCloseRequest(int flag) {
        byte[] fileData = FileProtocol.createRequestFile("list all files");
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        byte[] request = PacketProtocol.createPacketWithHeader(sequenceNumber, flag, fileData);
        try {
            DatagramPacket requestPacket = new DatagramPacket(request, request.length, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
            clientSocket.send(requestPacket);
        } catch (IOException e) {
            e.printStackTrace(); //todo
        }
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
            e.printStackTrace(); //todo
        }
    }

    /**
     * Check if client has received an acknowledgement from the server (as response to the request).
     *
     * @return true if acknowledgement is received, false if not.
     */
    public boolean receiveAcknowledgement() {
        byte[] responsePacket = new byte[256];
        DatagramPacket packetToReceive = new DatagramPacket(responsePacket, responsePacket.length);
        try {
            clientSocket.receive(packetToReceive);
        } catch (IOException e) {
            e.printStackTrace(); // todo
        }
        if (PacketProtocol.getFlag(packetToReceive.getData()) == (PacketProtocol.ACK)) {
            String messageFromServer = new String(packetToReceive.getData(), PacketProtocol.HEADER_SIZE, (packetToReceive.getLength() - PacketProtocol.HEADER_SIZE));
            System.out.println(messageFromServer);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if the client has received an error from the server (as response to the request). This error can indicate
     * that a file already exists at the server (and therefore an upload cannot be performed), or that the file does not
     * exist on the server yet (and therefore a removal or replacement cannot be performed, or no files can be listed).
     *
     * @return true if acknowledgement is received, false if not.
     */
    public boolean receiveError() {
        byte[] responsePacket = new byte[256];
        DatagramPacket packetToReceive = new DatagramPacket(responsePacket, responsePacket.length);
        try {
            clientSocket.receive(packetToReceive);
        } catch (IOException e) {
            e.printStackTrace(); // todo
        }
        if ((PacketProtocol.getFlag(packetToReceive.getData()) == (PacketProtocol.DOESALREADYEXIST + PacketProtocol.ACK)) || PacketProtocol.getFlag(packetToReceive.getData()) == (PacketProtocol.DOESNOTEXIST + PacketProtocol.ACK)) {
            String messageFromServer = new String(packetToReceive.getData(), PacketProtocol.HEADER_SIZE, (packetToReceive.getLength() - PacketProtocol.HEADER_SIZE));
            System.out.println(messageFromServer);
            return true;
        } else {
            return false;
        }
    }
}
