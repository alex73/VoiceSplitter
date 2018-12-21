package org.alex73.voice.splitter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import org.alex73.voice.splitter.stat.SilenceDetection;
import org.alex73.voice.splitter.storage.Audio;
import org.alex73.voice.splitter.storage.AudioFile;
import org.alex73.voice.splitter.storage.Part;
import org.alex73.voice.splitter.storage.TimeFormat;

@SuppressWarnings("serial")
public class PartPanel extends JPanel {
    public final AudioFile file;
    public final Audio audio;
    public final Part part;
    public final JTextArea textArea;
    private double startTime, nextPartTime;
    private SimpleAudioPanel pa;

    public PartPanel(AudioFile af, Audio a, Part p) {
        this.file = af;
        this.audio = a;
        this.part = p;
        setLayout(new BorderLayout());

        updateAudioView();

        textArea = new JTextArea();
        textArea.setRows(3);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        ((AbstractDocument) textArea.getDocument()).setDocumentFilter(documentFilter);

        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(scroll, BorderLayout.CENTER);

        initText();

        setPreferredSize(new Dimension(100, 60));
    }

    private void initText() {
        if (part.text != null) {
            textArea.setText(part.text);
            textArea.setCaretPosition(0);
        }
        textArea.getDocument().addDocumentListener(documentListener);
        textArea.getDocument().addUndoableEditListener(VoiceSplitter.undo);
        textArea.addFocusListener(documentFocusListener);
    }

    private DocumentFilter documentFilter = new DocumentFilter() {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            super.insertString(fb, offset, fix(string), attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            super.replace(fb, offset, length, fix(text), attrs);
        }

        private String fix(String text) {
            return text.replace('\n', ' ').replace('\r', ' ');
        }
    };
    private DocumentListener documentListener = new DocumentListener() {
        @Override
        public void removeUpdate(DocumentEvent e) {
            updateChanges();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            updateChanges();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            updateChanges();
        }
    };
    private FocusListener documentFocusListener = new FocusListener() {
        @Override
        public void focusLost(FocusEvent e) {
            pa.focused = false;
            pa.repaint();
        }

        @Override
        public void focusGained(FocusEvent e) {
            pa.focused = true;
            pa.repaint();
            showInAudioPanel(false);
        }
    };

    public void updateChanges() {
        if (textWasChanged()) {
            textArea.setBackground(Color.YELLOW);
        } else {
            textArea.setBackground(Color.WHITE);
        }
    }

    public boolean textWasChanged() {
        return !textArea.getText().equals(part.text);
    }

    public void updateAudioView() {
        if (pa != null) {
            remove(pa);
        }

        startTime = TimeFormat.s2t(part.startTime);
        int idx = audio.getParts().indexOf(part);
        if (idx + 1 < audio.getParts().size()) {
            nextPartTime = TimeFormat.s2t(audio.getParts().get(idx + 1).startTime);
        } else {
            nextPartTime = VoiceSplitter.audio.getFile().getDuration();
        }

        SilenceDetection sd = new SilenceDetection(file, audio, part);
        String tooltip = "";

        double partVoiceLength = sd.getVoiceLength();
        double showSeconds;
        if (partVoiceLength > Settings.partTimeMax) {
            showSeconds = Settings.partTimeMax * 2;
            tooltip = VoiceSplitter.bundle.getString("AUDIO_TOOLTIP_LENGTH_LONG");
        } else if (partVoiceLength < Settings.partTimeMin) {
            showSeconds = Settings.partTimeMin * 2;
            tooltip = VoiceSplitter.bundle.getString("AUDIO_TOOLTIP_LENGTH_SHORT");
        } else {
            showSeconds = Settings.partTimeMax * 1.2;
            tooltip = VoiceSplitter.bundle.getString("AUDIO_TOOLTIP_LENGTH_OK");
        }
        pa = new SimpleAudioPanel(VoiceSplitter.audio.getFile(), startTime, nextPartTime - startTime, showSeconds,
                (int) Math.round(showSeconds * 20));
        if (sd.silenceAtStart < 0.15 || sd.silenceAtEnd < 0.15) {
            pa.setBackground(Color.YELLOW);
            tooltip += VoiceSplitter.bundle.getString("AUDIO_TOOLTIP_SILENCE_SHORT");
        } else {
            pa.setBackground(Color.WHITE);
        }
        pa.setBackground(sd.silenceAtStart < 0.15 || sd.silenceAtEnd < 0.15 ? Color.YELLOW : Color.WHITE);
        pa.setToolTipText(tooltip);
        add(pa, BorderLayout.WEST);
    }

    public void showInAudioPanel(boolean force) {
        if (!force && VoiceSplitter.audio.p == part) {
            return;
        }
        VoiceSplitter.audio.draw(startTime, nextPartTime, part);
    }
}
