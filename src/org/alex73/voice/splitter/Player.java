package org.alex73.voice.splitter;

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.concurrent.CancellationException;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.alex73.voice.splitter.storage.AudioFile;

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
        SourceDataLine line;
        double start;

        public SHOW() {
            start = VoiceSplitter.audio.place;
            VoiceSplitter.frame.bPlay.setText(VoiceSplitter.bundle.getString("BUTTON_STOP"));
        }

        @Override
        protected Void doInBackground() throws Exception {
            AudioFile af = VoiceSplitter.audio.getFile();
            line = AudioSystem.getSourceDataLine(af.getAudioFormat());
            double maxlen = Math.min(25, VoiceSplitter.audio.getLength());
            line.addLineListener(listener);
            line.open();
            line.start();

            ByteBuffer buffer = af.getPart(VoiceSplitter.audio.getStartTime() + start, maxlen - start);
            int step = af.getOneSecondSize() / 20;
            while (buffer.hasRemaining() && !isCancelled()) {
                int len = Math.min(step, buffer.remaining());
                line.write(buffer.array(), buffer.position(), len);
                buffer.position(buffer.position() + len);
                System.out.println(line.getMicrosecondPosition());
                VoiceSplitter.audio.playPlace = line.getMicrosecondPosition() / 1000000.0 + start;
                SwingUtilities.invokeLater(() -> VoiceSplitter.audio.repaint());
            }
            if (!isCancelled()) {
                line.drain();
            }
            line.flush();
            line.stop();
            line.close();
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
