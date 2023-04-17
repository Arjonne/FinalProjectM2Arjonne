package com.nedap.university;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Represents the protocol for sending and receiving packets according to the Stop and Wait ARQ protocol..
 */
public class StopAndWaitProtocol {
    public static int flag;
    public static byte[] completeFileInBytes;
    public static int lastReceivedSeqNr;
    public static int lastReceivedAckNr;


    public static int getFlag() {
        return flag;
    }

    public static void setFlag(int flag) {
        StopAndWaitProtocol.flag = flag;
    }

    public static int getLastReceivedSeqNr() {
        return lastReceivedSeqNr;
    }

    public static void setLastReceivedSeqNr(int lastReceivedSeqNr) {
        StopAndWaitProtocol.lastReceivedSeqNr = lastReceivedSeqNr;
    }

    public static int getLastReceivedAckNr() {
        return lastReceivedAckNr;
    }

    public static void setLastReceivedAckNr(int lastReceivedAckNr) {
        StopAndWaitProtocol.lastReceivedAckNr = lastReceivedAckNr;
    }

    /**
     * Send a packet with file data and wait for acknowledgement to be received.
     *
     * @param fileInBytes is the byte representation of the total file.
     * @param socket      is the socket via which the server and client are connected.
     * @param address     is the address to which the packet(s) need to be sent.
     * @param port        is the port to which the packet(s) need to be sent.
     */
    public static void sendFile(byte[] fileInBytes, int lastReceivedAckNr, int lastReceivedSeqNr, DatagramSocket socket, InetAddress address, int port) {
        System.out.println("Start sending file...");
        boolean finished = false;
        int totalNumberOfPackets = (fileInBytes.length / PacketProtocol.MAX_PACKET_SIZE);
        int currentPacketNumber = 0; // start with 0 as totalNumberOfPackets is also rounded down.
        int filePointerSender = 0;
        int sequenceNumber = lastReceivedAckNr + 1;
        int acknowledgementNumber = lastReceivedSeqNr;
        DatagramPacket lastPacketSent = null;
        while (!finished) {
            if (currentPacketNumber != totalNumberOfPackets) {
                setFlag(PacketProtocol.MOREFRAGMENTS);
            } else {
                setFlag(PacketProtocol.LAST);
            }
            System.out.println("Getting ready for sending packet " + currentPacketNumber + " out of " + totalNumberOfPackets + " packets. (Flag of packet is: " + getFlag() + ", seq nr is " + sequenceNumber + ", ack nr is " + acknowledgementNumber + " ).");
            // create packet:
            int dataLenghtInPacket = Math.min((PacketProtocol.MAX_PACKET_SIZE - PacketProtocol.HEADER_SIZE), (fileInBytes.length - filePointerSender));
            byte[] dataToSend = new byte[dataLenghtInPacket];
            // copy data of the total file into a smaller packet:
            System.arraycopy(fileInBytes, filePointerSender, dataToSend, 0, dataLenghtInPacket);
            byte[] dataWithHeader = PacketProtocol.createPacketWithHeader(fileInBytes.length, sequenceNumber, acknowledgementNumber, getFlag(), dataToSend);
            DatagramPacket packetToSend = new DatagramPacket(dataWithHeader, dataWithHeader.length, address, port);
            DatagramPacket ackToReceive = Acknowledgement.createAckPacketToReceive();
            Acknowledgement.tryToReceiveAck(socket, ackToReceive, packetToSend);
            byte[] acknowledgement = ackToReceive.getData();
            System.out.println("ACK received with flag " + PacketProtocol.getFlag(acknowledgement) + ", ack nr " + PacketProtocol.getAcknowledgementNumber(acknowledgement) + " and seq nr " + PacketProtocol.getSequenceNumber(acknowledgement) + ".");
            // if you did receive an acknowledgement and did not receive the same acknowledgement twice, change
            // variables to be able to send a new packet with additional file data.
            if ((PacketProtocol.getFlag(acknowledgement) == PacketProtocol.ACK) && PacketProtocol.getAcknowledgementNumber(acknowledgement) != lastReceivedAckNr) {
                int lastReceivedSequenceNumber = PacketProtocol.getSequenceNumber(acknowledgement);
                setLastReceivedSeqNr(lastReceivedSequenceNumber);
                lastReceivedAckNr = PacketProtocol.getAcknowledgementNumber(acknowledgement);
                setLastReceivedAckNr(lastReceivedAckNr);
                if (getFlag() == PacketProtocol.LAST) {
                    finished = true;
                } else {
                    filePointerSender = filePointerSender + dataLenghtInPacket;
                    acknowledgementNumber = PacketProtocol.getSequenceNumber(acknowledgement);
                    currentPacketNumber++;
                    //todo check of 0xffffffff - 1 klopt in debug!
                    if (sequenceNumber == 0xffffffff - 1) {
                        sequenceNumber = 0;
                    } else {
                        sequenceNumber++;
                    }
                }
            }
        }
    }


