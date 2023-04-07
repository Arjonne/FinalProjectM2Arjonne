package com.nedap.university.client;

import java.util.Scanner;

/**
 * Represents the textual user interface of the client for interaction with the server.
 */
public class ClientTUI {
    private Client client;
    private final String UPLOAD = "UPLOAD";
    private final String DOWNLOAD = "DOWNLOAD";
    private final String REMOVE = "REMOVE";
    private final String REPLACE = "REPLACE";
    private final String LIST = "LIST";
    private final String OPTIONS = "OPTIONS";
    private final String CLOSE = "CLOSE";


    /**
     * Creates the clientTUI.
     */
    public ClientTUI() {
    }

    public static void main(String[] args) {
        ClientTUI clientTUI = new ClientTUI();
        clientTUI.start();
    }

    /**
     * Start the textual user interface. Ask for input and respond correctly to that input.
     */
    public void start() {
        boolean connected = true;
        while (connected) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Type what you want to do:");
            String input = scanner.nextLine().toUpperCase();
            String[] split = input.split("\\s+");
            String command = split[0];
            String fileName = null;
            String oldFileName = null;
            String newFileName = null;
            if (split.length == 2) {
                fileName = split[1];
            } else if (split.length > 2) {
                oldFileName = split[1];
                newFileName = split[2];
            }
            switch (command) {
                case UPLOAD:
                    client.doUpload(fileName);
                    break;
                case DOWNLOAD:
                    client.doDownload(fileName);
                    break;
                case REMOVE:
                    client.doRemove(fileName);
                    break;
                case REPLACE:
                    client.doReplace(oldFileName, newFileName);
                    break;
                case LIST:
                    client.doList();
                    break;
                case OPTIONS:
                    client.showOptions();
                    break;
                case CLOSE:
                    client.doClose();
                    System.out.println("Application is closing");
                    connected = false;
                    break;
                default:
                    System.out.println("The input is not correct. Use the following format:");
                    client.showOptions();
                    break;
            }
        }
    }
}
