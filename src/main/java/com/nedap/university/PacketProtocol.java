package com.nedap.university;

import java.util.Random;

/**
 * Represents the protocol for building packets for the transmission between client and server.
 */
public final class PacketProtocol {
//          --- ADDRESS AND PORT INFORMATION ---
 //   public static final String PI_ADDRESS = "localhost";
    public static final String PI_ADDRESS = "172.16.1.1";
    public static final int PI_PORT = 9090;

//          --- SIZES ---
    public static final int MAX_PACKET_SIZE = 1500; // which is the MTU.
    public static final int PACKET_WITH_MESSAGE_SIZE = 256; // which is big enough to receive the messages that are being sent.
    public static final int HEADER_SIZE = 16;
    public static final int RTT = 3000; // which is the mean RTT as calculated over a stable network.
    public static final int TIMEOUT = 2*RTT; // which is the default time-out time.

//          --- FLAGS ---
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
    public static final int CHECK = 2048;
    public static final int INCORRECT = 4096;

    /**
     * Create a header for the datagram packet to be able to use sequence numbers and acknowledgements for checking
     * packets, flags for the correct statement and checksum to check for correct data transmission.
     *
     * @param totalFileSize         is the total size of the actual file (without header) that needs to be transmitted.
     * @param sequenceNumber        is the sequence number of the packet.
     * @param acknowledgementNumber is acknowledgement number of the packet (= sequence number of received packet).
     * @param flag                  is the flag that this packet carries.
     * @param payloadLength         is the length of the data this packet carries.
     * @return the header of the packet.
     */
    public static byte[] createHeader(int totalFileSize, int sequenceNumber, int acknowledgementNumber, int flag, int payloadLength) {
        byte[] header = new byte[HEADER_SIZE];
        // four bytes for size of the total file (without header):
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
        header[12] = (byte) (flag >> 8); // room for 3 more flags if necessary
        header[13] = (byte) (flag & 0xff);
        // create a new byte array with all information that is needed for the checksum:
        byte[] checksumInput = DataIntegrityProtocol.getChecksumInput(header, payloadLength);
        // two bytes for checksum:
        header[14] = (byte) (DataIntegrityProtocol.calculateChecksum(checksumInput) >> 8);
        header[15] = (byte) (DataIntegrityProtocol.calculateChecksum(checksumInput) & 0xff);
        return header;
    }

    /**
     * Create a new byte array in which the header and actual data to be sent are combined.
     *
     * @param totalFileSize  is the size of the total file that needs to be transmitted.
     * @param sequenceNumber is the sequence number of the packet.
     * @param ackNumber      is the acknowledgement number of the packet.
     * @param flag           is the flag that this packet carries.
     * @param fileData       is the byte representation of the actual data this packet carries.
     * @return the total packet with header.
     */
    public static byte[] createPacketWithHeader(int totalFileSize, int sequenceNumber, int ackNumber, int flag, byte[] fileData) {
        int totalPacketSize = HEADER_SIZE + fileData.length;
        byte[] header = createHeader(totalFileSize, sequenceNumber, ackNumber, flag, fileData.length);
        byte[] totalPacket = new byte[totalPacketSize];
        // copy header into total packet:
        System.arraycopy(header, 0, totalPacket, 0, HEADER_SIZE);
        // copy data of file into total packet:
        System.arraycopy(fileData, 0, totalPacket, HEADER_SIZE, (totalPacketSize - HEADER_SIZE));
        return totalPacket;
    }

    /**
     * Generate random sequence number as every sequence of transmissions starts with a random, unique sequence number
     * and counts up from that point.
     *
     * @return the sequence number.
     */
    public static int generateRandomSequenceNumber() {
        Random random = new Random();
        // upperbound is the maximum number that can be represented in four bytes (number of bytes available for this
        // sequence number in the header).
        int upperbound = 0xfffffff;
        return random.nextInt(upperbound);
    }

//          --- GETTERS ---

    /**
     * Get the information on total file size from the header.
     *
     * @param packetWithHeader is the packet that includes the header.
     * @return the total file size.
     */
    public static int getFileSizeInPacket(byte[] packetWithHeader) {
        return (((packetWithHeader[0] & 0xff) << 24) | ((packetWithHeader[1] & 0xff) << 16) | ((packetWithHeader[2] & 0xff) << 8) | (packetWithHeader[3] & 0xff));
    }

    /**
     * Get the sequence number from the header.
     *
     * @param packetWithHeader is the packet that includes the header.
     * @return the sequence number.
     */
    public static int getSequenceNumber(byte[] packetWithHeader) {
        return (((packetWithHeader[4] & 0xff) << 24) | ((packetWithHeader[5] & 0xff) << 16) | ((packetWithHeader[6] & 0xff) << 8) | (packetWithHeader[7] & 0xff));
    }

    /**
     * Get the acknowledgement number from the header.
     *
     * @param packetWithHeader is the packet that includes the header.
     * @return the acknowledgement number.
     */
    public static int getAcknowledgementNumber(byte[] packetWithHeader) {
        return (((packetWithHeader[8] & 0xff) << 24) | ((packetWithHeader[9] & 0xff) << 16) | ((packetWithHeader[10] & 0xff) << 8) | (packetWithHeader[11] & 0xff));
    }

    /**
     * Get the information on the flags that are set from the header.
     *
     * @param packetWithHeader is the packet that includes the header.
     * @return the flag(s) that is/are set.
     */
    public static int getFlag(byte[] packetWithHeader) {
        return (((packetWithHeader[12] & 0xff) << 8) | (packetWithHeader[13] & 0xff));
    }

    /**
     * Get the checksum that is calculated by the sender from the header.
     *
     * @param packetWithHeader is the packet that includes the header.
     * @return the inverse checksum as calculated by the sender.
     */
    public static int getChecksum(byte[] packetWithHeader) {
        return (((packetWithHeader[14] & 0xff) << 8) | (packetWithHeader[15] & 0xff));
    }
}