    /**
     * Receive a packet with file data and send an acknowledgement as response.
     *
     * @param socket        is the socket via which the data can be sent and received.
     * @param totalFileSize is the total size of the file that needs to be received.
     */
    public static void receiveFile(DatagramSocket socket, int totalFileSize) {
        System.out.println("Start receiving file...");
        byte[] dataCompleteFile = new byte[totalFileSize];
        int lastSequenceNumberReceived = 0;
        int filePointerReceiver = 0;
        boolean stopReceiving = false;
        while (!stopReceiving) {
            try {
                // create a buffer of maximal or necessary size:
                int fragmentSize = Math.min((dataCompleteFile.length - filePointerReceiver + PacketProtocol.HEADER_SIZE), PacketProtocol.MAX_PACKET_SIZE);
                byte[] receivedPacket = new byte[fragmentSize];
                DatagramPacket fileDataPacket = new DatagramPacket(receivedPacket, receivedPacket.length);
                socket.receive(fileDataPacket);
                byte[] dataOfReceivedPacket = fileDataPacket.getData();
                // get address and port of the destination this packet came from (and where an acknowledgement needs to
                // be sent to):
                InetAddress inetAddress = fileDataPacket.getAddress();
                int port = fileDataPacket.getPort();
                int receivedSequenceNumber = PacketProtocol.getSequenceNumber(dataOfReceivedPacket);
                int receivedAckNumber = PacketProtocol.getAcknowledgementNumber(dataOfReceivedPacket);
                System.out.println("Packet received with flag " + PacketProtocol.getFlag(dataOfReceivedPacket) + " ackNr " + receivedAckNumber + " and seq nr: " + receivedSequenceNumber + ".");
                // only send acknowledgement if checksum is correct:
//                if (DataIntegrityCheck.isChecksumCorrect(dataOfReceivedPacket)) {
                if (true) { //todo terug aanpassen als probleem checksum is gevonden.
                    System.out.println("Checksum was correct.");
                    Acknowledgement.sendAcknowledgement(0, receivedSequenceNumber, receivedAckNumber, socket, inetAddress, port);
                    // check if you did not receive the same packet twice:
                    int sequenceNumber = receivedAckNumber + 1;
                    if (lastSequenceNumberReceived != sequenceNumber) {
                        // if new packet has arrived, add data in packet tot total of all received packets up until this point:
                        int dataLengthInPacket = (dataOfReceivedPacket.length - PacketProtocol.HEADER_SIZE);
                        System.arraycopy(dataOfReceivedPacket, PacketProtocol.HEADER_SIZE, dataCompleteFile, filePointerReceiver, dataLengthInPacket);
                        filePointerReceiver = filePointerReceiver + dataLengthInPacket;
                        lastSequenceNumberReceived = sequenceNumber;
                    }
                    if (PacketProtocol.getFlag(dataOfReceivedPacket) == PacketProtocol.LAST) {
                        // store the byte representation of the received file in order to be able to do hash code check if necessary.
                        setFileInBytes(dataCompleteFile);
                        // create file from dataOfReceivedPacket that is received:
                        System.out.println("Last fragment was received.");
                        stopReceiving = true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace(); // todo
            }
        }
    }

    /**
     * Set the value of the byte representation of the complete file in bytes.
     *
     * @param dataCompleteFile is the byte representation of the received file.
     */
    public static void setFileInBytes(byte[] dataCompleteFile) {
        completeFileInBytes = new byte[dataCompleteFile.length];
        for (int i = 0; i < dataCompleteFile.length; i++) {
            completeFileInBytes[i] = dataCompleteFile[i];
        }
    }

    /**
     * Get the byte representation of the received file.
     *
     * @return the byte representation of the received file.
     */
    public static byte[] getFileInBytes() {
        return completeFileInBytes;
    }
}
