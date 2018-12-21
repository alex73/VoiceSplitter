package org.alex73.voice.splitter.ops;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.alex73.voice.splitter.PartPanel;
import org.alex73.voice.splitter.VoiceSplitter;
import org.alex73.voice.splitter.storage.Audio;
import org.alex73.voice.splitter.storage.Part;
import org.alex73.voice.splitter.storage.TimeFormat;

@SuppressWarnings("serial")
public class OperationSplitAudio extends AbstractUndoableEdit {
    private final Audio a;
    private Part originalPart, newPart;
    private double time;
    public int dataIdx, uiIdx;
    public PartPanel originalPanel, newPanel;

    @Override
    public String getPresentationName() {
        return "split audio";
    }

    public OperationSplitAudio(Audio a, Part p, double time) {
        this.a = a;
        this.originalPart = p;
        this.time = time;

        // split data
        dataIdx = a.getParts().indexOf(p);
        newPart = new Part();
        newPart.startTime = TimeFormat.t2s(time);
        a.getParts().add(dataIdx + 1, newPart);

        // split UI
        uiIdx = VoiceSplitter.getPanelIndex(pp -> pp.part == p);
        originalPanel = (PartPanel) VoiceSplitter.frame.panelList.getComponent(uiIdx);
        originalPanel.updateAudioView();
        newPanel = new PartPanel(originalPanel.file, a, newPart);
        VoiceSplitter.frame.panelList.add(newPanel, null, uiIdx + 1);

        VoiceSplitter.frame.scroll.validate();
        VoiceSplitter.frame.scroll.repaint();
        originalPanel.showInAudioPanel(true);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        a.getParts().remove(newPart);

        originalPanel.updateAudioView();
        VoiceSplitter.frame.panelList.remove(newPanel);

        VoiceSplitter.frame.scroll.validate();
        VoiceSplitter.frame.scroll.repaint();
        originalPanel.showInAudioPanel(true);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        a.getParts().add(dataIdx + 1, newPart);

        originalPanel.updateAudioView();
        VoiceSplitter.frame.panelList.add(newPanel, null, uiIdx + 1);

        VoiceSplitter.frame.scroll.validate();
        VoiceSplitter.frame.scroll.repaint();
        originalPanel.showInAudioPanel(true);
    }
}
