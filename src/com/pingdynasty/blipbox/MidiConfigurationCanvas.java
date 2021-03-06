package com.pingdynasty.blipbox;

import java.util.Map;
import java.util.HashMap;
import javax.swing.JFrame;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.*;
import java.awt.event.*;
import javax.swing.JPanel;
import javax.swing.*;
import javax.swing.event.*;
import javax.sound.midi.*;
import org.apache.log4j.Logger;
import com.pingdynasty.midi.*;
import com.pingdynasty.midi.SpringUtilities;
import net.miginfocom.swing.MigLayout;

public class MidiConfigurationCanvas extends JPanel {
    private static final Logger log = Logger.getLogger(MidiOutputEventHandler.class);
    private MidiOutputEventHandler eventhandler;
    private BlipBox sender;

    private static final byte Y_NOTES_CC = 18;
    private static final byte X_CC = 20;
    private static final byte Y_CC = 21;
    private static final byte T_CC = 19;
    private static final byte POT_CC = 1;
    private static final byte BUTTON_CC = 22;

    private static final String[] controlTypes = 
    { "Unassigned", "Control Change", "NRPN", "Pitch Bend", "Basenote", "Scale", "Mode Change" };

    public class ModeConfigurationPanel extends JPanel {
        private String mode;
        private NotePlayerConfigurationPanel notes;
        private CombinedParameterConfigurationPanel parameters;

        public ModeConfigurationPanel(String mode){
            this.mode = mode;
            notes = new NotePlayerConfigurationPanel();
            parameters = new CombinedParameterConfigurationPanel();
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(notes);
            add(Box.createRigidArea(new Dimension(10,0)));
            add(parameters);
        }

        public String getOperationMode(){
            return mode;
        }

        public void setup(SensorType sensor, String type, String mode){
            parameters.setup(sensor, type, mode);
        }

        public void setup(SensorType sensor, String type, int channel, int cc, int min, int max){
            parameters.setup(sensor, type, channel, cc, min, max);
        }

        public void setup(boolean doPlay, boolean doPb, boolean doAt){
            notes.setup(doPlay, doPb, doAt);
        }

        public class ParameterConfigurationPanel extends JPanel {
            private String name;
            private SensorType type;
            private JComboBox typeList;
            private JSpinner channel;
            private JSpinner cc;
            private JSpinner min;
            private JSpinner max;
            private JComboBox modeList;

            public ParameterConfigurationPanel(String name, SensorType type){
                this.type = type;
                this.name = name;
                setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

                add(new Label(name));
                typeList = new JComboBox(controlTypes);
                typeList.addActionListener(new AbstractAction(){
                        public void actionPerformed(ActionEvent e) {
                            setType((String)typeList.getSelectedItem());
                            configure();
                        }
                    });
                add(typeList);

                add(new Label("channel"));
                channel = new JSpinner(new SpinnerNumberModel(1, 1, 16, 1));
                channel.setValue(1);
                channel.addChangeListener(new ChangeListener(){
                        public void stateChanged(ChangeEvent e){
                            configure();
                        }
                    });
                add(channel);

                add(new Label("cc"));
                cc = new JSpinner(new SpinnerNumberModel(21, 1, 127, 1));
                cc.setValue(21);
                cc.addChangeListener(new ChangeListener(){
                        public void stateChanged(ChangeEvent e){
                            configure();
                        }
                    });
                add(cc);

                add(new Label("min"));
                min = new JSpinner();
                min.addChangeListener(new ChangeListener(){
                        public void stateChanged(ChangeEvent e){
                            configure();
                        }
                    });
                add(min);

                add(new Label("max"));
                max = new JSpinner();
                max.setValue(127);
                max.addChangeListener(new ChangeListener(){
                        public void stateChanged(ChangeEvent e){
                            configure();
                        }
                    });
                add(max);

                JButton button = new JButton("invert");
                button.addActionListener(new AbstractAction(){
                        public void actionPerformed(ActionEvent e) {
                            int saved = (Integer)min.getValue();
                            min.setValue(max.getValue());
                            max.setValue(saved);
                        }
                    });
                add(button);

                modeList = new JComboBox(eventhandler.getModeNames());
                modeList.addActionListener(new AbstractAction(){
                        public void actionPerformed(ActionEvent e) {
                            configure();
                        }
                    });
                add(modeList);
            }

