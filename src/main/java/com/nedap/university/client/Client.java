package com.nedap.university.client;

import java.net.DatagramSocket;
import java.net.SocketException;

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

    public void doUpload(){
    }

    public void doDownload() {
    }

    public void doRemove() {
    }

    public void doReplace() {
    }

    public void doList() {
    }

    public void doOptions(){
    }

}
