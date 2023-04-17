package com.nedap.university;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public final class Acknowledgement {
    static byte[] acknowledgement;

    public static DatagramPacket createAckPacketToReceive() {
        byte[] acknowledgement = new byte[PacketProtocol.HEADER_SIZE];
        DatagramPacket ackToReceive = new DatagramPacket(acknowledgement, acknowledgement.length);
        return ackToReceive;
    }

    public static DatagramPacket createAckWithMessagePacketToReceive() {
        byte[] acknowledgement = new byte[256];
        DatagramPacket ackWithMessageToReceive = new DatagramPacket(acknowledgement, acknowledgement.length);
        return ackWithMessageToReceive;
    }

    public static DatagramPacket createInitialAckToSend(int optionalExtraFlag, int totalFileSize, int receivedSeqNr, String message, InetAddress address, int port) {
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        int acknowledgementNumber = receivedSeqNr;
        byte[] acknowledgement = PacketProtocol.createPacketWithHeader(totalFileSize, sequenceNumber, acknowledgementNumber, (PacketProtocol.ACK + optionalExtraFlag), message.getBytes());
        DatagramPacket initialAckWithMessagePacket = new DatagramPacket(acknowledgement, acknowledgement.length, address, port);
        return initialAckWithMessagePacket;
    }

    public static DatagramPacket createAckToSend(int optionalExtraFlag, int receivedSeqNumber, int receivedAckNumber, InetAddress address, int port) {
        int sequenceNumber = receivedAckNumber + 1;
        int acknowledgementNumber = receivedSeqNumber;
        byte[] acknowledgement = PacketProtocol.createHeader(0, sequenceNumber, acknowledgementNumber, (PacketProtocol.ACK + optionalExtraFlag), 0);
        DatagramPacket ackPacket = new DatagramPacket(acknowledgement, acknowledgement.length, address, port);
        return ackPacket;
    }

    /**
     * Respond with an acknowledgement to a received packet.
     *
     * @param optionalExtraFlag is the flag that is set, in addition to the ACK flag.
     * @param receivedSeqNumber is the sequence number in the received packet.
     * @param receivedAckNumber is the acknowledgement number in the received packet.
     * @param socket            is the socket via which the client and server are connected.
     * @param address           is the address to which the acknowledgement must be sent.
     * @param port              is the port to which the acknowledgement must be sent.
     */
    public static void sendAcknowledgement(int optionalExtraFlag, int receivedSeqNumber, int receivedAckNumber, DatagramSocket socket, InetAddress address, int port) {
        DatagramPacket ackPacket = createAckToSend(optionalExtraFlag, receivedSeqNumber, receivedAckNumber, address, port);
        try {
            socket.send(ackPacket);
        } catch (IOException e) {
            e.printStackTrace(); //todo
        }
    }

    /**
     * Send the initial acknowledgement to the request of the client with a message.
     *
     * @param optionalExtraFlag is the flag that can be set in addition to the ACK flag (DOESNOTEXIST/DOESALREADYEXIST).
     *                          Is set to 0 if request can be executed.
     * @param totalFileSize     is the size of the file that the client wants to upload (in case of upload / replace).
     *                          Is set to 0 in response to other requests.
     * @param receivedSeqNr     is the sequence number that is received.
     * @param message           is the message that is sent along with the acknowledgement.
     * @param socket            is the socket via which the client and server are connected.
     * @param address           is the address to which the acknowledgement needs to be sent.
     * @param port              is the port to which the acknowledgement needs to be sent.
     */
    public static void sendInitialAcknowledgementWithMessage(int optionalExtraFlag, int totalFileSize, int receivedSeqNr, String message, DatagramSocket socket, InetAddress address, int port) {
        DatagramPacket initialAckWithMessagePacket =  createInitialAckToSend(optionalExtraFlag, totalFileSize, receivedSeqNr, message, address, port);
        try {
            socket.send(initialAckWithMessagePacket);
        } catch (IOException e) {
            e.printStackTrace(); //todo
        }
    }

    public static void tryToReceiveAck(DatagramSocket socket, DatagramPacket ackPacket, DatagramPacket packetToSend) {
        boolean isAckReceived = false;
        while (!isAckReceived) {
            try {
                socket.send(packetToSend);
                socket.setSoTimeout(PacketProtocol.RTT);
                // set timeOut -- socket will try to receive ack for this period of time. if timer expires without receiving an ACK, method continues.
                // as long as there is no data received, stay in the loop and try receiving the ack. if timer has expired, resend the last packet sent.
                socket.receive(ackPacket);
                byte[] acknowledgement = ackPacket.getData();
                setAcknowledgement(acknowledgement);
                isAckReceived = true;
            } catch (IOException e) {
                System.out.println("Timer has expired - packet that is sent might not have arrived so will be retransmitted.");
            }
        }
    }

    public static void receiveAckWithMessage(DatagramSocket socket, DatagramPacket requestPacket) {
        DatagramPacket ackPacketWithMessage = createAckWithMessagePacketToReceive();
        tryToReceiveAck(socket, ackPacketWithMessage, requestPacket);
    }

    public static void receiveAckOnFileSize(int optionalExtraFlag, int totalFileSize, int receivedSeqNr, String message, InetAddress address, int port, DatagramSocket socket) {
        DatagramPacket initialAckPacketWithFileSize = createInitialAckToSend(optionalExtraFlag, totalFileSize, receivedSeqNr, message, address, port);
        DatagramPacket ackToReceive = createAckPacketToReceive();
        tryToReceiveAck(socket, ackToReceive, initialAckPacketWithFileSize);
    }

    public static byte[] getAcknowledgement() {
        return acknowledgement;
    }

    public static void setAcknowledgement(byte[] acknowledgement) {
        Acknowledgement.acknowledgement = acknowledgement;
    }
}
