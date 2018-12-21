package org.alex73.voice.splitter.stat;

import java.util.ArrayList;
import java.util.List;

import org.alex73.voice.splitter.Settings;
import org.alex73.voice.splitter.storage.Audio;
import org.alex73.voice.splitter.storage.AudioFile;
import org.alex73.voice.splitter.storage.Part;
import org.alex73.voice.splitter.storage.TimeFormat;

public class SilenceDetection {
    public List<Silence> silences = new ArrayList<>();
    public double start;
    public double length;
    public double silenceAtStart, silenceAtEnd;

    public SilenceDetection(AudioFile af, Audio a, Part p) {
        start = TimeFormat.s2t(p.startTime);
        int idx = a.getParts().indexOf(p);
        if (idx + 1 < a.getParts().size()) {
            length = TimeFormat.s2t(a.getParts().get(idx + 1).startTime) - start;
        } else {
            length = af.getDuration() - start;
        }

        short[] detection = af.getVolume(start, length, (int) Math.round(length / Settings.detectionBlockLength));
        Silence s = null;
        int firstDetectionVolume = -1, lastDetectionVolume = 0;
        for (int i = 0; i < detection.length; i++) {
            boolean silenceNow = detection[i] < Settings.silenceLevel;
            double time = start + i * Settings.detectionBlockLength;
            if (silenceNow) {
                if (s == null) {
                    s = new Silence();
                    s.from = time;
                }
            } else {
                if (firstDetectionVolume < 0) {
                    firstDetectionVolume = i;
                }
                lastDetectionVolume = i;
                if (s != null) {
                    s.to = time;
                    if (s.isEnoughSilence()) {
                        silences.add(s);
                    }
                    s = null;
                }
            }
        }
        if (s != null) {
            s.to = start + detection.length * Settings.detectionBlockLength;
            if (s.isEnoughSilence()) {
                silences.add(s);
            }
        }
        silenceAtStart = firstDetectionVolume < 0 ? 0 : firstDetectionVolume * Settings.detectionBlockLength;
        silenceAtEnd = (detection.length - 1 - lastDetectionVolume) * Settings.detectionBlockLength;
    }

    public double getVoiceLength() {
        return length - silenceAtStart - silenceAtEnd;
    }
    public double getVoiceStart() {
        return start + silenceAtStart ;
    }

    public static class Silence {
        public double from, to;

        boolean isEnoughSilence() {
            return (to - from) >= Settings.silenceMinLength;
        }

        @Override
        public String toString() {
            return "From " + TimeFormat.t2s(from) + " - " + TimeFormat.t2s(to - from);
        }
    }
}
