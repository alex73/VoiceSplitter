package org.alex73.voice.splitter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.alex73.voice.splitter.storage.AudioFile;

@SuppressWarnings("serial")
public class SimpleAudioPanel extends JPanel {
    private short[] vols;
    public boolean focused;

    public SimpleAudioPanel(AudioFile file, double startTime, double length, double displayLength, int displayWidth) {
        double showLength;
        int samples;
        if (length < displayLength) {
            showLength = length;
            samples = (int) Math.round(length * displayWidth / displayLength);
        } else {
            showLength = displayLength;
            samples = displayWidth;
        }
        vols = new short[0];
        setPreferredSize(new Dimension(displayWidth, 10));

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                vols = file.getVolume(startTime, showLength, samples);
                return null;
            }

            @Override
            protected void done() {
                repaint();
            }
        }.execute();
    }

    @Override
    protected void paintComponent(Graphics gr) {
        Graphics2D g = (Graphics2D) gr;

        g.setPaint(focused ? new Color(224, 224, 224) : getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setPaint(Color.GRAY);
        for (int x = 0; x < getWidth(); x++) {
            if (x < vols.length) {
                int y = vols[x] * getHeight() / 10000;
                g.drawLine(x, getHeight() - y, x, getHeight());
            }
        }
    }
}
