package com.nedap.university.client;


/**
 * Represents the client for the file transfer.
 */
public class Client {
    private ClientTUI clientTUI;

    /**
     * Create a new client that uses the textual user interface for file transmission.
     *
     * @param clientTUI
     */
    public Client(ClientTUI clientTUI) {
        this.clientTUI = clientTUI;
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
