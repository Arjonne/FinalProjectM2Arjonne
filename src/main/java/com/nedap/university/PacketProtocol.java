package com.nedap.university;

import java.util.Random;

/**
 * Represents the protocol for building packets for the transmission between client and server.
 */
public final class PacketProtocol {
    // address and port of Raspberry Pi:
    public static final String PI_ADDRESS = "localhost";
    //    public static final String PI_ADDRESS = "172.16.1.1";
    public static final int PI_PORT = 9090;
    // as MTU is 1500, use a maximal packet size of 1500:
    public static final int MAX_PACKET_SIZE = 1500;
    public static final int HEADER_SIZE = 16;
    // flags:
    public static final int ACK = 1;
    public static final int UPLOAD = 2;
    public static final int DOWNLOAD = 4;
    public static final int REMOVE = 8;
    public static final int REPLACE = 16;
    public static final int LIST = 32;
    public static final int CLOSE = 64;
    public static final int DOESNOTEXIST = 128;
    public static final int DOESALREADYEXIST = 256;
    public static final int MOREFRAGMENTS = 512;
    public static final int LAST = 1024;
    public static final int INCORRECT = 2048;

    /**
     * Create a header for the datagram packet to be able to use sequence numbers and acknowledgements for checking
     * packets, flags for the correct statement and checksum to check for correct data transmission.
     *
     * @param totalFileSize is the total size of the actual file (without header) that needs to be transmitted.
     * @param sequenceNumber   is the sequence number of the packet, which starts at a random number and goes up with 1
     *                         every round trip.
     * @param flag             is the flag that is being set.
     * @return the header as byte array (which is the same format as the datagram packet itself).
     */
    public static byte[] createHeader(int totalFileSize, int sequenceNumber, int flag) {
        int acknowledgementNumber = sequenceNumber + 1;
        byte[] header = new byte[HEADER_SIZE];
        // four bytes for size of the total file without header:
        header[0] = (byte) (totalFileSize >> 24);
        header[1] = (byte) ((totalFileSize >> 16) & 0xff);
        header[2] = (byte) ((totalFileSize >> 8) & 0xff);
        header[3] = (byte) (totalFileSize & 0xff);
        // four bytes for the sequence number:
        header[4] = (byte) (sequenceNumber >> 24);
        header[5] = (byte) ((sequenceNumber >> 16) & 0xff);
        header[6] = (byte) ((sequenceNumber >> 8) & 0xff);
        header[7] = (byte) (sequenceNumber & 0xff);
        // four bytes for the acknowledgement number:
        header[8] = (byte) (acknowledgementNumber >> 24);
        header[9] = (byte) ((acknowledgementNumber >> 16) & 0xff);
        header[10] = (byte) ((acknowledgementNumber >> 8) & 0xff);
        header[11] = (byte) (acknowledgementNumber & 0xff);
        // two bytes for the flag(s):
        header[12] = (byte) (flag >> 8); // room for 4 more flags if necessary
        header[13] = (byte) (flag & 0xff);

        // create a new byte array with all information that is needed for the checksum:
        byte[] checksumInput = getChecksumInput(header);
        // two bytes for checksum:
        header[14] = (byte) (calculateChecksum(checksumInput) >> 8);
        header[15] = (byte) (calculateChecksum(checksumInput) & 0xff);
        return header;
    }

    /**
     * Create a new byte array in which the header and actual data to be sent are combined.
     *
     * @param sequenceNumber is the sequence number (which is needed for the header).
     * @param flag           is (/are) the flag(s) that need to be set in the header.
     * @param fileData       is the data of the actual file to be transmitted.
     * @return the byte array of the total packet (combination of header and data).
     */
    public static byte[] createPacketWithHeader(int totalFileSize, int sequenceNumber, int flag, byte[] fileData) {
        int totalPacketSize = HEADER_SIZE + fileData.length;
        byte[] header = createHeader(totalFileSize, sequenceNumber, flag);
        byte[] totalPacket = new byte[totalPacketSize];
        // copy header into total packet:
        for (int i = 0; i < HEADER_SIZE; i++) {
            totalPacket[i] = header[i];
        }
        // copy data of file into total packet:
        for (int i = HEADER_SIZE; i < (totalPacketSize); i++) {
            totalPacket[i] = fileData[i - HEADER_SIZE];
        }
        return totalPacket;
    }

    /**
     * Generate random sequence number as every transmission starts with a random, unique sequence number and counts up
     * from that point.
     *
     * @return the sequence number.
     */
    public static int generateRandomSequenceNumber() {
        Random random = new Random();
        // upperbound is the maximum number that can be represented in four bytes (number of bytes available for this
        // sequence number in the header).
        int upperbound = 2147483647;
        int randomInt = random.nextInt(upperbound);
        return randomInt;
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
            checksum = checksum + ((checksumInput[i] >> 8) | (checksumInput[i + 1] & 0xff));
            if ((checksum & 0xffff0000) > 0) { //todo checken hoe dit precies zit
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
        int inverseChecksum = ~checksum;
        return inverseChecksum;
    }

    /**
     * Get the input that is needed to calculate the checksum (which is the total header without the two bytes that
     * include the checksum result).
     *
     * @param packetWithHeader is the byte representation of the header.
     * @return byte representation of the checksum input.
     */
    public static byte[] getChecksumInput(byte[] packetWithHeader) {
        byte[] checksumInput = new byte[(HEADER_SIZE - 2)];
        System.arraycopy(packetWithHeader, 0, checksumInput, 0, (HEADER_SIZE - 2));
        return checksumInput;
    }

    /**
     * Get the information on total file size from the header.
     *
     * @param packetWithHeader is the packet that includes the header.
     * @return the total file size.
     */
    public static int getFileSizeInPacket(byte[] packetWithHeader) {
        return (((packetWithHeader[0] << 24) & 0xff) | ((packetWithHeader[1] << 16) & 0xff) | ((packetWithHeader[2] << 8) & 0xff) | (packetWithHeader[3]) & 0xff);
    }

    /**
     * Get the sequence number from the header.
     *
     * @param packetWithHeader is the packet that includes the header.
     * @return the sequence number.
     */
    public static int getSequenceNumber(byte[] packetWithHeader) {
        return (((packetWithHeader[4] << 24) & 0xff) | ((packetWithHeader[5] << 16) & 0xff) | ((packetWithHeader[6] << 8) & 0xff) | (packetWithHeader[7]) & 0xff);
    }

    /**
     * Get the acknowledgement number from the header.
     *
     * @param packetWithHeader is the packet that includes the header.
     * @return the acknowledgement number.
     */
    public static int getAcknowledgementNumber(byte[] packetWithHeader) {
        return (((packetWithHeader[8] << 24) & 0xff) | ((packetWithHeader[9] << 16) & 0xff) | ((packetWithHeader[10] << 8) & 0xff) | (packetWithHeader[11]) & 0xff);
    }

    /**
     * Get the information on the flags that are set from the header.
     *
     * @param packetWithHeader is the packet that includes the header.
     * @return the flags that are set.
     */
    public static int getFlag(byte[] packetWithHeader) {
        return (((packetWithHeader[12] << 8) & 0xff) | (packetWithHeader[13] & 0xff));
    }

    /**
     * Get the inverse checksum that is calculated by the sender from the header.
     *
     * @param packetWithHeader is the packet that includes the header.
     * @return the inverse checksum as calculated by the sender.
     */
    public static int getChecksum(byte[] packetWithHeader) {
        return (((packetWithHeader[14] << 8) & 0xff) | (packetWithHeader[15] & 0xff));
    }
}
