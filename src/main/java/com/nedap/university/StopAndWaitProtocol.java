package com.nedap.university;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Represents the protocol for sending and receiving packets according to the Stop and Wait ARQ protocol.
 */
public class StopAndWaitProtocol {
    public static byte[] completeFileInBytes;
    public static int lastReceivedSeqNr;
    public static int lastReceivedAckNr;

    /**
     * Send packets with file data and wait for acknowledgement to be received.
     *
     * @param fileInBytes       is the byte representation of the total file.
     * @param lastReceivedSeqNr is the last received sequence number.
     * @param lastReceivedAckNr is the last received acknowledgement number.
     * @param socket            is the socket via which the server and client are connected.
     * @param address           is the address to which the packet(s) need to be sent.
     * @param port              is the port to which the packet(s) need to be sent.
     */
    public static void sendFile(byte[] fileInBytes, int lastReceivedSeqNr, int lastReceivedAckNr, DatagramSocket socket, InetAddress address, int port) {
        // start with creating variables that are updated during the file transmission:
        boolean finished = false;
        int totalNumberOfPackets = (fileInBytes.length / (PacketProtocol.MAX_PACKET_SIZE - PacketProtocol.HEADER_SIZE)+1); // add 1 as currentPacketNumber also starts at 1.
        int currentPacketNumber = 1;
        int filePointerSender = 0;
        int sequenceNumber = lastReceivedAckNr + 1;
        int acknowledgementNumber = lastReceivedSeqNr;
        int flag;
        // set variables in the StatisticsProtocol file in order to measure some statistics on file transmission:
        StatisticsProtocol.setOptimalNumberOfPackets(totalNumberOfPackets+1); // add one as the total number of packets is rounded down, and division by 0 is not possible so packet count should also start at 1.
        StatisticsProtocol.startTimer();
        StatisticsProtocol.resetPacketCount();
        while (!finished) {
            StatisticsProtocol.addPacket();
            // as long as the last packet of the file is not sent, the flag MOREFRAGMENTS will be sent as a sign for the
            // receiver that more fragments will follow. Otherwise, the flag LAST will be sent.
            if (currentPacketNumber != totalNumberOfPackets) {
                flag = PacketProtocol.MOREFRAGMENTS;
            } else {
                flag = PacketProtocol.LAST;
            }
            // create a packet with file data:
            int dataLenghtInPacket = Math.min((PacketProtocol.MAX_PACKET_SIZE - PacketProtocol.HEADER_SIZE), (fileInBytes.length - filePointerSender));
            byte[] dataToSend = new byte[dataLenghtInPacket];
            // copy data of the total file into a smaller packet:
            System.arraycopy(fileInBytes, filePointerSender, dataToSend, 0, dataLenghtInPacket);
            byte[] dataWithHeader = PacketProtocol.createPacketWithHeader(fileInBytes.length, sequenceNumber, acknowledgementNumber, flag, dataToSend);
            DatagramPacket packetToSend = new DatagramPacket(dataWithHeader, dataWithHeader.length, address, port);
            DatagramPacket ackToReceive = AcknowledgementProtocol.createAckPacketToReceive();
            // try to send the packet and receive an acknowledgement before the timer expires. If acknowledgement is not
            // received in time, the packet will be sent again.
            AcknowledgementProtocol.sendPacketAndReceiveAck(socket, ackToReceive, packetToSend);
            byte[] acknowledgement = ackToReceive.getData();
            // if you did receive an acknowledgement and did not receive the same acknowledgement twice, change
            // variables to be able to send a new packet with the next file data.
            if ((PacketProtocol.getFlag(acknowledgement) == PacketProtocol.ACK) && PacketProtocol.getAcknowledgementNumber(acknowledgement) != lastReceivedAckNr) {
                System.out.println("Sending progression: " + currentPacketNumber + "/" + totalNumberOfPackets + "% complete.");

             //   System.out.println("Sending progression: " + StatisticsProtocol.calculateProgress(currentPacketNumber, totalNumberOfPackets) + "% complete.");
                int lastReceivedSequenceNumber = PacketProtocol.getSequenceNumber(acknowledgement);
                setLastReceivedSeqNr(lastReceivedSequenceNumber);
                lastReceivedAckNr = PacketProtocol.getAcknowledgementNumber(acknowledgement);
                setLastReceivedAckNr(lastReceivedAckNr);
                if (flag == PacketProtocol.LAST) {
                    StatisticsProtocol.stopTimer();
                    System.out.println("Sending progression: 100% complete.");
                    System.out.println(StatisticsProtocol.statisticsInMessage());
                    finished = true;
                } else {
                    filePointerSender = filePointerSender + dataLenghtInPacket;
                    acknowledgementNumber = PacketProtocol.getSequenceNumber(acknowledgement);
                    currentPacketNumber++;
                    if (sequenceNumber == 0xffffffff) {
                        sequenceNumber = 0;
                    } else {
                        sequenceNumber++;
                    }
                }
            }
        }
    }

