package com.nedap.university;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Represents the protocol for building and sending packets between server and client.
 */
public final class PacketProtocol {
    public static final String PI_ADDRESS = "172.16.1.1";
    public static final int PI_PORT = 9090;
    // variables for own header:
    public static final int HEADER_SIZE = 12;
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
    public static byte[] createHeader(int sequenceNumber, int flag) {
        int acknowledgementNumber = sequenceNumber + 1;
        byte[] header = new byte[HEADER_SIZE];
        header[0] = (byte) (sequenceNumber >> 24);
        header[1] = (byte) ((sequenceNumber >> 16) & 0xff);
        header[2] = (byte) ((sequenceNumber >> 8) & 0xff);
        header[3] = (byte) (sequenceNumber & 0xff);
        header[4] = (byte) (acknowledgementNumber >> 24);
        header[5] = (byte) ((acknowledgementNumber >> 16) & 0xff);
        header[6] = (byte) ((acknowledgementNumber >> 8) & 0xff);
        header[7] = (byte) (acknowledgementNumber & 0xff);
        header[8] = (byte) flag;
        header[9] = 0; // room for more flags if necessary
        // two bytes for checksum: //todo implement checksum
        header[10] = 0; // checksum >> 8;
        header[11] = 0; // checksum & 0xff;
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
    public static byte[] addHeaderToData(int sequenceNumber, int flag, byte[] fileData) {
        int totalPacketSize = HEADER_SIZE + fileData.length;
        byte[] header = createHeader(sequenceNumber, flag);
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
}
