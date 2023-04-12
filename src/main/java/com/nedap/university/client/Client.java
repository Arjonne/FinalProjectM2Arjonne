package com.nedap.university.client;

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
    private byte[] requestPacket;
    private boolean quit;
    private boolean sendToServer;

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
        clientSocket.close();
        quit = true;
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
        System.out.println("Hello from Client");
        int destinationPort = PacketProtocol.PI_PORT;
        quit = false;
        while (!quit) {
            while (!sendToServer) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e); // todo
                }
            }
            try {
                InetAddress inetAddress = InetAddress.getByName("localhost");
//            InetAddress inetAddress = InetAddress.getByName(PacketProtocol.PI_ADDRESS);
                DatagramPacket packetToSend = new DatagramPacket(requestPacket, requestPacket.length, inetAddress, destinationPort);
                sendToServer = false;
                clientSocket.send(packetToSend);
                byte[] responsePacket = new byte[256];
                DatagramPacket packetToReceive = new DatagramPacket(responsePacket, responsePacket.length);
                clientSocket.receive(packetToReceive);
                String messageFromServer = new String(packetToReceive.getData(), PacketProtocol.HEADER_SIZE, (packetToReceive.getLength() - PacketProtocol.HEADER_SIZE));
                System.out.println(messageFromServer);
            } catch (IOException e) {
                e.printStackTrace(); // todo
            }
        }
        System.out.println("Connection is closed (Message from Client).");
    }

    /**
     * Set the boolean sendToServer to true when user has given input via TUI that needs to be communicated to the
     * server.
     */
    public void activateSendToServer() {
        sendToServer = true;
    }

    /**
     * Create file based on input from TUI.
     *
     * @param fileName is the filename that the user has typed in the TUI.
     * @return the data in the form of a byte array (which is needed for datagram packet) of the file.
     */
    public byte[] createFile(String fileName) {
        byte[] file = fileName.getBytes();
        return file;
    }

    /**
     * Upload a file from the client to the server on the PI.
     *
     * @param fileName is the file to be uploaded.
     */
    public void doUpload(String fileName) {
        byte[] fileData = createFile(fileName); // later aanpassen naar daadwerkelijke file ipv tekst.
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        requestPacket = PacketProtocol.createPacketWithHeader(sequenceNumber, 0, PacketProtocol.UPLOAD, fileData);
        // when buffer is created, boolean can be set to true --> loop will continue and data will actually be sent.
        activateSendToServer();
    }

    /**
     * Download a file from the server on the PI to the client.
     *
     * @param fileName is the file to be downloaded.
     */
    public void doDownload(String fileName) {
        byte[] fileData = createFile(fileName);
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        requestPacket = PacketProtocol.createPacketWithHeader(sequenceNumber, 0, PacketProtocol.DOWNLOAD, fileData);
        activateSendToServer();
    }

    /**
     * Remove a file from the server on the PI.
     *
     * @param fileName is the file to be removed.
     */
    public void doRemove(String fileName) {
        byte[] fileData = createFile(fileName);
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        requestPacket = PacketProtocol.createPacketWithHeader(sequenceNumber, 0, PacketProtocol.REMOVE, fileData);
        activateSendToServer();
    }

    /**
     * Replace a file on the server on the PI by a new file.
     *
     * @param oldFileName is the file to be replaced.
     * @param newFileName is the new file.
     */
    public void doReplace(String oldFileName, String newFileName) {
        byte[] fileData = createFile(oldFileName + " " + newFileName);
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        requestPacket = PacketProtocol.createPacketWithHeader(sequenceNumber, 0, PacketProtocol.REPLACE, fileData);
        activateSendToServer();
    }

    /**
     * Show a list of the files that are stored on the server on the PI.
     */
    public void doList() {
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        requestPacket = PacketProtocol.createHeader(0, sequenceNumber, 0, PacketProtocol.LIST);
        activateSendToServer();
    }

    /**
     * Show the options of the commands that can be used in the TUI.
     */
    public void showOptions() {
        System.out.println("   Commands:\n" +
                "          upload <file> ...................... upload <file> to server\n" +
                "          download <file> .................... download <file> from server\n" +
                "          remove <file> ...................... remove <file> from server\n" +
                "          replace <old file> <new file>  ..... replace <old file> by <new file> on server\n" +
                "          list ............................... list all files stored on server\n" +
                "          options ............................ show options (this menu)\n" +
                "          close .............................. close application"
        );
    }

    /**
     * Close the connection with the server.
     */
    public void doClose() {
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        requestPacket = PacketProtocol.createHeader(0, sequenceNumber, 0, PacketProtocol.CLOSE);
        activateSendToServer();
    }
}
