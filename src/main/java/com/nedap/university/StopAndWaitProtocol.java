package com.nedap.university;

public class StopAndWaitProtocol {
    public static final int RTT = 0; // todo check wat RTT is


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