            public void setup(String type, int channel, int cc, int min, int max){
//                 setType(type);
                typeList.setSelectedItem(type);
                this.channel.setValue(channel);
                this.cc.setValue(cc);
                this.min.setValue(min);
                this.max.setValue(max);
            }

            public void setup(String type, String mode){
                typeList.setSelectedItem(type);
                modeList.setSelectedItem(mode);
            }

            public void configure(){
                String type = (String)typeList.getSelectedItem();
                if("Control Change".equals(type)){
                    eventhandler.configureControlChange(getOperationMode(), this.type, 
                                                   (Integer)channel.getValue(), (Integer)cc.getValue(), 
                                                   (Integer)min.getValue(), (Integer)max.getValue());
                }else if("NRPN".equals(type)){
                    eventhandler.configureNRPN(getOperationMode(), this.type, 
                                               (Integer)channel.getValue(), (Integer)cc.getValue(), 
                                               (Integer)min.getValue(), (Integer)max.getValue());
                }else if("Pitch Bend".equals(type)){
                    eventhandler.configurePitchBend(getOperationMode(), this.type, 
                                               (Integer)channel.getValue(), 
                                               (Integer)min.getValue(), (Integer)max.getValue());
                }else if("Basenote".equals(type)){
                    eventhandler.configureBaseNoteChange(getOperationMode(), this.type, 
                                                    (Integer)min.getValue(), (Integer)max.getValue());
                }else if("Scale".equals(type)){
                    eventhandler.configureScaleChange(getOperationMode(), this.type);
                }else if("Mode Change".equals(type)){
                    eventhandler.configureModeChange(getOperationMode(), this.type, 
                                                   (String)modeList.getSelectedItem());
                }else if("Unassigned".equals(type)){
                    eventhandler.configureUnassigned(getOperationMode(), this.type);
                }
            }

            public void setType(String type){
                if("Control Change".equals(type)){
                    channel.setEnabled(true);
                    cc.setEnabled(true);
                    min.setEnabled(true);
                    max.setEnabled(true);
                    min.setValue(0);
                    max.setValue(127);
                    modeList.setEnabled(false);
                }else if("NRPN".equals(type)){
                    channel.setEnabled(true);
                    cc.setEnabled(true);
                    min.setEnabled(true);
                    max.setEnabled(true);
                    min.setValue(0);
                    max.setValue(127);
                    modeList.setEnabled(false);
                }else if("Pitch Bend".equals(type)){
                    channel.setEnabled(true);
                    cc.setEnabled(false);
                    min.setEnabled(true);
                    max.setEnabled(true);
                    min.setValue(-8192);
                    max.setValue(8191);
                    modeList.setEnabled(false);
                }else if("Basenote".equals(type)){
                    channel.setEnabled(false);
                    cc.setEnabled(false);
                    min.setEnabled(true);
                    max.setEnabled(true);
                    min.setValue(0);
                    max.setValue(127);
                    modeList.setEnabled(false);
                }else if("Scale".equals(type)){
                    channel.setEnabled(false);
                    cc.setEnabled(false);
                    min.setEnabled(false);
                    max.setEnabled(false);
                    modeList.setEnabled(false);
                }else if("Mode Change".equals(type)){
                    channel.setEnabled(false);
                    cc.setEnabled(false);
                    min.setEnabled(false);
                    max.setEnabled(false);
                    modeList.setEnabled(true);
                }else if("Unassigned".equals(type)){
                    channel.setEnabled(false);
                    cc.setEnabled(false);
                    min.setEnabled(false);
                    max.setEnabled(false);
                    modeList.setEnabled(false);
                }
            }
        }

