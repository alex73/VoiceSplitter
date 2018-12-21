package org.alex73.voice.splitter;

import java.text.MessageFormat;
import java.util.concurrent.CancellationException;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

public class Player {
    private static SHOW show;

    public static synchronized void stop() {
        SHOW p = show;
        if (p != null) {
            VoiceSplitter.audio.place = VoiceSplitter.audio.playPlace;
            p.cancel(false);
        }
    }

    public static synchronized void selected() {
        SHOW p = show;
        if (p != null) {
            VoiceSplitter.audio.place = VoiceSplitter.audio.playPlace;
            p.cancel(false);
        } else {
            show = new SHOW();
            show.execute();
        }
    }

    static class SHOW extends SwingWorker<Void, Void> {
        Clip clip;
        double start;

        public SHOW() {
            start = VoiceSplitter.audio.place;
            VoiceSplitter.frame.bPlay.setText(VoiceSplitter.bundle.getString("BUTTON_STOP"));
        }

        @Override
        protected Void doInBackground() throws Exception {
            clip = AudioSystem.getClip();
            double maxlen = Math.min(25, VoiceSplitter.audio.getLength());
            VoiceSplitter.audio.getFile().output(clip, VoiceSplitter.audio.getStartTime() + start, maxlen - start);
            clip.addLineListener(listener);
            clip.start();
            while (!isCancelled()) {
                VoiceSplitter.audio.playPlace = clip.getMicrosecondPosition() / 1000000.0 + start;
                VoiceSplitter.audio.repaint();
                Thread.sleep(50);
            }
            clip.stop();
            clip.close();
            return null;
        }

        LineListener listener = new LineListener() {
            public void update(LineEvent event) {
                if (event.getType() == LineEvent.Type.STOP) {
                    SHOW.this.cancel(false);
                }
            }
        };

        @Override
        protected void done() {
            VoiceSplitter.audio.playPlace = VoiceSplitter.audio.place;
            VoiceSplitter.audio.repaint();

            VoiceSplitter.frame.bPlay.setText(VoiceSplitter.bundle.getString("BUTTON_PLAY"));
            show = null;
            try {
                get();
            } catch (CancellationException ex) {
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(VoiceSplitter.frame,
                        MessageFormat.format(VoiceSplitter.bundle.getString("ERROR_PLAY"), ex.getMessage()),
                        VoiceSplitter.bundle.getString("ERROR_TITLE"), JOptionPane.ERROR_MESSAGE);
            }
        }
    };
}
