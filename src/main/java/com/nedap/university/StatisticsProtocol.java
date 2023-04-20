package com.nedap.university;

public class StatisticsProtocol {
    public static int optimalNumberOfPackets;
    public static int packetCount;
    public static long startTime;
    public static long endTime;
    public static int FACTORNANOTOMILLI = 1000000;

    /**
     * Calculate the progress of transmission.
     *
     * @param part  is the part that is being transmitted (number of packets sent in case of sending, size of received
     *              data in case of receiving).
     * @param total is the total part to be transmitted (total number of packets in case of sending, size of total data
     *              to be received in case of receiving).
     * @return the percentage that is being transmitted.
     */
    public static long calculateProgress(long part, long total) {
        return ((part * 100) / total);
    }

    /**
     * Set the optimal number of packets to the minimal number of packets that is needed to receive the total file.
     *
     * @param totalNumberOfPackets is the total number of packets that minimally required to receive the total file.
     */
    public static void setOptimalNumberOfPackets(int totalNumberOfPackets) {
        StatisticsProtocol.optimalNumberOfPackets = totalNumberOfPackets;
    }

    /**
     * Set the start time of file transmission to the time this timer is started.
     */
    public static void startTimer() {
        StatisticsProtocol.startTime = System.nanoTime();
    }

    /**
     * Set the end time of file transmission to the time this timer is stopped.
     */
    public static void stopTimer() {
        StatisticsProtocol.endTime = System.nanoTime();
    }

    /**
     * Add the packet count by one.
     */
    public static void addPacket() {
        packetCount++;
    }

    /**
     * Reset the packetCount to 0.
     */
    public static void resetPacketCount() {
        packetCount = 1;
    }

    /**
     * Calculate the total file transmission time.
     *
     * @return the total file transmission time.
     */
    public static long calculateTotalTransmissionTimeInMs() {
        return ((endTime - startTime)/FACTORNANOTOMILLI);
    }

    /**
     * Calculate the file transmission time per packet.
     *
     * @return the file transmission time per packet.
     */
    public static long getTransmissionTimePerPacket() {
        long totalTransmissionTime = calculateTotalTransmissionTimeInMs();
        return totalTransmissionTime / packetCount;
    }

    /**
     * Calculate the total number of retransmitted packets.
     *
     * @return the total number of retransmitted packets.
     */
    public static int getNumberOfRetransmittedPackets() {
        return (packetCount - optimalNumberOfPackets);
    }

    /**
     * Calculate the optimal file transmission time.
     *
     * @return the optimal file transmission time.
     */
    public static long getOptimalTransmissionTime() {
        return (getTransmissionTimePerPacket() * optimalNumberOfPackets)+1; // add one to adjust for rounding down.
    }

    public static String statisticsInMessage() {
        return ("   The total transmission time of the file was " + calculateTotalTransmissionTimeInMs() + " ms.\n" +
                "   The total file could have been sent in an optimal total of " + optimalNumberOfPackets + " packets.\n" +
                "   However, the total number of packets that were needed was " + packetCount + ".\n" +
                "   This indicates a total number of " + getNumberOfRetransmittedPackets() + " retransmitted packets. \n" +
                "   The mean transmission time per packet was " + getTransmissionTimePerPacket() + " ms per packet.\n" +
                "   This indicates that the transmission of the total packet could optimally have taken place in " + getOptimalTransmissionTime() + " ms.\n");
    }
}
