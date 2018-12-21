package org.alex73.voice.splitter.stat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import org.alex73.voice.splitter.Settings;
import org.alex73.voice.splitter.VoiceSplitter;
import org.alex73.voice.splitter.storage.Audio;
import org.alex73.voice.splitter.storage.AudioFile;
import org.alex73.voice.splitter.storage.Part;
import org.alex73.voice.splitter.storage.Storage;

@SuppressWarnings("serial")
public class StatLength extends JDialog {
    static double totalValue, minScale, maxScale;
    static double min, max;

    static List<Double> values = new ArrayList<>();
    static List<String> labels = new ArrayList<>();

    public StatLength() {
        super(VoiceSplitter.frame, true);
    }

    public static void showStat() {
        StatLength frame = new StatLength();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(900, 300);
        frame.setTitle("Statistics");

        JProgressBar progress = new JProgressBar();
        progress.setMaximum(Storage.all.getFiles().size());
        frame.getContentPane().add(progress, BorderLayout.NORTH);

        new SwingWorker<Void, String>() {
            List<Double> result = new ArrayList<>();

            @Override
            protected Void doInBackground() throws Exception {
                for (int i = 0; i < Storage.all.getFiles().size(); i++) {
                    Audio a = Storage.all.getFiles().get(i);
                    publish(a.getName());
                    AudioFile af = new AudioFile(new File(Storage.getStoreDir(), a.getName()).toPath());
                    for (Part p : a.getParts()) {
                        SilenceDetection sd = new SilenceDetection(af, a, p);
                        double voiceLength = sd.getVoiceLength();
                        if (voiceLength >= Settings.partTimeMin && voiceLength <= Settings.partTimeMax) {
                            result.add(voiceLength);
                        }
                    }
                }
                frame.prepare(result);
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String c : chunks) {
                    progress.setValue(progress.getValue() + 1);
                }
            }

            @Override
            protected void done() {
                try {
                    get();

                    Draw d = new Draw();
                    d.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                    JLabel total = new JLabel(
                            "TotalTime: " + new DecimalFormat("#.0").format(totalValue / 60 / 60) + "h");
                    total.setHorizontalAlignment(SwingConstants.CENTER);
                    frame.getContentPane().add(total, BorderLayout.SOUTH);
                    frame.getContentPane().add(d);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    frame.getContentPane().add(new JLabel("Error: " + ex.getMessage()));
                }
                frame.validate();
            }
        }.execute();

        frame.setVisible(true);
    }

    void prepare(List<Double> lengths) {
        min = Double.MAX_VALUE;
        max = 0;
        for (double len : lengths) {
            min = Math.min(min, len);
            max = Math.max(max, len);
            totalValue += len;
        }

        int vmin = (int) min;
        int vmax = (int) max + 1;
        int c = vmax - vmin;
        int step = Math.round(c / 10.0f);
        minScale = step / (vmin + step - min);
        maxScale = step / (max - vmax + step);
        for (int v = vmin; v < vmax; v += step) {
            double vlen = 0;
            int count = 0;
            for (double len : lengths) {
                len += 0.2; // 0.1 + 0.1 around voice
                if (len >= v && len < v + step) {
                    vlen += len;
                    count++;
                }
            }
            if (v == vmin) {
                labels.add(new DecimalFormat("#.#").format(min));
            } else {
                labels.add("" + v);
            }
            values.add(count * 1.0);
        }
        labels.add(new DecimalFormat("#.#").format(max));
    }

    static class Draw extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Insets ins = getBorder().getBorderInsets(this);
            int w = getWidth() - ins.left - ins.right;
            int h = getHeight() - ins.top - ins.bottom;

            int fontHeight = g.getFontMetrics().getHeight();

            h -= fontHeight - 5;

            double maxValue = 0;
            for (double v : values) {
                maxValue = Math.max(maxValue, v);
            }

            int columnWidth = w / labels.size();
            for (int i = 0; i < values.size(); i++) {
                int x1 = columnWidth * i + columnWidth / 2 + ins.left;
                int f = (int) Math.round(h * values.get(i) / maxValue);
                g.setColor(Color.BLUE);
                g.fillRect(x1, h - f + ins.top, columnWidth - 1, f);
                if (i == 0) {
                    f = (int) Math.round(h * values.get(i) / maxValue * minScale);
                    int cw = (int) Math.round(columnWidth / minScale) - 1;
                    g.setColor(new Color(0, 128, 0));
                    g.drawRect(x1 + columnWidth - cw, h - f + ins.top, cw - 2, f - 1);
                } else if (i == values.size() - 1) {
                    f = (int) Math.round(h * values.get(i) / maxValue * maxScale);
                    int cw = (int) Math.round(columnWidth / maxScale) - 1;
                    g.setColor(new Color(0, 128, 0));
                    g.drawRect(x1, h - f + ins.top, cw, f - 1);
                }
                g.setColor(Color.BLACK);
                String s = labels.get(i);
                int sw = g.getFontMetrics().stringWidth(s);
                g.drawString(s, x1 - sw / 2, h + ins.top + fontHeight - 2);
            }
            int x1 = columnWidth * values.size() + columnWidth / 2 + ins.left;
            String s = labels.get(values.size());
            int sw = g.getFontMetrics().stringWidth(s);
            g.drawString(s, x1 - sw / 2, h + ins.top + fontHeight - 2);
        }
    }
}
