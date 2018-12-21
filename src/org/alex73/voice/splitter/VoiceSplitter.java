package org.alex73.voice.splitter;

import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.function.Predicate;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.undo.UndoManager;

import org.alex73.voice.splitter.ops.OperationMergeAudio;
import org.alex73.voice.splitter.ops.OperationSplitAudio;
import org.alex73.voice.splitter.stat.StatLength;
import org.alex73.voice.splitter.storage.Audio;
import org.alex73.voice.splitter.storage.AudioFile;
import org.alex73.voice.splitter.storage.Export;
import org.alex73.voice.splitter.storage.Part;
import org.alex73.voice.splitter.storage.Storage;

public class VoiceSplitter {
    public static final ResourceBundle bundle = java.util.ResourceBundle.getBundle("messages");

    static public VoiceSplitterFrame frame;
    static public File dir = new File(".");
    static Audio currentAudio;
    static AudioPanel audio;
    static UndoManager undo = new UndoManager();
    static boolean wasChanged;

    public static void main(String[] args) throws Exception {
        UIManager.getDefaults().addResourceBundle("messages");
        frame = new VoiceSplitterFrame();

        frame.panelList.setLayout(new VerticalListLayout(new Insets(3, 3, 3, 3)));
        frame.scroll.getVerticalScrollBar().setUnitIncrement(20);

        frame.setBounds(100, 100, 1000, 800);
        frame.setVisible(true);
        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        Storage.open(dir);

        audio = new AudioPanel();
        frame.panelAudio.add(audio);
        frame.panelAudio.validate();

        frame.miStat.addActionListener(miStat);
        frame.miSave.addActionListener(miSave);
        frame.miExport.addActionListener(miExport);
        frame.miUndo.addActionListener((e) -> {
            if (undo.canUndo()) {
                undo.undo();
            }
        });
        frame.miRedo.addActionListener((e) -> {
            if (undo.canRedo()) {
                undo.redo();
            }
        });
        frame.cbFiles.addActionListener((e) -> {
            currentAudio = ((AudioFileComboItem) frame.cbFiles.getSelectedItem()).getAudio();
            showFile();
        });
        frame.bDec.addActionListener((e) -> {
            audio.pixelsPerSecond *= 0.8;
            audio.draw();
        });
        frame.bInc.addActionListener((e) -> {
            audio.pixelsPerSecond /= 0.8;
            audio.draw();
        });

        frame.bPlay.addActionListener(play);
        frame.bPlay.registerKeyboardAction(play, "play", KeyStroke.getKeyStroke("F1"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        frame.bPrev.addActionListener(prev);
        frame.bPrev.registerKeyboardAction(prev, "prev", KeyStroke.getKeyStroke("F2"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        frame.bNext.addActionListener(next);
        frame.bNext.registerKeyboardAction(next, "next", KeyStroke.getKeyStroke("F3"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        frame.bSplit.addActionListener(split);
        frame.bSplit.registerKeyboardAction(split, "split", KeyStroke.getKeyStroke("F4"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        frame.bMerge.addActionListener(merge);
        frame.bMerge.registerKeyboardAction(merge, "merge", KeyStroke.getKeyStroke("shift F4"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        frame.menuEdit.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                frame.miUndo.setEnabled(undo.canUndo());
                frame.miUndo.setText(undo.getUndoPresentationName());
                frame.miRedo.setEnabled(undo.canRedo());
                frame.miRedo.setText(undo.getRedoPresentationName());
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveChangedText();
                if (wasChanged) {
                    int a = JOptionPane.showConfirmDialog(frame, bundle.getString("EXIT_CONFIRMATION_TEXT"),
                            bundle.getString("EXIT_CONFIRMATION_TITLE"), JOptionPane.WARNING_MESSAGE);
                    if (a != JOptionPane.OK_OPTION) {
                        return;
                    }
                }
                System.exit(0);
            }
        });
        frame.bForceSplit.addActionListener(e->{
            // split exactly at the 'place' time
            if (audio.place > 0) {
                OperationSplitAudio op1 = new OperationSplitAudio(audio.a, audio.p,
                        audio.place + VoiceSplitter.audio.getStartTime());
                VoiceSplitter.undo.addEdit(op1);
                VoiceSplitter.wasChanged = true;
            }
        });

        show();
    }

    static ActionListener play = (e) -> {
        Player.selected();
    };

    static ActionListener prev = (e) -> {
        int idx = getPanelIndex(pp -> pp.part == audio.p);
        if (idx > 0) {
            PartPanel prev = (PartPanel) frame.panelList.getComponent(idx - 1);
            setActivePanel(prev);
        }
    };
    static ActionListener next = (e) -> {
        int idx = getPanelIndex(pp -> pp.part == audio.p);
        if (idx + 1 < frame.panelList.getComponentCount()) {
            PartPanel next = (PartPanel) frame.panelList.getComponent(idx + 1);
            setActivePanel(next);
        }
    };
    static ActionListener split = (e) -> {
        int idx = getPanelIndex(pp -> pp.part == audio.p);
        if (idx + 1 < frame.panelList.getComponentCount()) {
            PartPanel curr = (PartPanel) frame.panelList.getComponent(idx);
            PartPanel next = (PartPanel) frame.panelList.getComponent(idx + 1);
            int c = curr.textArea.getCaretPosition();
            String moveText = curr.textArea.getText().substring(c);
            next.textArea.setText(moveText + " " + next.textArea.getText());
            next.textArea.setCaretPosition(0);
            curr.textArea.setText(curr.textArea.getText().substring(0, c));
        }
    };
    static ActionListener merge = (e) -> {
        int idx = getPanelIndex(pp -> pp.part == audio.p);
        if (idx + 1 < frame.panelList.getComponentCount()) {
            PartPanel curr = (PartPanel) frame.panelList.getComponent(idx);
            PartPanel next = (PartPanel) frame.panelList.getComponent(idx + 1);
            String moveText = next.textArea.getText();
            curr.textArea.setText(curr.textArea.getText() + ' ' + moveText);

            OperationMergeAudio op = new OperationMergeAudio(audio.a, curr.part, next.part);
            VoiceSplitter.undo.addEdit(op);
            VoiceSplitter.wasChanged = true;
        }
    };

    static void show() {
        AudioFileComboItem[] c = new AudioFileComboItem[Storage.all.getFiles().size()];
        for (int i = 0; i < c.length; i++) {
            c[i] = new AudioFileComboItem(Storage.all.getFiles().get(i));
        }
        frame.cbFiles.setModel(new DefaultComboBoxModel<AudioFileComboItem>(c));
        frame.cbFiles.setSelectedIndex(0);
        undo.discardAllEdits();
    }

    static void showFile() {
        Player.stop();
        saveChangedText();
        frame.panelList.removeAll();
        try {
            AudioFile af = new AudioFile(new File(Storage.getStoreDir(), currentAudio.getName()).toPath());
            audio.setFile(currentAudio, af);

            for (Audio a : Storage.all.getFiles()) {
                if (currentAudio == a) {
                    for (Part p : a.getParts()) {
                        PartPanel pp = new PartPanel(af, a, p);
                        frame.panelList.add(pp);
                    }
                }
            }
            undo.discardAllEdits();
            frame.scroll.validate();
            frame.scroll.repaint();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(VoiceSplitter.frame,
                    MessageFormat.format(bundle.getString("ERROR_READ_AUDIO"), ex.getMessage()),
                    bundle.getString("ERROR_TITLE"), JOptionPane.ERROR_MESSAGE);
        }
    }

    public static int getPanelIndex(Predicate<PartPanel> p) {
        for (int i = 0; i < frame.panelList.getComponentCount(); i++) {
            PartPanel pp = (PartPanel) frame.panelList.getComponent(i);
            if (p.test(pp)) {
                return i;
            }
        }
        return -1;
    }

    public static void setActivePanel(PartPanel p) {
        p.textArea.requestFocus();
        Rectangle r = p.getBounds();
        r.y -= r.height + 10;
        r.height *= 3;
        r.height += 20;
        frame.panelList.scrollRectToVisible(r);
    }

    static void saveChangedText() {
        // get texts from yellow
        for (int i = 0; i < frame.panelList.getComponentCount(); i++) {
            PartPanel pp = (PartPanel) frame.panelList.getComponent(i);
            if (pp.textWasChanged()) {
                wasChanged = true;
                pp.part.text = pp.textArea.getText();
            }
        }
    }

    static ActionListener miSave = (a) -> {
        saveChangedText();
        undo.discardAllEdits();
        try {
            Storage.save();
            wasChanged = false;
            for (int i = 0; i < frame.panelList.getComponentCount(); i++) {
                PartPanel pp = (PartPanel) frame.panelList.getComponent(i);
                pp.updateChanges();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, MessageFormat.format(bundle.getString("ERROR_SAVE"), ex.getMessage()),
                    bundle.getString("ERROR_TITLE"), JOptionPane.ERROR_MESSAGE);
        }
    };
    static ActionListener miExport = (a) -> {
        Export.start();
    };
    static ActionListener miStat = (a) -> {
        StatLength.showStat();
    };
}
