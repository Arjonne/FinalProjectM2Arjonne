package com.nedap.university;

import java.net.InetAddress;

/**
 * Represents the protocol for building and sending packets between server and client.
 */
public final class PacketProtocol {
    // address and port of Raspberry Pi:
    public static final String PI_ADDRESS = "172.16.1.1";
    public static final int PI_PORT = 9090;
    // as MTU is 1500, use a maximal packet size of 1500:
    public static final int MAX_PACKET_SIZE = 1500;
    // variables for own header:
    public static final int HEADER_SIZE = 12;
    public static final int PSEUDOHEADER_SIZE = 14;
    // flags:
    public static final int SYN = 128;
    public static final int ACK = 64;
    public static final int UPLOAD = 32;
    public static final int DOWNLOAD = 16;
    public static final int REMOVE = 8;
    public static final int REPLACE = 4;
    public static final int LIST = 2;
    public static final int CLOSE = 1;

    /**
     * Creates the protocol.
     */
    private PacketProtocol() {
    }

    /**
     * Create a header for the datagram packet to be able to use sequence numbers and acknowledgements for checking
     * packets, flags for the correct statement and checksum to check for correct data transmission.
     *
     * @param sequenceNumber is the number of the
     * @param flag           is the flag that is being set
     * @return the header as byte array (which is the same format as the datagram packet itself).
     */
    public static byte[] createHeader(int sequenceNumber, int flag, byte[] pseudoheader) {
        int acknowledgementNumber = sequenceNumber + 1;
        byte[] header = new byte[HEADER_SIZE];
        // four bytes for the sequence number:
        header[0] = (byte) (sequenceNumber >> 24);
        header[1] = (byte) ((sequenceNumber >> 16) & 0xff);
        header[2] = (byte) ((sequenceNumber >> 8) & 0xff);
        header[3] = (byte) (sequenceNumber & 0xff);
        // four bytes for the acknowledgement number:
        header[4] = (byte) (acknowledgementNumber >> 24);
        header[5] = (byte) ((acknowledgementNumber >> 16) & 0xff);
        header[6] = (byte) ((acknowledgementNumber >> 8) & 0xff);
        header[7] = (byte) (acknowledgementNumber & 0xff);
        // one byte for the SYN/ACK/UPLOAD/DOWNLOAD/REMOVE/REPLACE/LIST/CLOSE flag(s):
        header[8] = (byte) flag;
        header[9] = 0; // room for more flags if necessary // todo flag for fragmentation and offset etc..?

        // create a new byte array with all information that is needed for the checksum:
        byte[] checksumInput = new byte[(HEADER_SIZE - 2) + PSEUDOHEADER_SIZE];
        // copy all information (except checksum information) from the header into this array:
        System.arraycopy(header, 0, checksumInput, 0, (HEADER_SIZE - 2));
        // additionally, add all information from the pseudoheader into this array:
        System.arraycopy(pseudoheader, 0, checksumInput, (HEADER_SIZE - 2), PSEUDOHEADER_SIZE);

        // two bytes for checksum:
        header[10] = (byte) (calculateChecksum(checksumInput) >> 8);
        header[11] = (byte) (calculateChecksum(checksumInput) & 0xff);
        return header;
    }

    /**
     * Create pseudoheader for a more exaggerated checksum.
     *
     * @param sourceAddress      is the address from which the packet is sent.
     * @param destinationAddress is the address to which the packet is sent.
     * @param sourcePort         is the port the source uses for this connection.
     * @param destinationPort    is the port the destination uses for this connection.
     * @param dataLength         is the total length of the packet, without header.
     * @return the pseudoheader which includes this information.
     */
    public static byte[] createPseudoHeader(InetAddress sourceAddress, InetAddress destinationAddress, int sourcePort, int destinationPort, int dataLength) {
        byte[] pseudoheader = new byte[PSEUDOHEADER_SIZE];
        System.arraycopy(sourceAddress.getAddress(), 0, pseudoheader, 0, 4);
        System.arraycopy(destinationAddress.getAddress(), 0, pseudoheader, 4, 4);
        pseudoheader[8] = (byte) (sourcePort >> 8);
        pseudoheader[9] = (byte) (sourcePort & 0xff);
        pseudoheader[10] = (byte) (destinationPort >> 8);
        pseudoheader[11] = (byte) (destinationPort & 0xff);
        pseudoheader[12] = (byte) (dataLength >> 8);
        pseudoheader[13] = (byte) (dataLength & 0xff);
        return pseudoheader;
    }


    /**
     * Create a new byte array in which the header and actual data to be sent are combined.
     *
     * @param sequenceNumber is the sequence number (which is needed for the header).
     * @param flag           is (/are) the flag(s) that need to be set in the header.
     * @param fileData       is the data of the actual file to be transmitted.
     * @return the byte array of the total packet (combination of header and data).
     */
    public static byte[] createPacketWithHeader(int sequenceNumber, int flag, byte[] fileData, byte[] pseudoheader) {
        int totalPacketSize = HEADER_SIZE + fileData.length;
        byte[] header = createHeader(sequenceNumber, flag, pseudoheader);
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
     * Calculate the checksum.
     *
     * @param checksumInput is the input for the checksum (header information and pseudoheader information).
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
}
