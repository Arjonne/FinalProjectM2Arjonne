package com.nedap.university;

import java.util.Random;

/**
 * Represents the protocol for building and sending packets between server and client.
 */
public final class PacketProtocol {
    // address and port of Raspberry Pi:
    public static final String PI_ADDRESS = "172.16.1.1";
    public static final int PI_PORT = 9090;
    // as MTU is 1500, use a maximal packet size of 1500:
    public static final int MAX_PACKET_SIZE = 1500;
    public static final int HEADER_SIZE = 20;
    // flags:
    public static final int HELLO = 1;
    public static final int ACK = 2;
    public static final int UPLOAD = 4;
    public static final int DOWNLOAD = 8;
    public static final int REMOVE = 16;
    public static final int REPLACE = 32;
    public static final int LIST = 64;
    public static final int CLOSE = 128;
    public static final int DOESNOTEXIST = 256;
    public static final int MOREFRAGMENTS = 512;
    public static final int LASTFRAGMENT = 1024;

    /**
     * Create a header for the datagram packet to be able to use sequence numbers and acknowledgements for checking
     * packets, flags for the correct statement and checksum to check for correct data transmission.
     *
     * @param sequenceNumber is the number of the
     * @param flag           is the flag that is being set
     * @return the header as byte array (which is the same format as the datagram packet itself).
     */
    public static byte[] createHeader(int fileSize, int sequenceNumber, int offset, int flag) {
        int acknowledgementNumber = sequenceNumber + 1;
        byte[] header = new byte[HEADER_SIZE];
        // four bytes for size of total file without header:
        header[0] = (byte) (fileSize >> 24);
        header[1] = (byte) ((fileSize >> 16) & 0xff);
        header[2] = (byte) ((fileSize >> 8) & 0xff);
        header[3] = (byte) (fileSize & 0xff);
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
        // four bytes to keep track of which part of the total packet this one carries:
        header[12] = (byte) (offset >> 24);
        header[13] = (byte) ((offset >> 16) & 0xff);
        header[14] = (byte) ((offset >> 8) & 0xff);
        header[15] = (byte) (offset & 0xff);
        // two bytes for the HELLO/ACK/UPLOAD/DOWNLOAD/REMOVE/REPLACE/LIST/CLOSE/DOESNOTEXIST/MOREFRAGMENTS/LASTFRAGMENT flag(s):
        header[16] = (byte) (flag >> 8); // room for 5 more flags if necessary
        header[17] = (byte) (flag & 0xff);

        // create a new byte array with all information that is needed for the checksum:
        byte[] checksumInput = new byte[(HEADER_SIZE - 2)];
        // copy all information (except checksum information) from the header into this array:
        System.arraycopy(header, 0, checksumInput, 0, (HEADER_SIZE - 2));

        // two bytes for checksum:
        header[18] = (byte) (calculateChecksum(checksumInput) >> 8);
        header[19] = (byte) (calculateChecksum(checksumInput) & 0xff);
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
    public static byte[] createPacketWithHeader(int sequenceNumber, int offset, int flag, byte[] fileData) {
        int totalPacketSize = HEADER_SIZE + fileData.length;
        byte[] header = createHeader(fileData.length, sequenceNumber, offset, flag);
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
        // upperbound is 2^23: there are 4 bytes reserved for sequence number --> start with random number that takes a
        // max of 3 bytes --> rest of space can be filled with increasing sequence number.
        int upperbound = 8388608;
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
        int inverseChecksum = ~checksum;
        return inverseChecksum;
    }

    /**
     * Get the information on total file size from the header.
     *
     * @param packetWithHeader is the packet that includes the header.
     * @return the total file size.
     */
    public int getFileSize(byte[] packetWithHeader) {
        return ((packetWithHeader[0] << 24) | (packetWithHeader[1] << 16) | (packetWithHeader[2] << 8) | packetWithHeader[3]);
    }

    /**
     * Get the sequence number from the header.
     *
     * @param packetWithHeader is the packet that includes the header.
     * @return the sequence number.
     */
    public int getSequenceNumber(byte[] packetWithHeader) {
        return ((packetWithHeader[4] << 24) | (packetWithHeader[5] << 16) | (packetWithHeader[6] << 8) | packetWithHeader[7]);
    }

    /**
     * Get the acknowledgement number from the header.
     *
     * @param packetWithHeader is the packet that includes the header.
     * @return the acknowledgement number.
     */
    public int getAcknowledgementNumber(byte[] packetWithHeader) {
        return ((packetWithHeader[8] << 24) | (packetWithHeader[9] << 16) | (packetWithHeader[10] << 8) | packetWithHeader[11]);
    }

    /**
     * Get the offset from the header.
     *
     * @param packetWithHeader is the packet that includes the header.
     * @return the offset.
     */
    public int getOffset(byte[] packetWithHeader) {
        return ((packetWithHeader[12] << 24) | (packetWithHeader[13] << 16) | (packetWithHeader[14] << 8) | packetWithHeader[15]);
    }

    /**
     * Get the information on the flags that are set from the header.
     *
     * @param packetWithHeader is the packet that includes the header.
     * @return the flags that are set.
     */
    public static int getFlag(byte[] packetWithHeader) {
        return ((packetWithHeader[16] << 8) | packetWithHeader[17]);
    }

    /**
     * Get the inverse checksum that is calculated by the sender from the header.
     *
     * @param packetWithHeader is the packet that includes the header.
     * @return the inverse checksum as calculated by the sender.
     */
    public int getChecksum(byte[] packetWithHeader) {
        return ((packetWithHeader[18] << 8) | packetWithHeader[19]);
    }

    /**
     * Check if checksum is correct. For this, the checksum that is sent can be compared with the calculated checksum at
     * the receiver side. If the sum of these is equal to two bytes of only ones, the sum is correct (checksum and its
     * inverse should result in only ones).
     *
     * @param packetWithHeader is the total packet in which the inverse of the checksum (as calculated by the sender)
     *                         is available.
     * @param checksumInput    is the information in the header that is needed to recalculate the checksum
     *                         (by the receiver)
     * @return true if these are the same, false if not
     */
    public boolean isChecksumCorrect(byte[] packetWithHeader, byte[] checksumInput) {
        int checksumSent = getChecksum(packetWithHeader);
        int checksumCalculated = calculateChecksum(checksumInput);
        if (checksumSent + checksumCalculated == 0xffff) {
            return true;
        } else {
            return false;
        }
    }
}
