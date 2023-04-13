package com.nedap.university;

public class DataIntegrityCheck {

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
        int checksumSent = PacketProtocol.getChecksum(packetWithHeader);
        int checksumCalculated = PacketProtocol.calculateChecksum(checksumInput);
        if (checksumSent + checksumCalculated == 0xffff) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if file on server and local file are the same by comparing their hash codes.
     *
     * @param hashCodeOfFileSent     is the hash code of the file that is sent.
     * @param hashCodeOfFileReceived is the hash code of the received file.
     * @return true if these hash codes are the same, false if not.
     */
    public boolean areSentAndReceivedFilesTheSame(int hashCodeOfFileSent, int hashCodeOfFileReceived) {
        return hashCodeOfFileSent == hashCodeOfFileReceived;
    }
}
