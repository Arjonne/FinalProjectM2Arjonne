package com.nedap.university;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Represents the functions for checking whether the received file is the same as the file that is sent.
 */
public class DataIntegrityCheck {
    public final static int HASHCODE_LENGTH = 32;
    public final static int BITS_IN_BYTE = 8;
    public static int flag;
    public static int hashCode;
    public static int receivedSeqNr;
    public static int receivedAckNr;

    public static int getFlag() {
        return flag;
    }

    public static void setFlag(int flag) {
        DataIntegrityCheck.flag = flag;
    }

    public static int getHashCode() {
        return hashCode;
    }

    public static void setHashCode(int hashCode) {
        DataIntegrityCheck.hashCode = hashCode;
    }

    public static int getReceivedSeqNr() {
        return receivedSeqNr;
    }

    public static void setReceivedSeqNr(int receivedSeqNr) {
        DataIntegrityCheck.receivedSeqNr = receivedSeqNr;
    }

    public static int getReceivedAckNr() {
        return receivedAckNr;
    }

    public static void setReceivedAckNr(int receivedAckNr) {
        DataIntegrityCheck.receivedAckNr = receivedAckNr;
    }

    /**
     * Check if checksum is correct. For this, the checksum that is sent can be compared with the calculated checksum at
     * the receiver side. If the sum of these is equal to two bytes of only ones, the sum is correct (checksum and its
     * inverse should result in only ones).
     *
     * @param packetWithHeader is the total packet in which the inverse of the checksum (as calculated by the sender)
     *                         is available. Besides, the checksum can be calculated over the header of the packet.
     * @return true if these are the same, false if not
     */
    public static boolean isChecksumCorrect(byte[] packetWithHeader, int payloadLength) {
        int checksumSent = PacketProtocol.getChecksum(packetWithHeader);
        byte[] checksumInput = PacketProtocol.getChecksumInput(packetWithHeader, payloadLength);
        int checksumCalculated = PacketProtocol.calculateChecksum(checksumInput);
        return checksumSent == checksumCalculated;
    }

//    /**
//     * Check if file on server and local file are the same by comparing their hash codes.
//     *
//     * @param hashCodeOfFileSent     is the hash code of the file that is sent.
//     * @param hashCodeOfFileReceived is the hash code of the received file.
//     * @return true if these hash codes are the same, false if not.
//     */
//    public static boolean areSentAndReceivedFilesTheSame(int hashCodeOfFileSent, int hashCodeOfFileReceived) {
//        return hashCodeOfFileSent == hashCodeOfFileReceived;
//    }
//
//    /**
//     * Try to receive a hashCode.
//     *
//     * @return true if the acknowledgement is received, false if not.
//     */
//    public static boolean receiveHashCode(DatagramSocket socket) {
//        byte[] hashCodeInBytes = new byte[DataIntegrityCheck.HASHCODE_LENGTH + PacketProtocol.HEADER_SIZE];
//        DatagramPacket packetWithHashCode = new DatagramPacket(hashCodeInBytes, hashCodeInBytes.length);
//        try {
//            socket.receive(packetWithHashCode);
////            if (isChecksumCorrect(hashCodeInBytes)) {
//                int flag = PacketProtocol.getFlag(hashCodeInBytes);
//                setFlag(flag);
//                int hashCode = getHashCodeFromPacket(hashCodeInBytes);
//                setHashCode(hashCode);
//                int receivedSeqNr = PacketProtocol.getSequenceNumber(hashCodeInBytes);
//                setReceivedSeqNr(receivedSeqNr);
//                int receivedAckNr = PacketProtocol.getAcknowledgementNumber(hashCodeInBytes);
//                setReceivedAckNr(receivedAckNr);
//                return true;
////            } else {
////                return false;
////            }
//        } catch (IOException e) {
//            e.printStackTrace(); // todo
//            return false;
//        }
//    }
//
//
//    /**
//     * Send hash code packet
//     * @param hashCode
//     * @param receivedSeqNumber
//     * @param receivedAckNumber
//     * @param socket
//     * @param address
//     * @param port
//     */
//    public static void sendHashCode(int hashCode, int receivedSeqNumber, int receivedAckNumber, DatagramSocket socket, InetAddress address, int port) {
//        int sequenceNumber = receivedAckNumber + 1;
//        int acknowledgementNumber = receivedSeqNumber;
//        byte[] hashCodeInBytes = getHashCodeInBytes(hashCode);
//        byte[] hashCodeFullPacket = PacketProtocol.createPacketWithHeader(HASHCODE_LENGTH, sequenceNumber, acknowledgementNumber, PacketProtocol.CHECK, hashCodeInBytes);
//        DatagramPacket hashCodePacket = new DatagramPacket(hashCodeFullPacket, hashCodeFullPacket.length, address, port);
//        try {
//            socket.send(hashCodePacket);
//        } catch (IOException e) {
//            e.printStackTrace(); // todo
//        }
//    }
//
//    public static int getHashCodeFromPacket(byte[] hashCodeInBytes) {
//        int numberOfBitsToShift = (HASHCODE_LENGTH * BITS_IN_BYTE) - BITS_IN_BYTE;
//        hashCode = 0;
//        for (int i = 0; i < HASHCODE_LENGTH; i++) {
//            hashCode = hashCode | (hashCodeInBytes[i] & 0xff) << numberOfBitsToShift;
//        }
//        return hashCode;
//    }
//
//
//    public static byte[] getHashCodeInBytes(int hashCode) {
//        byte[] hashCodeInBytes = new byte[HASHCODE_LENGTH];
//        int numberOfBitsToShift = (HASHCODE_LENGTH * BITS_IN_BYTE) - BITS_IN_BYTE;
//        for (int i = 0; i < HASHCODE_LENGTH; i++) {
//            hashCodeInBytes[i] = (byte) (hashCode >> numberOfBitsToShift);
//            numberOfBitsToShift = numberOfBitsToShift - BITS_IN_BYTE;
//        }
//        return hashCodeInBytes;
//    }
}
