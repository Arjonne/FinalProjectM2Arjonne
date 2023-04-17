package com.nedap.university.client;

import com.nedap.university.*;

import java.io.File;
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
    String fileName;
    String oldFileName;
    String newFileName;
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
            // send request and try to receive ACK (if not in time, resend packet):
            Acknowledgement.receiveAckWithMessage(clientSocket, getRequestPacket());
            byte[] acknowledgement = Acknowledgement.getAcknowledgement();
            String messageFromServer = new String(acknowledgement, PacketProtocol.HEADER_SIZE, (acknowledgement.length - PacketProtocol.HEADER_SIZE));
            System.out.println(messageFromServer.trim());
            if (PacketProtocol.getFlag(acknowledgement) == PacketProtocol.ACK) {
                int totalFileSize = PacketProtocol.getFileSizeInPacket(acknowledgement);
                int receivedSeqNr = PacketProtocol.getSequenceNumber(acknowledgement);
                int receivedAckNumber = PacketProtocol.getAcknowledgementNumber(acknowledgement);
                int requestFlag = PacketProtocol.getFlag(getRequestPacket().getData());
                String fileName = getFileName();
                String oldFileName = getOldFileName();
                String newFileName = getNewFileName();

                if (requestFlag == PacketProtocol.DOWNLOAD || requestFlag == PacketProtocol.LIST) {
                    try {
                        Acknowledgement.sendAcknowledgement(0, receivedSeqNr, receivedAckNumber, clientSocket, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e); // todo
                    }
                    StopAndWaitProtocol.receiveFile(clientSocket, totalFileSize);
                    if (requestFlag == PacketProtocol.DOWNLOAD) {
                        File downloadedFile = FileProtocol.bytesToFile(FileProtocol.CLIENT_FILEPATH, fileName, StopAndWaitProtocol.getFileInBytes());
                        System.out.println("Finally, receive HashCode");
//                            if (DataIntegrityCheck.receiveHashCode(clientSocket) && (DataIntegrityCheck.getFlag() == PacketProtocol.CHECK)) {
//                                System.out.println("Hashcode is received.");
//                                int originalHashCode = DataIntegrityCheck.getHashCode();
//                                int hashCodeOfReceivedFile = downloadedFile.hashCode();
//                                if (DataIntegrityCheck.areSentAndReceivedFilesTheSame(originalHashCode, hashCodeOfReceivedFile)) {
//                                    System.out.println("The file is successfully downloaded.");
//                                } else {
//                                    downloadedFile.delete();
//                                    System.out.println("The file that you downloaded is not the same as the original file on the server and is therefore not saved.");
//                                }
//                                receivedSeqNr = DataIntegrityCheck.getReceivedSeqNr();
//                                receivedAckNumber = DataIntegrityCheck.getReceivedAckNr();
//                                try {
//                                    Acknowledgement.sendAcknowledgement(0, receivedSeqNr, receivedAckNumber, clientSocket, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
//                                } catch (UnknownHostException e) {
//                                    throw new RuntimeException(e); // todo
//                                }
//                            }
                    } else {
                        byte[] receivedList = StopAndWaitProtocol.getFileInBytes();
                        String listOfFiles = new String(receivedList);
                        System.out.println(listOfFiles);
                    }
                } else if (requestFlag == PacketProtocol.UPLOAD || requestFlag == PacketProtocol.REPLACE) {
                    byte[] dataOfFileToSend;
                    if (requestFlag == PacketProtocol.UPLOAD) {
                        dataOfFileToSend = FileProtocol.fileToBytes(FileProtocol.CLIENT_FILEPATH, fileName);
                    } else {
                        dataOfFileToSend = FileProtocol.fileToBytes(FileProtocol.CLIENT_FILEPATH, newFileName);
                    }
                    try {
                        StopAndWaitProtocol.sendFile(dataOfFileToSend, receivedAckNumber, receivedSeqNr, clientSocket, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e); // todo
                    }
                    System.out.println("Then, send hash code of file");

//                        int receivedAckNumber = StopAndWaitProtocol.getLastReceivedAckNr();
//                        int receivedSeqNumber = StopAndWaitProtocol.getLastReceivedSeqNr();
//                        File fileSent = FileProtocol.getFile(FileProtocol.CLIENT_FILEPATH, fileName);
//                        int hashCode = fileSent.hashCode();
//                        try {
//                            DataIntegrityCheck.sendHashCode(hashCode, receivedSeqNumber, receivedAckNumber, clientSocket, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
//                        } catch (IOException e) {
//                            e.printStackTrace(); // todo
//                        }
//                        if (receiveAcknowledgement(clientSocket)) {
//                            int flag = PacketProtocol.getFlag(ackPacket);
//                            if (requestFlag == PacketProtocol.UPLOAD) {
//                                if (flag == PacketProtocol.ACK) {
//                                    System.out.println(fileName + " is successfully uploaded to the server.");
//                                } else {
//                                    System.out.println("The upload of " + fileName + " was not successful. Please, try again.");
//                                }
//                            } else {
//                                if (flag == PacketProtocol.ACK) {
//                                    System.out.println("The replacement of " + oldFileName + " by " + newFileName + " is successfully completed.");
//                                } else {
//                                    System.out.println("The replacement of " + oldFileName + " by " + newFileName + " was not successful. Please, try again (but be aware that " + oldFileName + " does not exist on the server anymore!)");
//                                }
//                            }
//                        }
                } else if (requestFlag == PacketProtocol.CLOSE) {
                    stopClient();
                    break;
                }
            }
            System.out.println("Give the command you want to execute next:");
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

    // METHODS USED IN SWITCH IN CLIENT-TUI:

    /**
     * Send request to server.
     *
     * @param fileNameFromRequest is the name of the file (from the request of the user) with which the server needs
     *                            to do something.
     * @param flag                represents the action the server needs to do.
     */
    public void sendRequest(String fileNameFromRequest, int flag, int fileSize) {
        byte[] fileData = fileNameFromRequest.getBytes();
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        byte[] request = PacketProtocol.createPacketWithHeader(fileSize, sequenceNumber, 0, flag, fileData);
        try {
            DatagramPacket requestPacket = new DatagramPacket(request, request.length, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
            setRequestPacket(requestPacket);
            setFileName(fileNameFromRequest);
            activateTryToReceive();
        } catch (IOException e) {
            e.printStackTrace(); //todo
        }
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
        byte[] fileData = (oldFileNameFromRequest + " " + newFileNameFromRequest).getBytes();
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        byte[] request = PacketProtocol.createPacketWithHeader(fileSize, sequenceNumber, 0, PacketProtocol.REPLACE, fileData);
        try {
            DatagramPacket requestPacket = new DatagramPacket(request, request.length, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
            setRequestPacket(requestPacket);
            setOldFileName(oldFileNameFromRequest);
            setNewFileName(newFileNameFromRequest);
            activateTryToReceive();
        } catch (IOException e) {
            e.printStackTrace(); //todo
        }
    }

    /**
     * Send list request to server (flag is internally set and no file name(s) are needed here).
     */
    public void sendListOrCloseRequest(int flag) {
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        byte[] request = PacketProtocol.createHeader(0, sequenceNumber, 0, flag);
        try {
            DatagramPacket requestPacket = new DatagramPacket(request, request.length, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
            setRequestPacket(requestPacket);
            activateTryToReceive();
        } catch (IOException e) {
            e.printStackTrace(); //todo
        }
    }

    // GETTERS AND SETTERS:

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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOldFileName() {
        return oldFileName;
    }

    public void setOldFileName(String oldFileName) {
        this.oldFileName = oldFileName;
    }

    public String getNewFileName() {
        return newFileName;
    }

    public void setNewFileName(String newFileName) {
        this.newFileName = newFileName;
    }
}
