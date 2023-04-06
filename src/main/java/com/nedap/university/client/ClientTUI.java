package com.nedap.university.client;

import java.util.Scanner;

/**
 * Represents the textual user interface of the client for interaction with the server.
 */
public class ClientTUI {
    private Client client;

    /**
     * Creates the clientTUI.
     */
    public ClientTUI() {
    }

    public static void main(String[] args) {
        ClientTUI clientTUI = new ClientTUI();
        clientTUI.start();
    }

    public void start() {
        // socket??
        boolean connected = true;
        while (connected) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Type what you want to do:");
            String input = scanner.nextLine();
            String[] split = input.split("\\s+");
            String command = split[0];
//            String param = null;
            switch (command) {
                case UPLOAD:
                    client.doUpload();
                    break;
                case DOWNLOAD:
                    client.doDownload();
                    break;
                case REMOVE:
                    client.doRemove();
                    break;
                case REPLACE:
                    client.doReplace();
                    break;
                case LIST:
                    client.doList();
                    break;
                default:
                    System.out.println("The input is not correct.");
                    client.doOptions();
                    break;
            }
        }
    }
}