        public class NotePlayerConfigurationPanel extends JPanel {
            private JCheckBox play;
            private JCheckBox pb;
            private JCheckBox at;
            private boolean doPlay = true;
            private boolean doAt, doPb;
            private JComboBox scale;
            private JSpinner basenote;

            public NotePlayerConfigurationPanel(){
                setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
                JPanel panel = new JPanel(new MigLayout());
                add(panel);

                play = new JCheckBox("Play notes", true);
                play.addActionListener(new AbstractAction(){
                        public void actionPerformed(ActionEvent e) {
                            doPlay = !doPlay;
                            if(doPlay){
                                pb.setEnabled(true);
                                at.setEnabled(true);
                            }else{
                                pb.setEnabled(false);
                                at.setEnabled(false);
                            }
                            configure();
                        }
                    });
                panel.add(play, "wrap");

                pb = new JCheckBox("Pitch Bend", false);
                pb.addActionListener(new AbstractAction(){
                        public void actionPerformed(ActionEvent e) {
                            doPb = !doPb;
                            configure();
                        }
                    });
                panel.add(pb, "wrap");

                at = new JCheckBox("Aftertouch", false);
                at.addActionListener(new AbstractAction(){
                        public void actionPerformed(ActionEvent e) {
                            doAt = !doAt;
                            configure();
                        }
                    });
                panel.add(at, "wrap");

                panel = new JPanel(new MigLayout());
                add(panel);

                panel.add(new Label("Follow Mode"), "label");
                JComboBox box = new JComboBox(sender.getFollowModes());
                box.setSelectedItem(eventhandler.getConfigurationMode(getOperationMode()).getFollowMode());
                box.addActionListener(new AbstractAction(){
                        public void actionPerformed(ActionEvent e) {
                            JComboBox box = (JComboBox)e.getSource();
                            String name = (String)box.getSelectedItem();
                            eventhandler.getConfigurationMode(getOperationMode()).setFollowMode(name);
                            sender.setFollowMode(name);
                        }
                    });
                panel.add(box, "wrap");

                panel.add(new Label("Scale"), "label");
                scale = new JComboBox(eventhandler.getScaleMapper().getScaleNames());
                scale.setSelectedItem(eventhandler.getCurrentScale());
                scale.addActionListener(new AbstractAction(){
                        public void actionPerformed(ActionEvent e) {
                            String scalename = (String)scale.getSelectedItem();
                            eventhandler.getScaleMapper(getOperationMode()).setScale(scalename);
                        }
                    });
                panel.add(scale, "wrap");

                panel.add(new Label("Basenote"), "label");
                basenote = new JSpinner(new SpinnerNumberModel(1, 1, 127, 1));
                basenote.setValue(eventhandler.getBaseNote());
                basenote.addChangeListener(new ChangeListener(){
                        public void stateChanged(ChangeEvent e){
                            Integer value = (Integer)basenote.getValue();
                            eventhandler.setBaseNote(getOperationMode(), value);
                        }
                    });
                panel.add(basenote, "wrap");

            }

            public void configure(){
                eventhandler.configureNotePlayer(getOperationMode(), doPlay, doPb, doAt);
            }

            public void setup(boolean doPlay, boolean doPb, boolean doAt){
                play.setSelected(doPlay);
                this.doPlay = doPlay;
                pb.setSelected(doPb);
                this.doPb = doPb;
                at.setSelected(doAt);
                this.doAt = doAt;
                configure();
            }
        }

        public class CombinedParameterConfigurationPanel extends JPanel {
            Map<SensorType, ParameterConfigurationPanel> panels = new HashMap<SensorType, ParameterConfigurationPanel>();

