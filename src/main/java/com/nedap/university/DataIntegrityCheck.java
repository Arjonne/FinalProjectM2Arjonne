package com.nedap.university;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Represents the functions for checking whether the received file is the same as the file that is sent.
 */
public final class DataIntegrityCheck {
    public final static int CHECKSUM_LENGTH = 2;
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
     * Get the input that is needed to calculate the checksum (which is the total header without the two bytes that
     * include the checksum result).
     *
     * @return byte representation of all the checksum input.
     */
    public static byte[] getChecksumInput(byte[] packetHeader, int payloadLength) {
        byte[] totalChecksumInput = new byte[(PacketProtocol.HEADER_SIZE + 2)];
        System.arraycopy(packetHeader, 0, totalChecksumInput, 0, (PacketProtocol.HEADER_SIZE - 2));
        totalChecksumInput[PacketProtocol.HEADER_SIZE - 2] = (byte) ((payloadLength >> 8) & 0xff);
        totalChecksumInput[PacketProtocol.HEADER_SIZE - 1] = (byte) (payloadLength & 0xff);
        totalChecksumInput[PacketProtocol.HEADER_SIZE] = (byte) ((PacketProtocol.HEADER_SIZE >> 8) & 0xff);
        totalChecksumInput[PacketProtocol.HEADER_SIZE + 1] = (byte) (PacketProtocol.HEADER_SIZE & 0xff);
        return totalChecksumInput;
    }

    /**
     * Calculate the checksum.
     *
     * @param checksumInput is the input for the checksum (header information).
     * @return the inverse result of the checksum.
     */
    public static int calculateChecksum(byte[] checksumInput) {
        int checksum = 0;
        int length = checksumInput.length;
        int i = 0;
        while (length > 1) {
            checksum = checksum + (((checksumInput[i] & 0xff) << 8) | (checksumInput[i + 1] & 0xff));

            if ((checksum & 0xffff0000) > 0) {
                checksum = checksum & 0xffff;
                checksum++;
            }
            i = i + 2;
            length = length - 2;
        }
        if (length == 1) {
            checksum = checksum + (checksumInput[i] >> 8);
            if ((checksum & 0xffff0000) > 0) {
                checksum = checksum & 0xffff;
                checksum++;
            }
        }
        int inverseChecksum = ~checksum & 0xffff;
        return inverseChecksum;
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
        byte[] checksumInput = getChecksumInput(packetWithHeader, payloadLength);
        int checksumCalculated = calculateChecksum(checksumInput);
        return checksumSent == checksumCalculated;
    }

    public static int getChecksum(DatagramPacket packetWithChecksum) {
        byte[] checksumPacketData = packetWithChecksum.getData();
        byte[] checksum = new byte[CHECKSUM_LENGTH];
        System.arraycopy(checksumPacketData, PacketProtocol.HEADER_SIZE, checksum, 0, CHECKSUM_LENGTH);
        return (((checksum[0] & 0xff) << 8) | (checksum[1] & 0xff));
    }


    public static DatagramPacket receiveChecksum(DatagramSocket socket) {
        boolean received = false;
        byte[] receivedChecksumInBytes = new byte[DataIntegrityCheck.CHECKSUM_LENGTH + PacketProtocol.HEADER_SIZE];
        DatagramPacket packetWithChecksum = new DatagramPacket(receivedChecksumInBytes, receivedChecksumInBytes.length);
        while (!received) {
            try {
                socket.receive(packetWithChecksum);
                received = true;
            } catch (IOException e) {
                e.printStackTrace(); // todo
                return null;
            }
        }
        return packetWithChecksum;
    }

    public static byte[] checksumOfTotalFileInBytes(int checksumOfTotalFile) {
        byte[] checksumInBytes = new byte[CHECKSUM_LENGTH];
        checksumInBytes[0] = (byte) (checksumOfTotalFile >> 8);
        checksumInBytes[1] = (byte) (checksumOfTotalFile & 0xff);
        return checksumInBytes;
    }

    public static DatagramPacket createChecksumPacket(int checksumOfTotalFile, int receivedSeqNumber, int receivedAckNumber, InetAddress address, int port) {
        int sequenceNumber = receivedAckNumber + 1;
        int acknowledgementNumber = receivedSeqNumber;
        byte[] checksumInBytes = checksumOfTotalFileInBytes(checksumOfTotalFile);
        byte[] checksumFullPacket = PacketProtocol.createPacketWithHeader(CHECKSUM_LENGTH, sequenceNumber, acknowledgementNumber, PacketProtocol.CHECK, checksumInBytes);
        DatagramPacket checksumToSend = new DatagramPacket(checksumFullPacket, checksumFullPacket.length, address, port);
        return checksumToSend;
    }
}
