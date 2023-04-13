package com.nedap.university;

import java.util.Timer;
import java.util.TimerTask;

public class PacketTimer {
    Timer timer;
    public static final int RTT = 0; // todo check wat RTT is in milliseconds

    /**
     * Start timer for TTL - if acknowledgement is not received before, packet needs to be sent again.
     *
     * @param milliseconds is the number of milliseconds after which the timer needs to expire.
     */
    public void startTimer(int milliseconds) {
        timer = new java.util.Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                System.out.println("Timer expired and no acknowledgement received -- send packet again!");
                // boolean goed zetten // todo nadenken hoe en wat!
                // waitingForAck = false;
            }
        };
        timer.schedule(task, milliseconds);
    }
}