            public CombinedParameterConfigurationPanel(){
                setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
                ParameterConfigurationPanel panel = new ParameterConfigurationPanel("X parameter", SensorType.X_SENSOR);
                panels.put(SensorType.X_SENSOR, panel);
                add(panel);
                add(Box.createRigidArea(new Dimension(10,0)));

                panel = new ParameterConfigurationPanel("Y parameter", SensorType.Y_SENSOR);
                panels.put(SensorType.Y_SENSOR, panel);
                add(panel);
                add(Box.createRigidArea(new Dimension(10,0)));

                panel = new ParameterConfigurationPanel("Knob", SensorType.POT_SENSOR);
                panels.put(SensorType.POT_SENSOR, panel);
                add(panel);
                add(Box.createRigidArea(new Dimension(10,0)));

                panel = new ParameterConfigurationPanel("Button", SensorType.BUTTON1_SENSOR);
                panels.put(SensorType.BUTTON1_SENSOR, panel);
                add(panel);
                add(Box.createRigidArea(new Dimension(10,0)));
            }

            public void setup(SensorType sensor, String type, String mode){
                ParameterConfigurationPanel panel = panels.get(sensor);
                if(panel == null)
                    throw new RuntimeException("No configuration available for sensor type "+sensor);
                panel.setup(type, mode);
            }

            public void setup(SensorType sensor, String type, int channel, int cc, int min, int max){
                ParameterConfigurationPanel panel = panels.get(sensor);
                if(panel == null)
                    throw new RuntimeException("No configuration available for sensor type "+sensor);
                panel.setup(type, channel, cc, min, max);
            }
        }
    }

    private Map<String, ModeConfigurationPanel> modes = new HashMap<String, ModeConfigurationPanel>();

    public MidiConfigurationCanvas(BlipBoxApplication application, 
                                   MidiOutputEventHandler eventhandler, BlipBox sender){
        this.eventhandler = eventhandler;
        this.sender = sender;

        JTabbedPane tabs = new JTabbedPane();

        JPanel control = new BlipBoxControlPanel(application);
        tabs.addTab("Setup", control);

        ModeConfigurationPanel panel = new ModeConfigurationPanel("Cross");
        modes.put(panel.getOperationMode(), panel);
        tabs.addTab("Cross Mode", panel);
        panel = new ModeConfigurationPanel("Criss");
        modes.put(panel.getOperationMode(), panel);
        tabs.addTab("Criss Mode", panel);

        add(tabs);

        setup("Cross", true, false, false);
        setup("Cross", SensorType.X_SENSOR, "Unassigned");
        setup("Cross", SensorType.Y_SENSOR, "Control Change", 1, Y_NOTES_CC, 0, 127);
        setup("Cross", SensorType.POT_SENSOR, "Control Change", 1, POT_CC, 0, 127);
        setup("Cross", SensorType.BUTTON1_SENSOR, "Mode Change", "Criss");

        setup("Criss", false, false, false);
        setup("Criss", SensorType.X_SENSOR, "Control Change", 1, X_CC, 0, 127);
        setup("Criss", SensorType.Y_SENSOR, "Control Change", 1, Y_CC, 0, 127);
        setup("Criss", SensorType.POT_SENSOR, "Control Change", 1, POT_CC, 0, 127);
        setup("Criss", SensorType.BUTTON1_SENSOR, "Mode Change", "Cross");

    }

    public void setup(String mode, SensorType sensor, String type){
        setup(mode, sensor, type, 1, 1, 0, 127);
    }

    public void setup(String mode, SensorType sensor, String type, int channel, int cc, int min, int max){
        ModeConfigurationPanel panel = modes.get(mode);
        panel.setup(sensor, type, channel, cc, min, max);
    }

    public void setup(String mode, SensorType sensor, String type, String toMode){
        ModeConfigurationPanel panel = modes.get(mode);
        panel.setup(sensor, type, toMode);
    }

    public void setup(String mode, boolean doPlay, boolean doPb, boolean doAt){
        ModeConfigurationPanel panel = modes.get(mode);
        panel.setup(doPlay, doPb, doAt);
    }

}