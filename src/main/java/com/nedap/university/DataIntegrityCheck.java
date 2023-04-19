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

    /**
     * Get the input that is needed to calculate the checksum (which is the total header without the two bytes that
     * include the checksum result).
     *
     * @param packetHeader  is the header of the packet of interest.
     * @param payloadLength is the total length of the data that is being transmitted in the packet.
     * @return the byte representation of all the checksum input.
     */
    public static byte[] getChecksumInput(byte[] packetHeader, int payloadLength) {
        byte[] totalChecksumInput = new byte[(PacketProtocol.HEADER_SIZE + 2)];
        System.arraycopy(packetHeader, 0, totalChecksumInput, 0, (PacketProtocol.HEADER_SIZE - 2));
        totalChecksumInput[PacketProtocol.HEADER_SIZE - 2] = (byte) (payloadLength >> 8);
        totalChecksumInput[PacketProtocol.HEADER_SIZE - 1] = (byte) (payloadLength & 0xff);
        totalChecksumInput[PacketProtocol.HEADER_SIZE] = (byte) (PacketProtocol.HEADER_SIZE >> 8);
        totalChecksumInput[PacketProtocol.HEADER_SIZE + 1] = (byte) (PacketProtocol.HEADER_SIZE & 0xff);
        return totalChecksumInput;
    }

    /**
     * Calculate the checksum.
     *
     * @param checksumInput is the input for the checksum.
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
            checksum = checksum + (checksumInput[i] << 8);
            if ((checksum & 0xffff0000) > 0) {
                checksum = checksum & 0xffff;
                checksum++;
            }
        }
        return (~checksum & 0xffff);
    }

    /**
     * Check if checksum is correct. For this, the checksum that is sent can be compared with the calculated checksum at
     * the receiver side.
     *
     * @param packetWithHeader is the total packet, including header, of interest.
     * @param payloadLength    is the total length of the data that is being transmitted in the packet.
     * @return true if the checksum is the same, false if not.
     */
    public static boolean isChecksumCorrect(byte[] packetWithHeader, int payloadLength) {
        int checksumSent = PacketProtocol.getChecksum(packetWithHeader);
        byte[] checksumInput = getChecksumInput(packetWithHeader, payloadLength);
        int checksumCalculated = calculateChecksum(checksumInput);
        return checksumSent == checksumCalculated;
    }

    /**
     * Get the checksum of the total file which is sent in a separate packet.
     *
     * @param packetWithChecksum is the packet that includes the checksum.
     * @return the checksum that is sent.
     */
    public static int getChecksum(DatagramPacket packetWithChecksum) {
        byte[] checksumPacketData = packetWithChecksum.getData();
        byte[] checksum = new byte[CHECKSUM_LENGTH];
        System.arraycopy(checksumPacketData, PacketProtocol.HEADER_SIZE, checksum, 0, CHECKSUM_LENGTH);
        return (((checksum[0] & 0xff) << 8) | (checksum[1] & 0xff));
    }

    /**
     * Try to receive the packet that includes the checksum of the total file.
     *
     * @param socket is the socket via which the client and server are connected.
     * @return the packet that carries the checksum.
     */
    public static DatagramPacket receiveChecksum(DatagramSocket socket) {
        boolean received = false;
        byte[] receivedChecksumInBytes = new byte[DataIntegrityCheck.CHECKSUM_LENGTH + PacketProtocol.HEADER_SIZE];
        DatagramPacket packetWithChecksum = new DatagramPacket(receivedChecksumInBytes, receivedChecksumInBytes.length);
        while (!received) {
            try {
                socket.receive(packetWithChecksum);
                received = true;
            } catch (IOException e) {
                System.out.println("Timer has expired - packet that is sent might not have arrived so will be retransmitted.");
            }
        }
        return packetWithChecksum;
    }

    /**
     * Get the byte representation of the checksum.
     *
     * @param checksumOfTotalFile is the checksum of the total file.
     * @return the byte representation of the checksum.
     */
    public static byte[] checksumOfTotalFileInBytes(int checksumOfTotalFile) {
        byte[] checksumInBytes = new byte[CHECKSUM_LENGTH];
        checksumInBytes[0] = (byte) (checksumOfTotalFile >> 8);
        checksumInBytes[1] = (byte) (checksumOfTotalFile & 0xff);
        return checksumInBytes;
    }

    /**
     * Create a datagram packet that includes the checksum of the total file
     *
     * @param checksumOfTotalFile   is the checksum of the total file.
     * @param lastReceivedSeqNumber is the last received sequence number.
     * @param lastReceivedAckNumber is the last received acknowledgement number.
     * @param address               is the address to which the packet needs to be sent.
     * @param port                  is the port to which the packet needs to be sent.
     * @return the packet that includes the checksum to send.
     */
    public static DatagramPacket createChecksumPacket(int checksumOfTotalFile, int lastReceivedSeqNumber, int lastReceivedAckNumber, InetAddress address, int port) {
        int sequenceNumber = lastReceivedAckNumber + 1;
        int acknowledgementNumber = lastReceivedSeqNumber;
        byte[] checksumInBytes = checksumOfTotalFileInBytes(checksumOfTotalFile);
        byte[] checksumFullPacket = PacketProtocol.createPacketWithHeader(CHECKSUM_LENGTH, sequenceNumber, acknowledgementNumber, PacketProtocol.CHECK, checksumInBytes);
        return new DatagramPacket(checksumFullPacket, checksumFullPacket.length, address, port);
    }

    /**
     * Check if the received and actual checksum of the total file are the same.
     *
     * @param receivedChecksum       is the checksum that is received from the source.
     * @param checksumOfReceivedFile is the checksum that is calculated over the received file.
     * @return true if the two checksums are the same, false if not.
     */
    public static boolean areChecksumOfTwoFilesTheSame(int receivedChecksum, int checksumOfReceivedFile) {
        return receivedChecksum == checksumOfReceivedFile;
    }


    /**
     * Receive the checksum of the original file, calculate the checksum of the received file, compare the results and
     * send an acknowledgement to the source.
     *
     * @param socket       is the socket via which the client and server are connected.
     * @param inetAddress  is the address to which the acknowledgement needs to be sent.
     * @param port         is the port to which the acknowledgement needs to be sent.
     * @param receivedFile is the file that is received.
     * @return true if the checksum is correct, false if not.
     */
    public static boolean receiveAndPerformTotalChecksum(DatagramSocket socket, InetAddress inetAddress, int port, File receivedFile) {
        boolean correctlyReceived = false;
        while (!correctlyReceived) {
            // receive the checksum of the original file from the client:
            DatagramPacket packetWithChecksum = DataIntegrityCheck.receiveChecksum(socket);
            int receivedFlag = PacketProtocol.getFlag(packetWithChecksum.getData());
            // if you did not receive a packet with the CHECK flag, wait for a new packet. Otherwise, the checksum
            // can be performed.
            if (receivedFlag != PacketProtocol.CHECK) {
                continue;
            }
            correctlyReceived = true;
            int lastReceivedSeqNr = PacketProtocol.getSequenceNumber(packetWithChecksum.getData());
            int lastReceivedAckNr = PacketProtocol.getAcknowledgementNumber(packetWithChecksum.getData());
            int receivedChecksum = DataIntegrityCheck.getChecksum(packetWithChecksum);
            // calculate the checksum of the received file:
            byte[] receivedFileInBytes = StopAndWaitProtocol.getFileInBytes();
            int checksumOfReceivedFile = DataIntegrityCheck.calculateChecksum(receivedFileInBytes);
            if (DataIntegrityCheck.areChecksumOfTwoFilesTheSame(receivedChecksum, checksumOfReceivedFile)) {
                // if the two checksums are the same, send an acknowledgement
                Acknowledgement.sendAcknowledgement(0, lastReceivedSeqNr, lastReceivedAckNr, socket, inetAddress, port);
                return true;
            } else {
                // if the two checksums are not the same, a mistake has occurred during transmission. The
                // downloaded file will be removed (as it is not the same as the original one) and an
                // INCORRECT flag will be sent to the server.
                if (receivedFile != null && receivedFile.delete()) {
                    Acknowledgement.sendAcknowledgement(PacketProtocol.INCORRECT, lastReceivedSeqNr, lastReceivedAckNr, socket, inetAddress, port);
                    return false;
                }
            }
        }
        return false;
    }
}
