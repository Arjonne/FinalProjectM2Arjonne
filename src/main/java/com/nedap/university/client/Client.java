package com.nedap.university.client;


import com.nedap.university.Protocol;

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
    private byte[] buffer;
    private boolean inputIsGiven;

    /**
     * Create a new client that uses the textual user interface for file transmission.
     *
     * @param clientTUI
     */
    public Client(ClientTUI clientTUI) {
        this.clientTUI = clientTUI;
    }

    public void startClient() {
        try {
            clientSocket = new DatagramSocket();
            Thread clientThread = new Thread(this);
            clientThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * When an object implementing interface {@code Runnable} is used
     * to create a thread, starting the thread causes the object's
     * {@code run} method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method {@code run} is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        System.out.println("test");
        int port = Protocol.CLIENT_PORT;
        while (!inputIsGiven) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        try {
//            InetAddress inetAddress = InetAddress.getByName("localhost");
            InetAddress inetAddress = InetAddress.getByName(Protocol.PI_ADDRESS);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, inetAddress, port);
            inputIsGiven = false;
            clientSocket.send(packet);
            clientSocket.receive(packet);
            String messageFromServer = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Server replied that client typed: " + messageFromServer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the boolean inputIsGiven to true.
     */
    public void setInputIsGiven() {
        inputIsGiven = true;
    }

    /**
     * Set buffer based on input from TUI.
     *
     * @param input is the message that the user has typed in the TUI.
     * @return the buffer with input data.
     */
    public byte[] setBuffer(String input) {
        buffer = input.getBytes();
        return buffer;
    }

    /**
     * Upload a file from the client to the server on the PI.
     *
     * @param fileName is the file to be uploaded.
     */
    public void doUpload(String fileName) {
        //todo
    }

    /**
     * Download a file from the server on the PI to the client.
     *
     * @param fileName is the file to be downloaded.
     */
    public void doDownload(String fileName) {
        //todo
    }

    /**
     * Remove a file from the server on the PI.
     *
     * @param fileName is the file to be removed.
     */
    public void doRemove(String fileName) {
        //todo
    }

    /**
     * Replace a file on the server on the PI by a new file.
     *
     * @param oldFileName is the file to be replaced.
     * @param newFileName is the new file.
     */
    public void doReplace(String oldFileName, String newFileName) {
        //todo
    }

    /**
     * Show a list of the files that are stored on the server on the PI.
     */
    public void doList() {
        //todo
    }

    /**
     * Show the options of the commands that can be used in the TUI.
     */
    public void showOptions() {
        System.out.println("\"   Commands:\n" +
                "          upload <file> ...................... upload <file> to server\n" +
                "          download <file> .................... download <file> from server\n" +
                "          remove <file> ...................... remove <file> from server\n" +
                "          replace <old file> <new file>  ..... replace <old file> by <new file> on server\n" +
                "          list ............................... list all files stored on server\n" +
                "          options ............................ show options (this menu)\n" +
                "          close .............................. close application\""
        );
    }

    /**
     * Close the TUI.
     */
    public void doClose() {
        //todo
    }
}