    /**
     * Receive packets with file data and send an acknowledgement as response.
     *
     * @param socket        is the socket via which the data can be sent and received.
     * @param totalFileSize is the total size of the file that needs to be received.
     */
    public static void receiveFile(DatagramSocket socket, int totalFileSize) {
        // start with creating variables that are updated during the file transmission:
        byte[] dataCompleteFile = new byte[totalFileSize];
        int lastSequenceNumberReceived = 0;
        int filePointerReceiver = 0;
        boolean stopReceiving = false;
        while (!stopReceiving) {
            try {
                // create a buffer of maximal or necessary size and receive a packet with (a part of the) data of the
                // file of interest:
                int fragmentSize = Math.min((dataCompleteFile.length - filePointerReceiver + PacketProtocol.HEADER_SIZE), PacketProtocol.MAX_PACKET_SIZE);
                byte[] receivedPacket = new byte[fragmentSize];
                DatagramPacket fileDataPacket = new DatagramPacket(receivedPacket, receivedPacket.length);
                socket.receive(fileDataPacket);
                byte[] dataOfReceivedPacket = fileDataPacket.getData();
                // check flags to see if file data is received; if not, don't execute any further actions but wait for
                // the next packet to arrive (as the ack to the request or ack with file size could be lost too):
                int receivedFlag = PacketProtocol.getFlag(dataOfReceivedPacket);
                if (receivedFlag != PacketProtocol.MOREFRAGMENTS && receivedFlag != PacketProtocol.LAST) {
                    continue;
                }
                // get address and port of the destination this packet came from (and where an acknowledgement needs to
                // be sent to):
                InetAddress inetAddress = fileDataPacket.getAddress();
                int port = fileDataPacket.getPort();
                int receivedSequenceNumber = PacketProtocol.getSequenceNumber(dataOfReceivedPacket);
                int receivedAckNumber = PacketProtocol.getAcknowledgementNumber(dataOfReceivedPacket);
                // only send an acknowledgement if the checksum is correct:
                if (DataIntegrityProtocol.isChecksumCorrect(dataOfReceivedPacket, (fragmentSize - PacketProtocol.HEADER_SIZE))) {
                    AcknowledgementProtocol.sendAcknowledgement(0, receivedSequenceNumber, receivedAckNumber, socket, inetAddress, port);
                    // check if you did not receive the same packet twice:
                    int sequenceNumber = receivedAckNumber + 1;
                    if (lastSequenceNumberReceived != sequenceNumber) {
                        System.out.println("Receiving progression: " + filePointerReceiver + "/" + dataCompleteFile.length + "% complete.");

                     //   System.out.println("Receiving progression: " + StatisticsProtocol.calculateProgress(filePointerReceiver, dataCompleteFile.length) + "% complete.");
                        // if new packet has arrived, add the new data in the byte array that stores all received data
                        // up until this point:
                        int dataLengthInPacket = (dataOfReceivedPacket.length - PacketProtocol.HEADER_SIZE);
                        System.arraycopy(dataOfReceivedPacket, PacketProtocol.HEADER_SIZE, dataCompleteFile, filePointerReceiver, dataLengthInPacket);
                        filePointerReceiver = filePointerReceiver + dataLengthInPacket;
                        lastSequenceNumberReceived = sequenceNumber;
                    }
                    if (receivedFlag == PacketProtocol.LAST) {
                        System.out.println("Receiving progression: 100% complete.");
                        // store the byte representation of the received file in order to be able to do hash code check if necessary.
                        setFileInBytes(dataCompleteFile);
                        // create file from dataOfReceivedPacket that is received:
                        stopReceiving = true;
                    }
                }
            } catch (IOException e) {
                System.out.println("Timer has expired, packet will be retransmitted."); // as timer is reset to infinite after receiving ack, this should never appear.
            }
        }
    }


//          --- GETTERS AND SETTERS ---

    /**
     * Set the value of the byte representation of the complete file in bytes.
     *
     * @param dataCompleteFile is the byte representation of the received file.
     */
    public static void setFileInBytes(byte[] dataCompleteFile) {
        completeFileInBytes = new byte[dataCompleteFile.length];
        System.arraycopy(dataCompleteFile, 0, completeFileInBytes, 0, dataCompleteFile.length);
    }

    /**
     * Get the byte representation of the received file.
     *
     * @return the byte representation of the received file.
     */
    public static byte[] getFileInBytes() {
        return completeFileInBytes;
    }

    /**
     * Get the last received sequence number.
     *
     * @return the last received sequence number.
     */
    public static int getLastReceivedSeqNr() {
        return lastReceivedSeqNr;
    }

    /**
     * Set the last received sequence number.
     *
     * @param lastReceivedSeqNr is the sequence number that is last received.
     */
    public static void setLastReceivedSeqNr(int lastReceivedSeqNr) {
        StopAndWaitProtocol.lastReceivedSeqNr = lastReceivedSeqNr;
    }

    /**
     * Get the last received acknowledgement number.
     *
     * @return the last received acknowledgement number.
     */
    public static int getLastReceivedAckNr() {
        return lastReceivedAckNr;
    }

    /**
     * Set the last received acknowledgement number.
     *
     * @param lastReceivedAckNr is acknowledgement number that is last received.
     */
    public static void setLastReceivedAckNr(int lastReceivedAckNr) {
        StopAndWaitProtocol.lastReceivedAckNr = lastReceivedAckNr;
    }
}
