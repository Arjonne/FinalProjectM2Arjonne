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
     * Create a new client that contains a textual user interface for file transmission.
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
     * Stop the connection between the client and the server by closing the socket.
     */
    public void stopClient() {
        quit = true;
        clientSocket.close();
        System.out.println("Client has stopped, application is closed.");
    }

    /**
     * As long as the client thread is active (= as long as the client is connected to the server), first wait for input
     * from the user via the TUI, and then communicate accordingly with the server (via the clientHandler). Within this
     * loop, the total communication between server and client that is needed for executing a command is being handled.
     */
    @Override
    public void run() {
        quit = false;
        while (!quit) {
            while (!tryToReceive) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("Not able to sleep for a second due to interruption.");
                }
            }
            // send request to the server and try to receive an ACK (if ACK not received in time, resend packet):
            Acknowledgement.sendRequestAndReceiveAckWithMessage(clientSocket, getRequestPacket());
            // print the message from the server:
            byte[] acknowledgement = Acknowledgement.getAcknowledgement();
            String messageFromServer = new String(acknowledgement, PacketProtocol.HEADER_SIZE, (acknowledgement.length - PacketProtocol.HEADER_SIZE));
            System.out.println(messageFromServer.trim());
            // if the server responded with an acknowledgement, execute the command:
            if (PacketProtocol.getFlag(acknowledgement) == PacketProtocol.ACK) {
                // first, get some information from the packet that is received:
                int lastReceivedSeqNr = PacketProtocol.getSequenceNumber(acknowledgement);
                int lastReceivedAckNr = PacketProtocol.getAcknowledgementNumber(acknowledgement);
                int totalFileSize = PacketProtocol.getFileSizeInPacket(acknowledgement);
                // then, get the flag and name(s) of the file(s) that are sent in the initial request to the server be
                // able to correctly execute that command:
                int requestFlag = PacketProtocol.getFlag(getRequestPacket().getData());
                String fileName = getFileName();
                String oldFileName = getOldFileName();
                String newFileName = getNewFileName();
                // execute the steps that are needed per command:
                if (requestFlag == PacketProtocol.UPLOAD || requestFlag == PacketProtocol.REPLACE) {
                    byte[] dataOfFileToSend;
                    // create a byte representation from the (new) file that needs to be uploaded to the server:
                    if (requestFlag == PacketProtocol.UPLOAD) {
                        dataOfFileToSend = FileProtocol.fileToBytes(FileProtocol.CLIENT_FILEPATH, fileName);
                    } else {
                        dataOfFileToSend = FileProtocol.fileToBytes(FileProtocol.CLIENT_FILEPATH, newFileName);
                    }
                    // send the byte representation of the file to the server:
                    if (dataOfFileToSend != null) {
                        try {
                            StopAndWaitProtocol.sendFile(dataOfFileToSend, lastReceivedAckNr, lastReceivedSeqNr, clientSocket, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
                        } catch (UnknownHostException e) {
                            System.out.println("Check the destination address input (server address), as the destination could not be found.");
                        }
                        // calculate the checksum of the original file and send it to the server:
                        int checksumOfTotalFile = DataIntegrityCheck.calculateChecksum(dataOfFileToSend);
                        lastReceivedSeqNr = StopAndWaitProtocol.getLastReceivedSeqNr();
                        lastReceivedAckNr = StopAndWaitProtocol.getLastReceivedAckNr();
                        // create packet with checksum of total file in it, send it to the server and try to receive an ACK:
                        DatagramPacket checksumToSend = null;
                        try {
                            checksumToSend = DataIntegrityCheck.createChecksumPacket(checksumOfTotalFile, lastReceivedSeqNr, lastReceivedAckNr, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
                        } catch (UnknownHostException e) {
                            System.out.println("Check the destination address input (server address), as the destination could not be found.");
                        }
                        Acknowledgement.sendChecksumAndReceiveAck(clientSocket, checksumToSend);
                        // check if the file is correctly received by the server:
                        byte[] ackReceived = Acknowledgement.getAcknowledgement();
                        int flag = PacketProtocol.getFlag(ackReceived);
                        if (requestFlag == PacketProtocol.UPLOAD) {
                            if (flag == PacketProtocol.ACK) {
                                System.out.println(fileName + " is successfully uploaded to the server.");
                            } else {
                                System.out.println("The upload of " + fileName + " was not successful. Please, try again.");
                            }
                        } else {
                            if (flag == PacketProtocol.ACK) {
                                System.out.println("The server successfully replaced " + oldFileName + " by " + newFileName + ".");
                            } else {
                                System.out.println("The replacement of " + oldFileName + " by " + newFileName + " was not successful. Please, try again (but be aware that " + oldFileName + " does not exist on the server anymore!)");
                            }
                        }
                    }
                } else if (requestFlag == PacketProtocol.DOWNLOAD) {
                    // respond with an acknowledgement to the server, to let it know that download can start:
                    try {
                        Acknowledgement.sendAcknowledgement(0, lastReceivedSeqNr, lastReceivedAckNr, clientSocket, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
                    } catch (UnknownHostException e) {
                        System.out.println("Check the destination address input (server address), as the destination could not be found.");
                    }
                    // receive the file from the server:
                    StopAndWaitProtocol.receiveFile(clientSocket, totalFileSize);
                    File downloadedFile = FileProtocol.bytesToFile(FileProtocol.CLIENT_FILEPATH, fileName, StopAndWaitProtocol.getFileInBytes());
                    // receive the checksum of the original file from the server:
                    DatagramPacket packetWithChecksum = DataIntegrityCheck.receiveChecksum(clientSocket);
                    if (packetWithChecksum != null) {
                        int receivedFlag = PacketProtocol.getFlag(packetWithChecksum.getData());
                        // if you indeed received a packet with the CHECK flag, the checksum check can be performed:
                        if (receivedFlag == PacketProtocol.CHECK) {
                            // get information from the received packet:
                            lastReceivedSeqNr = PacketProtocol.getSequenceNumber(packetWithChecksum.getData());
                            lastReceivedAckNr = PacketProtocol.getAcknowledgementNumber(packetWithChecksum.getData());
                            int receivedChecksum = DataIntegrityCheck.getChecksum(packetWithChecksum);
                            // calculate the checksum of the received file:
                            byte[] receivedFileInBytes = StopAndWaitProtocol.getFileInBytes();
                            int checksumOfReceivedFile = DataIntegrityCheck.calculateChecksum(receivedFileInBytes);
                            if (DataIntegrityCheck.areChecksumOfTwoFilesTheSame(receivedChecksum, checksumOfReceivedFile)) {
                                // if the two checksums are the same, send an acknowledgement
                                System.out.println("The file is successfully downloaded.");
                                try {
                                    Acknowledgement.sendAcknowledgement(0, lastReceivedSeqNr, lastReceivedAckNr, clientSocket, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
                                } catch (UnknownHostException e) {
                                    System.out.println("Check the destination address input (server address), as the destination could not be found.");
                                }
                            }
                        } else {
                            // if the two checksums are not the same, a mistake has occurred during transmission. The
                            // downloaded file will be removed (as it is not the same as the original one) and an
                            // INCORRECT flag will be sent to the server.
                            if (downloadedFile != null && downloadedFile.delete()) {
                                System.out.println("The file that you downloaded is not the same as the original file on the server and is therefore not saved.");
                            }
                            try {
                                Acknowledgement.sendAcknowledgement(PacketProtocol.INCORRECT, lastReceivedSeqNr, lastReceivedAckNr, clientSocket, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
                            } catch (UnknownHostException e) {
                                System.out.println("Check the destination address input (server address), as the destination could not be found.");
                            }
                        }
                    }
                } else if (requestFlag == PacketProtocol.LIST) {
                    // respond with an acknowledgement to the server, to let it know that it can start sending the list:
                    try {
                        Acknowledgement.sendAcknowledgement(0, lastReceivedSeqNr, lastReceivedAckNr, clientSocket, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
                    } catch (UnknownHostException e) {
                        System.out.println("Check the destination address input (server address), as the destination could not be found.");
                    }
                    // receive the list from the server:
                    StopAndWaitProtocol.receiveFile(clientSocket, totalFileSize);
                    byte[] receivedList = StopAndWaitProtocol.getFileInBytes();
                    // show the list:
                    String listOfFiles = new String(receivedList);
                    System.out.println(listOfFiles);
                } else if (requestFlag == PacketProtocol.CLOSE) {
                    stopClient();
                    break;
                }
            }
            // wait for a new command by the user:
            System.out.println("Give the command you want to execute next:");
            tryToReceive = false;
        }
    }


//          --- METHODS USED IN SWITCH OF CLIENT-TUI ---

    /**
     * Send the initial request to the server.
     *
     * @param fileNameFromRequest is the name of the file (from the request of the user) with which the server needs
     *                            to do something.
     * @param flag                represents the command that the client wants to execute.
     */
    public void sendRequest(String fileNameFromRequest, int flag, int fileSize) {
        byte[] fileData = fileNameFromRequest.getBytes();
        // as this is the first message from the client to the server, the sequence number can be randomly generated:
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        // create the request packet and try to send it to the server. Furthermore, activate the run() to receive the
        // response to the request by the server and execute the command of the user:
        byte[] request = PacketProtocol.createPacketWithHeader(fileSize, sequenceNumber, 0, flag, fileData);
        try {
            DatagramPacket requestPacket = new DatagramPacket(request, request.length, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
            setRequestPacket(requestPacket);
            setFileName(fileNameFromRequest);
            activateTryToReceive();
        } catch (IOException e) {
            System.out.println("Check the destination address input (server address), as the destination could not be found.");
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
        // as this is the first message from the client to the server, the sequence number can be randomly generated:
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        // create the request packet and try to send it to the server. Furthermore, activate the run() to receive the
        // response to the request by the server and execute the command of the user:
        byte[] request = PacketProtocol.createPacketWithHeader(fileSize, sequenceNumber, 0, PacketProtocol.REPLACE, fileData);
        try {
            DatagramPacket requestPacket = new DatagramPacket(request, request.length, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
            setRequestPacket(requestPacket);
            setOldFileName(oldFileNameFromRequest);
            setNewFileName(newFileNameFromRequest);
            activateTryToReceive();
        } catch (IOException e) {
            System.out.println("Check the destination address input (server address), as the destination could not be found.");
        }
    }

    /**
     * Send list request to server (flag is internally set and no file name(s) are needed here).
     */
    public void sendListOrCloseRequest(int flag) {
        // as this is the first message from the client to the server, the sequence number can be randomly generated:
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        // create the request packet and try to send it to the server. Furthermore, activate the run() to receive the
        // response to the request by the server and execute the command of the user:
        byte[] request = PacketProtocol.createHeader(0, sequenceNumber, 0, flag, 0);
        try {
            DatagramPacket requestPacket = new DatagramPacket(request, request.length, InetAddress.getByName(PacketProtocol.PI_ADDRESS), PacketProtocol.PI_PORT);
            setRequestPacket(requestPacket);
            activateTryToReceive();
        } catch (IOException e) {
            System.out.println("Check the destination address input (server address), as the destination could not be found.");
        }
    }


//          --- GETTERS AND SETTERS ---

    /**
     * Set the boolean sendToServer to true when user has given input via TUI that needs to be communicated to the
     * server.
     */
    public void activateTryToReceive() {
        tryToReceive = true;
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
     * Get the filename from the request.
     *
     * @return the filename.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Set the filename in order to be able to access it in other places.
     *
     * @param fileName is the filename that needs to be stored.
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Get the name of the old file (the file that should be replaced) from the request.
     *
     * @return the name of the old file.
     */
    public String getOldFileName() {
        return oldFileName;
    }

    /**
     * Set the name of the old file (the file that should be replaced) in order to be able to access it in other places.
     *
     * @param oldFileName is the filename that needs to be stored.
     */
    public void setOldFileName(String oldFileName) {
        this.oldFileName = oldFileName;
    }

    /**
     * Get the name of the new file (the file that should be replacing the old file) from the request.
     *
     * @return the name of the new file.
     */
    public String getNewFileName() {
        return newFileName;
    }

    /**
     * Set the name of the old file (the file that should be replacing the old file) in order to be able to access it in
     * other places.
     *
     * @param newFileName is the filename that needs to be stored.
     */
    public void setNewFileName(String newFileName) {
        this.newFileName = newFileName;
    }
}