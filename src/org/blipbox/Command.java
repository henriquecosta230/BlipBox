package org.blipbox;

public enum Command {

    FADE                (4, "Decrease LED brightness"),
    BRIGHTEN            (5, "Increase LED brightness"),
    CFG_REQUEST         (6, "Request configuration parameters"),
    CFG_RESET           (7, "Reset configuration"),
    CFG_READ            (8, "Read configuration from EEPROM"),
    CFG_WRITE           (9, "Write configuration to EEPROM"),
    LEDS_STOP           (10, "Stop LED screen updates"),
    LEDS_START          (11, "Start LED screen updates"),
    RESET               (15, "Soft reset the BlipBox");

    public int cid;
    public String name;

    Command(int cid, String name){
        this.cid = cid;
        this.name = name;
    }
}