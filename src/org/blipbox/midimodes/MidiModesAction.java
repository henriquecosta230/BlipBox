package org.blipbox.midimodes;

import rwmidi.*;
import org.blipbox.*;
import org.apache.log4j.Logger;

public class MidiModesAction extends AbstractBlipAction {
    private static final Logger log = Logger.getLogger(MidiModesAction.class);
    private BlipBox blipbox;
    private MidiOutput output;
    private MidiMode mode;
    private int channel = 0;

    public MidiModesAction(MidiModesBlipBox blipbox){
        this.blipbox = blipbox;
        for(MidiOutputDevice device: RWMidi.getOutputDevices())
            log.info("MIDI output device: "+device.getName());
        this.output = RWMidi.getOutputDevices()[0].createOutput();
        log.info("Using MIDI output "+output.getName());
        mode = getMode(0);
    }

    public void tick(long clock){
        if(clock % 20 == 0){
            blipbox.fade();
            blipbox.setLed(9, mode.getModeId(), 0xff);
            if(blipbox.isScreenPressed())
                blipbox.setLed(blipbox.getX(0, 10), blipbox.getY(8, 0), 0xff);
        }
        mode.tick(clock);
    }

    public void tap(Position pos){
        if(blipbox.getX(0, 10) == 9)
            changeMode(blipbox.getY(8, 0));
        else
            mode.tap(pos);
    }

    public void taptap(Position pos){
        if(blipbox.getX(0, 10) != 9)
            mode.taptap(pos);
    }

    public void release(Position pos){
        if(blipbox.getX(0, 10) != 9)
            mode.release(pos);
    }

    public void drag(Position from, Position to){
        if(blipbox.getX(0, 10) != 9)
            mode.drag(from, to);
    }

    public void position(Position pos){
        if(blipbox.getX(0, 10) != 9)
            mode.position(pos);
    }

    public void changeMode(int modeId){
        if(mode.getModeId() != modeId){
            log.debug("Setting mode: "+modeId);
            mode.stop();
            mode = getMode(modeId);
            mode.start();
        }
    }

    public MidiMode getMode(int modeId){
        MidiMode mode;
        switch(modeId){
        case 0:
        case 1:
            mode = new NotePlayerMode();
            break;
        case 2:
        case 3:
            mode = new ControlChangeMode(18, 19);
            break;
        default:
            mode = new ButtonMode();
        }
        mode.setModeId(modeId);
        mode.setMidiOutput(output);
        mode.setMidiChannel(channel);
        mode.setBlipBox(blipbox);
        return mode;
    }
}