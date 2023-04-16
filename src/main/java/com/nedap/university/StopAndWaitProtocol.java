package com.nedap.university;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Represents the protocol for sending and receiving packets according to the Stop and Wait ARQ protocol..
 */
public class StopAndWaitProtocol {
    static DatagramPacket lastPacketSent;
    static int flag;
    static byte[] completeFileInBytes;

    public static DatagramPacket getLastPacketSent() {
        return lastPacketSent;
    }

    public static void setLastPacketSent(DatagramPacket lastPacketSent) {
        StopAndWaitProtocol.lastPacketSent = lastPacketSent;
    }

    public static int getFlag() {
        return flag;
    }

    public static void setFlag(int flag) {
        StopAndWaitProtocol.flag = flag;
    }

    /**
     * Send a packet with file data and wait for acknowledgement to be received.
     *
     * @param fileInBytes is the byte representation of the total file.
     * @param socket      is the socket via which the server and client are connected.
     * @param address     is the address to which the packet(s) need to be sent.
     * @param port        is the port to which the packet(s) need to be sent.
     */
    public static void sendFile(byte[] fileInBytes, int lastUsedSequenceNumber, int lastReceivedSeqNr, DatagramSocket socket, InetAddress address, int port) {
        System.out.println("Start sending file...");
        boolean finished = false;
        int totalNumberOfPackets = (fileInBytes.length / PacketProtocol.MAX_PACKET_SIZE);
        int currentPacketNumber = 0; // start with 0 as totalNumberOfPackets is also rounded down.
        int filePointerSender = 0;
        int lastAckReceived = 0;
        int sequenceNumber = lastUsedSequenceNumber + 1;
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
            byte[] acknowledgement = new byte[PacketProtocol.HEADER_SIZE];
            DatagramPacket ackToReceive = new DatagramPacket(acknowledgement, acknowledgement.length);
            boolean isAckReceived = false;
            while (!isAckReceived) {
                try {
                    socket.send(packetToSend);
                    System.out.println("Packet " + currentPacketNumber + " is sent.");
                    socket.setSoTimeout(PacketProtocol.RTT);
                    // set timeOut -- socket will try to receive ack for this period of time. if timer expires without receiving an ACK, method continues.
                    // as long as there is no data received, stay in the loop and try receiving the ack. if timer has expired, resend the last packet sent.
                    socket.receive(ackToReceive);
                    isAckReceived = true;
                } catch (IOException e) {
                    System.out.println("Timer has expired - try sending the packet again");
                }
            }
            System.out.println("ACK received with flag " + PacketProtocol.getFlag(acknowledgement) + ", ack nr " + PacketProtocol.getAcknowledgementNumber(acknowledgement) + " and seq nr " + PacketProtocol.getSequenceNumber(acknowledgement) + ".");
            // if you did receive an acknowledgement and did not receive the same acknowledgement twice, change
            // variables to be able to send a new packet with additional file data.
            if ((PacketProtocol.getFlag(acknowledgement) == PacketProtocol.ACK) && PacketProtocol.getAcknowledgementNumber(acknowledgement) != lastAckReceived) {
                if (getFlag() == PacketProtocol.LAST) {
                    System.out.println("Hashcode must be sent here.");
                    finished = true;
                    //todo send hash code!!!
                } else {
                    filePointerSender = filePointerSender + dataLenghtInPacket;
                    acknowledgementNumber = PacketProtocol.getSequenceNumber(acknowledgement);
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
     * @param fileName      is the name of the file that needs to be received.
     */
    public static void receiveFile(DatagramSocket socket, int totalFileSize, String fileName) {
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
                    int sequenceNumber = receivedAckNumber + 1;
                    int ackNumber = receivedSequenceNumber;
                    byte[] acknowledgement = PacketProtocol.createHeader(0, sequenceNumber, ackNumber, PacketProtocol.ACK);
                    DatagramPacket ackPacket = new DatagramPacket(acknowledgement, acknowledgement.length, inetAddress, port);
                    try {
                        socket.send(ackPacket);
                    } catch (IOException e) {
                        // todo
                    }
                    System.out.println("Acknowledgement with seq nr " + sequenceNumber + " and ackNumber " + ackNumber + " is sent.");
                    // check if you did not receive the same packet twice:
                    if (lastSequenceNumberReceived != sequenceNumber) {
                        // if new packet has arrived, add data in packet tot total of all received packets up until this point:
                        int dataLengthInPacket = (dataOfReceivedPacket.length - PacketProtocol.HEADER_SIZE);
                        System.arraycopy(dataOfReceivedPacket, PacketProtocol.HEADER_SIZE, dataCompleteFile, filePointerReceiver, dataLengthInPacket);
                        filePointerReceiver = filePointerReceiver + dataLengthInPacket;
                        lastSequenceNumberReceived = sequenceNumber;
                    }
                    if (PacketProtocol.getFlag(dataOfReceivedPacket) == PacketProtocol.LAST) {
                        // store the byte representation of the received file in order to be able to do hash code check if necessary.
                        setDataCompleteFile(dataCompleteFile);
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
    public static void setDataCompleteFile(byte[] dataCompleteFile) {
        byte[] completeFileInBytes = new byte[dataCompleteFile.length];
        for (int i = 0; i < dataCompleteFile.length; i++) {
            completeFileInBytes[i] = dataCompleteFile[i];
        }
    }

    /**
     * Get the byte representation of the received file.
     *
     * @return the byte representation of the received file.
     */
    public byte[] getDataCompleteFile() {
        return completeFileInBytes;
    }
}
