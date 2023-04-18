package com.nedap.university;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public final class Acknowledgement {
    static byte[] acknowledgement;

//          --- CREATE ACKNOWLEDGEMENT PACKETS ---

    /**
     * Create a buffer to be able to receive an acknowledgement.
     *
     * @return an empty DatagramPacket which can be filled with the byte representation of the acknowledgement.
     */
    public static DatagramPacket createAckPacketToReceive() {
        byte[] acknowledgement = new byte[PacketProtocol.HEADER_SIZE];
        return new DatagramPacket(acknowledgement, acknowledgement.length);
    }

    /**
     * Create a buffer to be able to receive an acknowledgement with message.
     *
     * @return an empty DatagramPacket which can be filled with the byte representation of the acknowledgement,
     * including a message.
     */
    public static DatagramPacket createAckWithMessagePacketToReceive() {
        byte[] acknowledgement = new byte[256];
        return new DatagramPacket(acknowledgement, acknowledgement.length);
    }

    /**
     * Create an initial acknowledgement as response to the request of the client.
     *
     * @param optionalExtraFlag is an optional extra flag that can be set (in addition to the standard ACK flag).
     * @param totalFileSize     is the size of the total file that needs to be transmitted.
     * @param lastReceivedSeqNr is the last sequence number received.
     * @param message           is the message that needs to be transmitted.
     * @param address           is the address to which the acknowledgement needs to be sent.
     * @param port              is the port to which the acknowledgement needs to be sent.
     * @return the initial acknowledgement as response to the request of the client.
     */
    public static DatagramPacket createInitialAckToSend(int optionalExtraFlag, int totalFileSize, int lastReceivedSeqNr, String message, InetAddress address, int port) {
        // as this is the first message from the server to the client, the sequence number can be randomly generated:
        int sequenceNumber = PacketProtocol.generateRandomSequenceNumber();
        int acknowledgementNumber = lastReceivedSeqNr;
        byte[] acknowledgement = PacketProtocol.createPacketWithHeader(totalFileSize, sequenceNumber, acknowledgementNumber, (PacketProtocol.ACK + optionalExtraFlag), message.getBytes());
        return new DatagramPacket(acknowledgement, acknowledgement.length, address, port);
    }

    /**
     * Create an acknowledgement to send as a response to a message from the client/server.
     *
     * @param optionalExtraFlag is an optional extra flag that can be set (in addition to the standard ACK flag).
     * @param lastReceivedSeqNr is the last sequence number received.
     * @param lastReceivedAckNr is the last acknowledgement number received.
     * @param address           is the address to which the acknowledgement needs to be sent.
     * @param port              is the port to which the acknowledgement needs to be sent.
     * @return is the acknowledgement as response to a previous message of the client or server.
     */
    public static DatagramPacket createAckToSend(int optionalExtraFlag, int lastReceivedSeqNr, int lastReceivedAckNr, InetAddress address, int port) {
        int sequenceNumber = lastReceivedAckNr + 1;
        int acknowledgementNumber = lastReceivedSeqNr;
        byte[] acknowledgement = PacketProtocol.createHeader(0, sequenceNumber, acknowledgementNumber, (PacketProtocol.ACK + optionalExtraFlag), 0);
        return new DatagramPacket(acknowledgement, acknowledgement.length, address, port);
    }

//          --- SEND ACKNOWLEDGEMENT PACKETS ---

    /**
     * Send the initial acknowledgement to the request of the client with a message.
     *
     * @param optionalExtraFlag is an optional extra flag that can be set (in addition to the standard ACK flag).
     * @param totalFileSize     is the size of the total file that needs to be transmitted.
     * @param lastReceivedSeqNr is the last sequence number received.
     * @param message           is the message that needs to be transmitted.
     * @param socket            is the socket via which the client and server are connected.
     * @param address           is the address to which the acknowledgement needs to be sent.
     * @param port              is the port to which the acknowledgement needs to be sent.
     */
    public static void sendInitialAcknowledgementWithMessage(int optionalExtraFlag, int totalFileSize, int lastReceivedSeqNr, String message, DatagramSocket socket, InetAddress address, int port) {
        DatagramPacket initialAckWithMessagePacket = createInitialAckToSend(optionalExtraFlag, totalFileSize, lastReceivedSeqNr, message, address, port);
        try {
            socket.send(initialAckWithMessagePacket);
        } catch (IOException e) {
            System.out.println("Check the destination address input, as the destination could not be found.");
        }
    }

    /**
     * Respond with an acknowledgement to a received packet.
     *
     * @param optionalExtraFlag is an optional extra flag that can be set (in addition to the standard ACK flag).
     * @param lastReceivedSeqNr is the last sequence number received.
     * @param lastReceivedAckNr is the last acknowledgement number received.
     * @param socket            is the socket via which the client and server are connected.
     * @param address           is the address to which the acknowledgement needs to be sent.
     * @param port              is the port to which the acknowledgement needs to be sent.
     */
    public static void sendAcknowledgement(int optionalExtraFlag, int lastReceivedSeqNr, int lastReceivedAckNr, DatagramSocket socket, InetAddress address, int port) {
        DatagramPacket ackPacket = createAckToSend(optionalExtraFlag, lastReceivedSeqNr, lastReceivedAckNr, address, port);
        try {
            socket.send(ackPacket);
        } catch (IOException e) {
            System.out.println("Check the destination address input, as the destination could not be found.");
        }
    }


//          --- RECEIVE ACKNOWLEDGEMENT PACKETS ---

    /**
     * Try to send a packet and to receive an acknowledgement before the timer expires. Resend the packet to be
     * transmitted if the acknowledgement is not received in time.
     *
     * @param socket       is the socket via which the client and server are connected.
     * @param ackPacket    is the acknowledgement packet that needs to be received.
     * @param packetToSend is the packet that the server or client tries to send and to which an acknowledgement is
     *                     expected.
     */
    public static void sendPacketAndReceiveAck(DatagramSocket socket, DatagramPacket ackPacket, DatagramPacket packetToSend) {
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

    /**
     * Send a request and try to receive an acknowledgement before the timer expires. If that does not happen, resend
     * the request.
     *
     * @param socket        is the socket via which the client and server are connected.
     * @param requestPacket is the request that needs to be transmitted.
     */
    public static void sendRequestAndReceiveAckWithMessage(DatagramSocket socket, DatagramPacket requestPacket) {
        DatagramPacket ackPacketWithMessage = createAckWithMessagePacketToReceive();
        sendPacketAndReceiveAck(socket, ackPacketWithMessage, requestPacket);
    }

    /**
     * Send the checksum and try to receive an acknowledgement. This acknowledgement can carry the additional flag
     * 'INCORRECT', indicating that the total checksum was not correct.
     *
     * @param socket         is the socket via which the client and server are connected.
     * @param checksumPacket is the packet with the checksum of the total original file.
     * @return true if the checksum was received correctly, false if not.
     */
    public static boolean sendChecksumAndReceiveAck(DatagramSocket socket, DatagramPacket checksumPacket) {
        DatagramPacket ackPacket = createAckPacketToReceive();
        sendPacketAndReceiveAck(socket, ackPacket, checksumPacket);
        byte[] ackReceived = getAcknowledgement();
        int flag = PacketProtocol.getFlag(ackReceived);
        return flag == PacketProtocol.ACK;
    }

    /**
     * Send an acknowledgement that also carries the total file size of the packet to transmit, and try to receive an
     * acknowledgement before the timer expires. If that does not happen, resend the request.
     *
     * @param optionalExtraFlag is an optional extra flag that can be set (in addition to the standard ACK flag).
     * @param totalFileSize     is the size of the total file that needs to be transmitted.
     * @param lastReceivedSeqNr is the last sequence number received.
     * @param message           is the message that needs to be transmitted.
     * @param address           is the address to which the acknowledgement needs to be sent.
     * @param port              is the port to which the acknowledgement needs to be sent.
     * @param socket            is the socket via which the client and server are connected.
     */
    public static void sendAckWithFileSizeAndReceiveAck(int optionalExtraFlag, int totalFileSize, int lastReceivedSeqNr, String message, InetAddress address, int port, DatagramSocket socket) {
        DatagramPacket initialAckPacketWithFileSize = createInitialAckToSend(optionalExtraFlag, totalFileSize, lastReceivedSeqNr, message, address, port);
        DatagramPacket ackToReceive = createAckPacketToReceive();
        sendPacketAndReceiveAck(socket, ackToReceive, initialAckPacketWithFileSize);
    }


//          --- GETTERS AND SETTERS ---

    /**
     * Get the byte representation of the acknowledgement packet.
     *
     * @return the byte representation of the acknowledgement packet.
     */
    public static byte[] getAcknowledgement() {
        return acknowledgement;
    }

    /**
     * Set the byte representation of the acknowledgement packet to be able to reuse this information in other classes.
     *
     * @param acknowledgement is the byte representation of the acknowledgement packet.
     */
    public static void setAcknowledgement(byte[] acknowledgement) {
        Acknowledgement.acknowledgement = acknowledgement;
    }
}
