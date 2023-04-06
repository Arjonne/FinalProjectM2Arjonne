package com.nedap.university;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Represents the protocol for building and sending packets between server and client.
 */
public final class Protocol {
    // variables for IPv4 header:
    public static final int IPV4_HEADER_SIZE = 20;
    public static final int VERSION = 4;
    public static final int IPV4_HEADER_LENGTH = (IPV4_HEADER_SIZE / 4); // per 4 bytes
    public static final int RTT = 0; // todo check wat RTT is
    public static final int PROTOCOL_NUMBER_UDP = 17;
    public static final int CLIENT_ADDRESS = 0; // todo uitzoeken
    public static final int PI_ADDRESS = 0; // todo uitzoeken

    // variables for UDP header:
    public static final int TCP_HEADER_SIZE = 16;
    public static final int CLIENT_PORT = 0; //todo uitzoeken
    public static final int PI_PORT = 0; // todo uitzoeken

    /**
     * Creates the protocol.
     */
    private Protocol() {
    }

    public byte[] createIPv4Header(String source, int fileLength) {
        byte[] headerIPv4 = new byte[IPV4_HEADER_SIZE];
        headerIPv4[0] = (VERSION << 4) | IPV4_HEADER_LENGTH; //todo check of dit klopt
        // type of service, not in use so set on 0:
        headerIPv4[1] = 0;
        // total packet length including headers:
        headerIPv4[2] = (byte) ((IPV4_HEADER_SIZE + TCP_HEADER_SIZE + fileLength) >> 8);
        headerIPv4[3] = (byte) ((IPV4_HEADER_SIZE + TCP_HEADER_SIZE + fileLength) & 0xff);
        // identification number: // todo
        headerIPv4[4] = 0 >> 8;
        headerIPv4[5] = 0 & 0xff;
        // first three bits of next byte are flags; other five bits are part of offset (together with next full byte)
        headerIPv4[6] = // todo;
                headerIPv4[7] = // todo;
                        // TTL (which is 2x time out; time out is 2x RTT):
                        headerIPv4[8] = (4 * RTT);
        headerIPv4[9] = PROTOCOL_NUMBER_UDP;
        // two bytes for checksum (0 if not in use): //todo
        headerIPv4[10] = 0;
        headerIPv4[11] = 0;
        if (source.equals("client")) {
            // four bytes for source address (= client address):
            headerIPv4[12] = CLIENT_ADDRESS >> 24;
            headerIPv4[13] = (CLIENT_ADDRESS >> 16) & 0xff;
            headerIPv4[14] = (CLIENT_ADDRESS >> 8) & 0xff;
            headerIPv4[15] = CLIENT_ADDRESS & 0xff;
            // four bytes for destination address (= PI address):
            headerIPv4[16] = PI_ADDRESS >> 24;
            headerIPv4[17] = (PI_ADDRESS >> 16) & 0xff;
            headerIPv4[18] = (PI_ADDRESS >> 8) & 0xff;
            headerIPv4[19] = PI_ADDRESS & 0xff;
        } else {
            // four bytes for source address (= PI address):
            headerIPv4[12] = PI_ADDRESS >> 24;
            headerIPv4[13] = (PI_ADDRESS >> 16) & 0xff;
            headerIPv4[14] = (PI_ADDRESS >> 8) & 0xff;
            headerIPv4[15] = PI_ADDRESS & 0xff;
            // four bytes for destination address (= client address):
            headerIPv4[16] = CLIENT_ADDRESS >> 24;
            headerIPv4[17] = (CLIENT_ADDRESS >> 16) & 0xff;
            headerIPv4[18] = (CLIENT_ADDRESS >> 8) & 0xff;
            headerIPv4[19] = CLIENT_ADDRESS & 0xff;
        }
        return headerIPv4;
    }

