package com.nedap.university.client;

import com.nedap.university.FileProtocol;
import com.nedap.university.PacketProtocol;

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

    /**
     * Create and start the textual user interface.
     */
    public static void main(String[] args) {
        ClientTUI clientTUI = new ClientTUI();
        clientTUI.start();
    }

    /**
     * Start the textual user interface. Ask for input and respond correctly to that input.
     */
    public void start() {
        client = new Client(this);
        if (client.startClient()) {
            System.out.println("Client started successfully. \nType OPTIONS to get an overview of the commands.");
        }
        boolean close = false;
        while (!close) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Type your command: ");
            String input = scanner.nextLine();
            String[] split = input.split("\\s+");
            String command = split[0].toUpperCase();
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
                    client.sendRequest(fileName, PacketProtocol.UPLOAD);
                    break;
                case DOWNLOAD:
                    if (!FileProtocol.checkIfFileExists(fileName, FileProtocol.createFilePath(PacketProtocol.CLIENT_FILEPATH))) {
                        client.sendRequest(fileName, PacketProtocol.DOWNLOAD);
                    } else {
                        System.out.println("The file " + fileName + " already exists in your local folder " + PacketProtocol.CLIENT_FILEPATH + ", first remove this file if you want to download it again.");
                    }
                    break;
                case REMOVE:
                    client.sendRequest(fileName, PacketProtocol.REMOVE);
                    break;
                case REPLACE:
                    client.sendReplaceRequest(oldFileName, newFileName);
                    break;
                case LIST:
                    client.sendListOrCloseRequest(PacketProtocol.LIST);
                    break;
                case OPTIONS:
                    showOptions();
                    break;
                case CLOSE:
                    client.sendListOrCloseRequest(PacketProtocol.CLOSE);
                    System.out.println("Application is closing");
                    close = true;
                    break;
                default:
                    System.out.println("The input is not correct. Use the following format:");
                    showOptions();
                    break;
            }
        }
        System.out.println("out of the while loop ");
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
}