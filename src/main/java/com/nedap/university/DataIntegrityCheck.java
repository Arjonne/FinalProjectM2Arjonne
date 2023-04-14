package com.nedap.university;

/**
 * Represents the functions for checking whether the received file is the same as the file that is sent.
 */
public class DataIntegrityCheck {

    /**
     * Check if checksum is correct. For this, the checksum that is sent can be compared with the calculated checksum at
     * the receiver side. If the sum of these is equal to two bytes of only ones, the sum is correct (checksum and its
     * inverse should result in only ones).
     *
     * @param packetWithHeader is the total packet in which the inverse of the checksum (as calculated by the sender)
     *                         is available. Besides, the checksum can be calculated over the header of the packet.
     * @return true if these are the same, false if not
     */
    public static boolean isChecksumCorrect(byte[] packetWithHeader) {
        int checksumSent = PacketProtocol.getChecksum(packetWithHeader);
        byte[] checksumInput = PacketProtocol.getChecksumInput(packetWithHeader);
        int checksumCalculated = PacketProtocol.calculateChecksum(checksumInput);
        return checksumSent + checksumCalculated == 0xffff;
    }

    /**
     * Check if file on server and local file are the same by comparing their hash codes.
     *
     * @param hashCodeOfFileSent     is the hash code of the file that is sent.
     * @param hashCodeOfFileReceived is the hash code of the received file.
     * @return true if these hash codes are the same, false if not.
     */
    public static boolean areSentAndReceivedFilesTheSame(int hashCodeOfFileSent, int hashCodeOfFileReceived) {
        return hashCodeOfFileSent == hashCodeOfFileReceived;
    }
}
