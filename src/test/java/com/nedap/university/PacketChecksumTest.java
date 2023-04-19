package com.nedap.university;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test whether the calculation of the checksum is correctly performed and if the results from the calculated checksum
 * and checksum that is extracted from the header are correctly compared.
 */
public class PacketChecksumTest {

    /**
     * Test whether the calculation of the checksum goes correctly by first performing this calculation with small
     * numbers.
     */
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
        byte[] checksumInput = DataIntegrityCheck.getChecksumInput(packetHeader, payloadLength);
        int inverseResultOfChecksum = DataIntegrityCheck.calculateChecksum(checksumInput);
        assertEquals((~sum & 0xffff), inverseResultOfChecksum);
    }

    /**
     * Test whether the calculation of the checksum goes correctly if the result of the calculation exceeds the space
     * that is reserved for the checksum in the header (which is 2 bytes --> 2^15).
     */
    @Test
    public void calculateChecksumWithLargeNumber() {
        int totalFileSize = 1;
        int sequenceNumber = 32768; // = 2^15;
        int acknowledgementNumber = 1;
        int flag = 1;
        int payloadLength = 32768; // = 2^15;
        int sum = 20; // which was the result of performing the calculation on paper.
        byte[] packetHeader = PacketProtocol.createHeader(totalFileSize, sequenceNumber, acknowledgementNumber, flag, payloadLength);
        byte[] checksumInput = DataIntegrityCheck.getChecksumInput(packetHeader, payloadLength);
        int inverseResultOfChecksum = DataIntegrityCheck.calculateChecksum(checksumInput);
        assertEquals((~sum & 0xffff), inverseResultOfChecksum);
    }

    /**
     * Test whether the methods for calculating the checksum and getting this same checksum from the header (after
     * adding it to it) give the same result (using small numbers).
     */
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

    /**
     * Test whether the methods for calculating the checksum and getting this same checksum from the header (after
     * adding it to it) give the same result (using large numbers that exceed the space that is reserved for the
     * checksum in the header).
     */
    @Test
    public void testChecksumCheckWithRandomNumbers() {
        int totalFileSize = 500;
        int sequenceNumber = 32768; //2^15
        int acknowledgementNumber = 765;
        int flag = 4096;
        int payloadLength = 32768; //2^15
        byte[] packetHeader = PacketProtocol.createHeader(totalFileSize, sequenceNumber, acknowledgementNumber, flag, payloadLength);
        assertTrue(DataIntegrityCheck.isChecksumCorrect(packetHeader, payloadLength));
    }
}
