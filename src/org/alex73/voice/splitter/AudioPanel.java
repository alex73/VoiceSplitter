package org.alex73.voice.splitter;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;

import org.alex73.voice.splitter.ops.OperationSplitAudio;
import org.alex73.voice.splitter.stat.SilenceDetection;
import org.alex73.voice.splitter.stat.SilenceDetection.Silence;
import org.alex73.voice.splitter.storage.Audio;
import org.alex73.voice.splitter.storage.AudioFile;
import org.alex73.voice.splitter.storage.Part;

@SuppressWarnings("serial")
public class AudioPanel extends JPanel {
    public int pixelsPerSecond = 64;
    public Audio a;
    public Part p;
    private AudioFile file;
    private double startTime, length;
    short[] vols = new short[0];
    public double place;
    public double playPlace;
    List<Silence> silences = Collections.emptyList();

    public AudioPanel() {
        addMouseListener(audioMouse);
        addMouseMotionListener(audioMouseMotion);

        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        for (Silence s : silences) {
            int xb = (int) Math.round((s.from - startTime) * pixelsPerSecond);
            int xe = (int) Math.round((s.to - startTime) * pixelsPerSecond);
            if (event.getX() > xb && event.getX() < xe) {
                return new DecimalFormat("#0.000").format(s.to - s.from);
            }
        }
        return null;
    }

    public void setFile(Audio a, AudioFile f) {
        Player.stop();
        this.file = f;
        startTime = 0;
        length = 0;
        this.a = a;
        this.p = null;
        vols = new short[0];
        silences = new ArrayList<>();
        repaint();
    }

    public void draw(double startTime, double endTime, Part p) {
        Player.stop();
        this.startTime = startTime;
        this.length = Math.min(endTime - startTime, 50);
        this.length = endTime - startTime;
        this.p = p;
        draw();
    }

    public void draw() {
        place = 0;
        playPlace = 0;
        vols = new short[0];
        silences.clear();
        new SwingWorker<Void, Void>() {
            short[] vs;
            SilenceDetection sd;

            @Override
            protected Void doInBackground() throws Exception {
                vs = file.getVolume(startTime, length, (int) Math.round(length * pixelsPerSecond));
                sd = new SilenceDetection(file, a, p);
                return null;
            }

            @Override
            protected void done() {
                vols = vs;
                silences = sd.silences;
                repaint();
            }
        }.execute();
        repaint();
    }
    
    void updateVoicePlace() {
        
    }

    public AudioFile getFile() {
        return file;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getLength() {
        return length;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.clearRect(0, 0, getWidth(), getHeight());
        int h = getHeight() - 30;

        // silences
        g.setColor(Color.CYAN);
        for (Silence s : silences) {
            int xb = (int) Math.round((s.from - startTime) * pixelsPerSecond);
            int xe = (int) Math.round((s.to - startTime) * pixelsPerSecond);
            g.fillOval(xb, h - h / 8, xe - xb, h / 4);
        }
        g.clearRect(0, h, getWidth(), getHeight());

        // silence level
        int sy = h - Settings.silenceLevel * h / 10000;
        g.setColor(Color.BLACK);
        g.drawLine(0, sy, vols.length, sy);

        // volumes
        g.setColor(Color.RED);
        for (int i = 0; i < vols.length; i++) {
            int y1 = h - vols[i] * h / 10000;
            g.drawLine(i, y1, i, h);
        }

        // scale
        g.setColor(Color.BLUE);
        g.fillRect(0, h, vols.length, 1);
        int fh = g.getFontMetrics().getHeight();
        for (int i = 0; i * pixelsPerSecond <= getWidth(); i++) {
            int x = i * pixelsPerSecond;
            g.drawLine(x, h + 1, x, h + 5);
            g.drawString("" + i, x, h + 5 + fh);
        }

        g.setColor(Color.ORANGE);
        int px = (int) Math.round(place * pixelsPerSecond);
        g.drawLine(px, 0, px, getHeight());

        g.setColor(Color.GREEN);
        int px2 = (int) Math.round(playPlace * pixelsPerSecond);
        g.drawLine(px2, 0, px2, getHeight());
    }

    MouseAdapter audioMouse = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            switch (e.getButton()) {
            case MouseEvent.BUTTON1:
                // left button - set cursor position
                place = 1.0 * e.getX() / pixelsPerSecond;
                repaint();
                break;
            case MouseEvent.BUTTON3:
                // right button - split at the middle of silence
                for (Silence s : silences) {
                    int xb = (int) Math.round((s.from - startTime) * pixelsPerSecond);
                    int xe = (int) Math.round((s.to - startTime) * pixelsPerSecond);
                    if (e.getX() > xb && e.getX() < xe) {
                        // split in the middle of silence
                        OperationSplitAudio op2 = new OperationSplitAudio(a, p, (s.from + s.to) / 2);
                        VoiceSplitter.undo.addEdit(op2);
                        VoiceSplitter.wasChanged = true;
                        break;
                    }
                }
                break;
            }
        }
    };
    MouseMotionAdapter audioMouseMotion = new MouseMotionAdapter() {
        public void mouseDragged(MouseEvent e) {
            switch (e.getModifiersEx()) {
            case MouseEvent.BUTTON1_DOWN_MASK:
                place = 1.0 * e.getX() / pixelsPerSecond;
                repaint();
                break;
            }
        }
    };
}
