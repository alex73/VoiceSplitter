package org.alex73.voice.splitter.storage;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.alex73.voice.splitter.Settings;
import org.alex73.voice.splitter.VoiceSplitter;
import org.alex73.voice.splitter.stat.SilenceDetection;

public class Export {
    public static void start() {
        JFileChooser ch = new JFileChooser();
        ch.setCurrentDirectory(VoiceSplitter.dir);
        ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        JCheckBox cb = new JCheckBox(VoiceSplitter.bundle.getString("EXPORT_RANDOM"));
        ch.setAccessory(cb);
        if (ch.showSaveDialog(VoiceSplitter.frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        boolean randomSort = cb.isSelected();

        JProgressBar bar = new JProgressBar();
        bar.setMaximum(Storage.all.getFiles().size());
        JDialog dialog = new JDialog(VoiceSplitter.frame, true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().add(bar);
        new SwingWorker<Void, String>() {
            int num;
            int num2;

            @Override
            protected Void doInBackground() throws Exception {
                List<Audio> files = new ArrayList<>(Storage.all.getFiles());
                if (randomSort) {
                    files = randomSort(files);
                }
                double len = 0;
                new File(ch.getSelectedFile().getPath() + "/wavs/").mkdirs();
                List<String> metadata = new ArrayList<>();
                List<String> metadata_s = new ArrayList<>();
                for (int i = 0; i < files.size(); i++) {
                    Audio a = files.get(i);
                    publish(a.getName());
                    AudioFile af = new AudioFile(new File(Storage.getStoreDir(), a.getName()).toPath());
                    List<Part> exportParts = new ArrayList<>();
                    List<Part> exportParts2 = new ArrayList<>();
                    for (Part p : a.getParts()) {
                        if (p.text.trim().isEmpty()) {
                            continue;
                        }
                        if (p.text.contains("*")) {
                            exportParts2.add(p);
                        } else {
                            exportParts.add(p);
                        }
                    }
                    if (randomSort) {
                        exportParts = randomSort(exportParts);
                        exportParts2 = randomSort(exportParts2);
                    }
                    for (Part p : exportParts) {
                        if (!dialog.isVisible()) {
                            return null;
                        }
                        SilenceDetection sd = new SilenceDetection(af, a, p);
                        double voiceLength = sd.getVoiceLength();
                        if (voiceLength >= Settings.partTimeMin && voiceLength <= Settings.partTimeMax) {
                            num++;
                            String fn = new DecimalFormat("000000").format(num);
                            double start = sd.silenceAtStart < 0.2 ? 0 : sd.silenceAtStart - 0.2;
                            double end = sd.silenceAtEnd < 0.2 ? 0 : sd.silenceAtEnd - 0.2;
                            af.export(ch.getSelectedFile().getPath() + "/wavs/" + fn + ".wav", sd.start + start,
                                    sd.length - start - end);
                            metadata.add(fn + "|" + p.text.trim() + "|" + p.text.trim());
                            if (sd.silenceAtStart < 0.15 || sd.silenceAtEnd < 0.15) {
                                //System.out.println("Wrong silence: "+af.getFile().getFileName().toString()+" part "+p.startTime);
                            }
                            len += sd.length - start - end;
                        } else {
                            System.out.println("Wrong length: "+af.getFile().getFileName().toString()+" part "+p.startTime+" length "+TimeFormat.t2s(voiceLength));
                        }
                    }
                    for (Part p : exportParts2) {
                        if (!dialog.isVisible()) {
                            return null;
                        }
                        SilenceDetection sd = new SilenceDetection(af, a, p);
                        double voiceLength = sd.getVoiceLength();
                        if (voiceLength >= Settings.partTimeMin && voiceLength <= Settings.partTimeMax) {
                            num2++;
                            String fn = new DecimalFormat("000000").format(num2);
                            af.export(ch.getSelectedFile().getPath() + "/wavs/s" + fn + ".wav", sd.start, sd.length);
                            metadata_s.add(fn + "|" + p.text.trim() + "|" + p.text.trim());
                        }
                    }
                }
                Files.write(Paths.get(ch.getSelectedFile().getPath() + "/metadata.csv"), metadata,
                        StandardCharsets.UTF_8);
                Files.write(Paths.get(ch.getSelectedFile().getPath() + "/metadata-s.csv"), metadata_s,
                        StandardCharsets.UTF_8);
                System.out.println("Total length of good records: "+Math.round(len)+"s");
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String c : chunks) {
                    bar.setValue(bar.getValue() + 1);
                }
            }

            @Override
            protected void done() {
                String result;
                try {
                    get();
                    result = "OK";
                } catch (Exception ex) {
                    result = "Error: " + ex.getMessage();
                }
                dialog.getContentPane().removeAll();
                dialog.getContentPane().add(new JLabel(result, JLabel.CENTER));
                dialog.getContentPane().validate();
            }
        }.execute();
        dialog.setSize(600, 200);
        dialog.setLocationRelativeTo(VoiceSplitter.frame);
        dialog.setVisible(true);
    }

    static <T> List<T> randomSort(List<T> parts) {
        List<T> in = new ArrayList<>(parts);
        List<T> result = new ArrayList<>();
        Random r = new Random();
        while (!in.isEmpty()) {
            int idx = r.nextInt(in.size());
            result.add(in.remove(idx));
        }
        return result;
    }
}