    /**
     * Create a TCP-like header for the datagram packet.
     *
     * @param source is the source from which the packet is coming (can be either server or client).
     * @return the TCP-like header of the datagram packet.
     */
    public byte[] createTCPHeader(String source) {
        byte[] headerTCP = new byte[TCP_HEADER_SIZE];
        if (source.equals("client")) { // todo juiste manier van aanroepen
            // two bytes for source port (= client port):
            headerTCP[0] = CLIENT_PORT >> 8;
            headerTCP[1] = CLIENT_PORT & 0xff;
            // two bytes for destination port (= PI port):
            headerTCP[2] = PI_PORT >> 8;
            headerTCP[3] = PI_PORT & 0xff;
        } else {
            // two bytes for source port (= PI port):
            headerTCP[0] = PI_PORT >> 8;
            headerTCP[1] = PI_PORT & 0xff;
            // two bytes for destination port (= client port):
            headerTCP[2] = CLIENT_PORT >> 8;
            headerTCP[3] = CLIENT_PORT & 0xff;
        }
        // four bytes for sequence number: // todo impl
        headerTCP[4] = 0;
        headerTCP[5] = 0;
        headerTCP[6] = 0;
        headerTCP[7] = 0;
        // four bytes for ack number: // todo impl
        headerTCP[8] = 0;
        headerTCP[9] = 0;
        headerTCP[10] = 0;
        headerTCP[11] = 0;
        // one byte for header length + four zeroes:
        headerTCP[12] = (byte) (TCP_HEADER_SIZE << 4);
        // one byte for flags:
        headerTCP[13] = 0; // todo hoe?
        // two bytes for checksum (0 if not in use): //todo
        headerTCP[6] = 0;
        headerTCP[7] = 0;
        return headerTCP;
    }

    /**
     * Create the datagram packet
     *
     * @param fileData is the data of the file to be transmitted.
     * @param source   is the source from where the packet is being sent.
     * @return the packet that is being sent including the correct data and the UDP header.
     */
    public DatagramPacket createDatagramPacket(byte[] fileData, String source) {
        byte[] IPv4Header = createIPv4Header(source, fileData.length);
        byte[] TCPHeader = createTCPHeader(source);
        byte[] totalPacket = new byte[IPV4_HEADER_SIZE + TCP_HEADER_SIZE + fileData.length];
        // copy IPv4 header into total packet:
        for (int i = 0; i < IPV4_HEADER_SIZE; i++) {
            totalPacket[i] = IPv4Header[i];
        }
        // copy UDP header into total packet:
        for (int i = IPV4_HEADER_SIZE; i < (IPV4_HEADER_SIZE + TCP_HEADER_SIZE); i++) {
            totalPacket[i] = TCPHeader[(i - IPV4_HEADER_SIZE)];
        }
        // copy data of file into total packet:
        for (int i = (IPV4_HEADER_SIZE + TCP_HEADER_SIZE); i < (IPV4_HEADER_SIZE + TCP_HEADER_SIZE + fileData.length); i++) {
            totalPacket[i] = fileData[i - (TCP_HEADER_SIZE + IPV4_HEADER_SIZE)];
        }
        DatagramPacket packet = new DatagramPacket(totalPacket, (fileData.length + TCP_HEADER_SIZE));
        return packet;
    }

    /**
     * Try to send packet to the connected client / server (depending on which of the two is sending).
     *
     * @param socket from which the packet is being sent.
     * @param packet is the packet that needs to be transmitted.
     */
    public void sendDatagramPacket(DatagramSocket socket, DatagramPacket packet) {
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.out.println("Could not send packet, try again.");
            // throw new RuntimeException(e);
            // todo error handling
        }
    }

    /**
     * Try to receive packet from connected client / server (depending on which of the two is sending).
     *
     * @param socket from which the packet is being sent.
     * @param packet is the packet that needs to be transmitted.
     */
    public void receiveDatagramPacket(DatagramSocket socket, DatagramPacket packet) {
        try {
            socket.receive(packet);
        } catch (IOException e) {
            System.out.println("Could not receive packet, try again.");
            // throw new RuntimeException(e);
            // todo error handling
        }
    }
}
