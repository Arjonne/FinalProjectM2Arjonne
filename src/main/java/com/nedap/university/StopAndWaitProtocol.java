package com.nedap.university;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class StopAndWaitProtocol {
    Timer timer;
    public static final int RTT = 0; // todo check wat RTT is in milliseconds

    /**
     * Start timer for TTL - if acknowledgement is not received before, packet needs to be sent again.
     *
     * @param milliseconds is the number of milliseconds after which the timer needs to expire.
     */
    public void startTimer(int milliseconds) {
        timer = new Timer();
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

    /**
     * Send a packet with file data and wait for acknowledgement to be received.
     *
     * @param fileName    is the name of the file.
     * @param socket      is the socket via which the data can be sent and received.
     * @param totalPacket is the data of the total packet (including header).
     */
    public void send(String fileName, DatagramSocket socket, DatagramPacket totalPacket) {
        System.out.println("Sending...");
        boolean finished = false;
        boolean waitingForAck = true;
        while (!finished) {
            try {
                System.out.println("Sending packet " + fileName);
                socket.send(totalPacket);
                startTimer(RTT * 4);
                while (waitingForAck) {
                    DatagramPacket ackPacket = new DatagramPacket(); // todo packet creeeren en sturen.
                    socket.receive(ackPacket);
                    byte[] acknowledgement = ackPacket.getData();
                    // check if acknowledgement is actually received, and if ACK flag is set:
                    if ((acknowledgement != null) && acknowledgement[8] == PacketProtocol.ACK) {
                        timer.cancel();
                        System.out.println("Acknowledgement is successfully received.");
                        waitingForAck = false;
                        finished = true;
                    } else {
                        Thread.sleep(10); // todo check of nodig is!
                    }
                }
            } catch (IOException e) {
                System.out.println("Not able to send or receive data.");
            } catch (InterruptedException e) {
                waitingForAck = false;
                finished = true;
            }
        }
    }

    /**
     * Receive a packet with file data and send an acknowledgement as response.
     *
     * @param socket is the socket via which the data can be sent and received.
     */
    public void receive(DatagramSocket socket) {
        System.out.println("Receiving...");
        byte[] fileData = new byte[0];
        boolean stopReceiving = false;
        while (!stopReceiving) {
            try {
                DatagramPacket fileDataPacket = new DatagramPacket(); // todo packet creeeren
                socket.receive(fileDataPacket);
                byte[] data = fileDataPacket.getData();
                if (PacketProtocol.isChecksumCorrect(data)) {
                    System.out.println("Packet successfully and correctly received."); // todo evt informatie toevoegen
                    DatagramPacket acknowledgement = new DatagramPacket(); // todo create ack
                    socket.send(acknowledgement);
                    System.out.println("Acknowledgement is sent");
                    int oldLength = fileData.length;
                    int packetDataLength = data.length - PacketProtocol.HEADER_SIZE;
                    fileData = Arrays.copyOf(fileData, oldLength + packetDataLength);
                    System.arraycopy(data, PacketProtocol.HEADER_SIZE, fileData, oldLength, packetDataLength);
                } else {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        stopReceiving = true;
                    }
                }
            } catch (IOException e) {
                System.out.println("Not able to send or receive data.");
            }
        }
    }

    // voorbeeld voor uploaden (downloaden werkt het zelfde maar andere kant uit!)

    // 1. commando upload komt binnen via tui
    // 2. pakket wordt gecreeerd met juiste flag
    // 3. server ontvangt pakket en weet wat te doen (receiven)
    // 4. server stuurt ack (bevestiging dat verzoek is binnengekomen)
    // 5. als client ack heeft ontvangen, begint deze met sturen van de file
    // 6. server probeert file te ontvangen
    //      6.1 als file ontvangen is, wordt checksum gecheckt. als deze klopt, wort ack gestuurd, anders niet!!
    //      6.2 als file niet ontvangen is, wordt er ook niets gestuurd.
    // 7. als TTL is verlopen, stuurt client dezelfde file opnieuw.
}
