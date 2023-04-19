package com.nedap.university;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test whether bit shifting is correctly performed by creating the header and getting information from the header.
 */
class BitShiftingTest {
    int fileSize;
    int sequenceNumber;
    int acknowledgementNumber;
    int flag;
    String message;
    byte[] messageData;
    byte[] packetWithHeader;

    /**
     * Create a packet with header before byte shifting tests can be performed.
     */
    @BeforeEach
    public void createPacketWithHeader() {
        fileSize = 100;
        sequenceNumber = 12345678;
        acknowledgementNumber = 87654321;
        flag = PacketProtocol.ACK;
        message = "Hello";
        messageData = message.getBytes();
        packetWithHeader = PacketProtocol.createPacketWithHeader(fileSize, sequenceNumber, acknowledgementNumber, flag, messageData);
    }

    /**
     * Test whether the file size that is extracted from the header is the same as the file size that is put in the
     * header by default.
     */
    @Test
    public void getFileSize() {
        int fileSizeFromHeader = PacketProtocol.getFileSizeInPacket(packetWithHeader);
        assertEquals(fileSize, fileSizeFromHeader);
    }

    /**
     * Test whether the sequence number that is extracted from the header is the same as the file size that is put in
     * the header by default.
     */
    @Test
    public void getSequenceNumber() {
        int seqNrFromHeader = PacketProtocol.getSequenceNumber(packetWithHeader);
        assertEquals(sequenceNumber, seqNrFromHeader);
    }

    /**
     * Test whether the acknowledgement number that is extracted from the header is the same as the file size that is
     * put in the header by default.
     */
    @Test
    public void getAcknowledgementNumber() {
        int ackNrFromHeader = PacketProtocol.getAcknowledgementNumber(packetWithHeader);
        assertEquals(acknowledgementNumber, ackNrFromHeader);
    }

    /**
     * Test whether the flag that is extracted from the header is the same as the file size that is put in the
     * header by default.
     */
    @Test
    public void getFlag() {
        int flagFromHeader = PacketProtocol.getFlag(packetWithHeader);
        assertEquals(flag, flagFromHeader);
    }
}