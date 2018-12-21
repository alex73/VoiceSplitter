package org.alex73.voice.splitter.ops;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.alex73.voice.splitter.PartPanel;
import org.alex73.voice.splitter.VoiceSplitter;
import org.alex73.voice.splitter.storage.Audio;
import org.alex73.voice.splitter.storage.Part;

@SuppressWarnings("serial")
public class OperationMergeAudio extends AbstractUndoableEdit {
    private final Audio a;
    private Part originalPart, originalNextPart;
    public int dataIdx, uiIdx, uiNextIdx;
    public PartPanel originalPanel, nextPanel;

    @Override
    public String getPresentationName() {
        return "merge with next";
    }

    public OperationMergeAudio(Audio a, Part p, Part pnext) {
        this.a = a;
        this.originalPart = p;
        this.originalNextPart = pnext;

        // split data
        dataIdx = a.getParts().indexOf(p);
        a.getParts().remove(originalNextPart);

        // split UI
        uiIdx = VoiceSplitter.getPanelIndex(pp -> pp.part == originalPart);
        originalPanel = (PartPanel) VoiceSplitter.frame.panelList.getComponent(uiIdx);
        uiNextIdx = VoiceSplitter.getPanelIndex(pp -> pp.part == originalNextPart);
        nextPanel = (PartPanel) VoiceSplitter.frame.panelList.getComponent(uiNextIdx);
        VoiceSplitter.frame.panelList.remove(nextPanel);

        originalPanel.updateAudioView();

        VoiceSplitter.frame.scroll.validate();
        VoiceSplitter.frame.scroll.repaint();
        originalPanel.showInAudioPanel(true);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        a.getParts().add(dataIdx + 1, originalNextPart);

        originalPanel.updateAudioView();
        VoiceSplitter.frame.panelList.add(nextPanel, null, uiIdx + 1);

        VoiceSplitter.frame.scroll.validate();
        VoiceSplitter.frame.scroll.repaint();
        originalPanel.showInAudioPanel(true);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        a.getParts().remove(originalNextPart);

        originalPanel.updateAudioView();
        VoiceSplitter.frame.panelList.remove(nextPanel);

        VoiceSplitter.frame.scroll.validate();
        VoiceSplitter.frame.scroll.repaint();
        originalPanel.showInAudioPanel(true);
    }
}
