package com.nedap.university;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;

/**
 * Represents the protocol for sending and receiving packets according to the Stop and Wait ARQ protocol..
 */
public class StopAndWaitProtocol {
    static DatagramPacket lastPacketSent;
    static int flag;

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
     * @param socket is the socket via which the server and client are connected.
     * @param address is the address to which the packet(s) need to be sent.
     * @param port is the port to which the packet(s) need to be sent.
     */
    public static void sendFile(byte[] fileInBytes, int lastUsedSequenceNumber, int lastReceivedSeqNr, DatagramSocket socket, InetAddress address, int port) {
        System.out.println("Start sending file...");
        boolean finished = false;
        int totalNumberOfPackets = (fileInBytes.length / PacketProtocol.MAX_PACKET_SIZE);
        int currentPacketNumber = 1;
        int filePointerSender = 0;
        int lastAckReceived = 0;
        int sequenceNumber = lastUsedSequenceNumber + 1;
        int acknowledgementNumber = lastReceivedSeqNr;
        PacketTimer timer = new PacketTimer(new Timer());
        DatagramPacket lastPacketSent = null;
        while (!finished) {
            if (currentPacketNumber != totalNumberOfPackets) {
                setFlag(PacketProtocol.MOREFRAGMENTS);
            } else {
                setFlag(PacketProtocol.LAST);
            }
            System.out.println("Sending packet " + currentPacketNumber + " out of " + totalNumberOfPackets + " packets.");
            int dataLenghtInPacket = Math.min((PacketProtocol.MAX_PACKET_SIZE - PacketProtocol.HEADER_SIZE), (fileInBytes.length - filePointerSender));
            byte[] dataToSend = new byte[dataLenghtInPacket];
            // copy data of the total file into a smaller packet:
            System.arraycopy(fileInBytes, filePointerSender, dataToSend, 0, dataLenghtInPacket);
            byte[] dataWithHeader = PacketProtocol.createPacketWithHeader(fileInBytes.length, sequenceNumber, acknowledgementNumber, getFlag(), dataToSend);
            DatagramPacket packetToSend = new DatagramPacket(dataWithHeader, dataWithHeader.length, address, port);
            setLastPacketSent(packetToSend);
            try {
                socket.send(packetToSend);
                System.out.println("Packet " + currentPacketNumber + " is sent.");
                timer.startTimer(socket, getLastPacketSent());
                byte[] acknowledgement = new byte[PacketProtocol.HEADER_SIZE];
                DatagramPacket ackToReceive = new DatagramPacket(acknowledgement, acknowledgement.length);
                socket.receive(ackToReceive);
                // if ack is received and timer is not expired, stop the timer again:
                timer.stopTimer();
                // if you did receive an acknowledgement and did not receive the same acknowledgement twice, change
                // variables to be able to send a new packet with additional file data.
                if ((PacketProtocol.getFlag(acknowledgement) == PacketProtocol.ACK) && PacketProtocol.getAcknowledgementNumber(ackToReceive.getData()) != lastAckReceived) {
                    if (getFlag() == PacketProtocol.LAST) {
                        finished = true;
                    } else {
                        filePointerSender = filePointerSender + dataLenghtInPacket;
                        if (sequenceNumber == 0xffffffff - 1) {
                            acknowledgementNumber = sequenceNumber;
                            sequenceNumber = 0;
                        } else {
                            acknowledgementNumber = sequenceNumber;
                            sequenceNumber++;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace(); // todo
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
                // create a buffer of maximal size to be able to receive as much data per packet as possible:
                byte[] receivedPacket = new byte[PacketProtocol.MAX_PACKET_SIZE];
                DatagramPacket fileDataPacket = new DatagramPacket(receivedPacket, receivedPacket.length);
                socket.receive(fileDataPacket);
                byte[] data = fileDataPacket.getData();
                // get address and port of the destination this packet came from (and where an acknowledgement needs to
                // be sent to):
                InetAddress inetAddress = fileDataPacket.getAddress(); // todo checken of adres en poort kloppen.
                int port = fileDataPacket.getPort();
                int receivedSequenceNumber = PacketProtocol.getSequenceNumber(data);
                int receivedAckNumber = PacketProtocol.getAcknowledgementNumber(data);
                // only send acknowledgement if checksum is correct:
                if (DataIntegrityCheck.isChecksumCorrect(data)) {
                    System.out.println("Packet with sequence number " + receivedSequenceNumber + " successfully and correctly received.");
                    int sequenceNumber = receivedAckNumber + 1;
                    int ackNumber = receivedSequenceNumber;
                    byte[] acknowledgement = PacketProtocol.createHeader(0, sequenceNumber, ackNumber, PacketProtocol.ACK);
                    DatagramPacket acknowledgementPacket = new DatagramPacket(acknowledgement, acknowledgement.length, inetAddress, port);
                    socket.send(acknowledgementPacket);
                    System.out.println("Acknowledgement with number " + ackNumber + " is sent.");
                    // check if you did not receive the same packet twice:
                    if (lastSequenceNumberReceived != sequenceNumber) {
                        int dataLengthInPacket = (data.length - PacketProtocol.HEADER_SIZE);
                        System.arraycopy(data, PacketProtocol.HEADER_SIZE, dataCompleteFile, filePointerReceiver, dataLengthInPacket);
                        filePointerReceiver = filePointerReceiver + dataLengthInPacket;
                        lastSequenceNumberReceived = sequenceNumber;
                    }
                    if (PacketProtocol.getFlag(data) == PacketProtocol.LAST) {
                        // create file from data that is received:
                        File receivedFile = FileProtocol.bytesToFile(fileName, dataCompleteFile);

                        // check if received file and original file are the same:
                        int hashCodeOfFile = receivedFile.hashCode();
                        byte[] receiveHashcodeForCheck = new byte[PacketProtocol.MAX_PACKET_SIZE]; // todo check how large hashcode is.
                        DatagramPacket receiveHashCode = new DatagramPacket(receiveHashcodeForCheck, receiveHashcodeForCheck.length);
                        socket.receive(receiveHashCode);
                        byte[] hashCode = receiveHashCode.getData();
                        receivedSequenceNumber = PacketProtocol.getSequenceNumber(hashCode);
                        receivedAckNumber = PacketProtocol.getAcknowledgementNumber(hashCode);
                        sequenceNumber = receivedAckNumber + 1;
                        ackNumber = receivedSequenceNumber;
                        if (DataIntegrityCheck.isChecksumCorrect(hashCode)) {
                            String hashCodeInString = hashCode.toString();
                            int originalHashCode = Integer.getInteger(hashCodeInString);
                            if (DataIntegrityCheck.areSentAndReceivedFilesTheSame(originalHashCode, hashCodeOfFile)) {
                                byte[] finalAcknowledgement = PacketProtocol.createHeader(0, sequenceNumber, ackNumber, (PacketProtocol.ACK + PacketProtocol.LAST));
                                DatagramPacket lastAcknowledgementPacket = new DatagramPacket(finalAcknowledgement, finalAcknowledgement.length, inetAddress, port);
                                socket.send(lastAcknowledgementPacket);
                                stopReceiving = true;
                            } else {
                                byte[] incorrectHashCode = PacketProtocol.createHeader(0, sequenceNumber, ackNumber, PacketProtocol.INCORRECT);
                                DatagramPacket hashCodeIncorrect = new DatagramPacket(incorrectHashCode, incorrectHashCode.length, inetAddress, port);
                                socket.send(hashCodeIncorrect);
                                System.out.println("Hash code is not correct.");
                                //todo receive total file again?
                            }
                        }
                    }
//                } else {
//                    try {
//                        Thread.sleep(10);
//                    } catch (InterruptedException e) {
//                        stopReceiving = true;
//                    }
                }
            } catch (IOException e) {
                e.printStackTrace(); // todo
            }
        }
    }
}
