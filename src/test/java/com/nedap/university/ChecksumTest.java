package com.nedap.university;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ChecksumTest {

    @Test
    public void calculateSimpleChecksum() {
        int totalFileSize = 1;
        int sequenceNumber = 1;
        int acknowledgementNumber = 1;
        int flag = 1;
        int payloadLength = 0;
        int headerSize = 16;
        int sum = totalFileSize + sequenceNumber + acknowledgementNumber + flag + payloadLength + headerSize;
        byte[] packetHeader = PacketProtocol.createHeader(totalFileSize, sequenceNumber, acknowledgementNumber, flag, payloadLength);
        byte[] checksumInput = PacketProtocol.getChecksumInput(packetHeader, payloadLength);
        int inverseResultOfChecksum = PacketProtocol.calculateChecksum(checksumInput);
        assertEquals(~sum, inverseResultOfChecksum);
    }

    @Test
    public void calculateChecksumWithLargeNumber() {
        int totalFileSize = 1;
        int sequenceNumber = 32768; // = 2^15;
        int acknowledgementNumber = 1;
        int flag = 1;
        int payloadLength = 32768; // = 2^15;
        int sum = 20;

        byte[] packetHeader = PacketProtocol.createHeader(totalFileSize, sequenceNumber, acknowledgementNumber, flag, payloadLength);
        byte[] checksumInput = PacketProtocol.getChecksumInput(packetHeader, payloadLength);
        int inverseResultOfChecksum = PacketProtocol.calculateChecksum(checksumInput);
        assertEquals(~sum, inverseResultOfChecksum);
    }

    @Test
    public void testChecksumCheckWithSimpleInput() {
        int totalFileSize = 1;
        int sequenceNumber = 1;
        int acknowledgementNumber = 1;
        int flag = 1;
        int payloadLength = 0;
        byte[] packetHeader = PacketProtocol.createHeader(totalFileSize, sequenceNumber, acknowledgementNumber, flag, payloadLength);
        int inverseResultOfChecksum = PacketProtocol.getChecksum(packetHeader);
        assertEquals(inverseResultOfChecksum, PacketProtocol.getChecksum(packetHeader));
        assertTrue(DataIntegrityCheck.isChecksumCorrect(packetHeader, payloadLength));
    }

    @Test
    public void testChecksumCheckWithRandomNumbers() {
        int totalFileSize = 500;
        int sequenceNumber = 32768;
        int acknowledgementNumber = 765;
        int flag = 1;
        int payloadLength = 32768;
        byte[] packetHeader = PacketProtocol.createHeader(totalFileSize, sequenceNumber, acknowledgementNumber, flag, payloadLength);
        assertTrue(DataIntegrityCheck.isChecksumCorrect(packetHeader, payloadLength));
    }
}
