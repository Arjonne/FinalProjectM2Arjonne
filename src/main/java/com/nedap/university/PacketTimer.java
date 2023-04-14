package com.nedap.university;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Represents the functions for creating and setting a timer for receiving acknowledgements.
 */
public class PacketTimer {
    private Timer timer;

    public PacketTimer(Timer timer) {
        this.timer = timer;
    }

    /**
     * Start the timer and retransmit a packet when the timer expires.
     *
     * @param socket is the socket via which a packet needs to be retransmitted if the timer expires.
     * @param packet is the packet that needs to be retransmitted if the timer expires.
     */
    public void startTimer(DatagramSocket socket, DatagramPacket packet) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                System.out.println("Timer expired and no acknowledgement received -- send last packet again!");
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    throw new RuntimeException(e); //todo
                }
            }
        };
        timer.schedule(task, PacketProtocol.RTT);
    }

    /**
     * Stop the timer (in case the acknowledgement was received in time).
     */
    public void stopTimer() {
        timer.cancel();
    }
}
