package org.alex73.voice.splitter;

import org.alex73.voice.splitter.storage.Audio;

public class AudioFileComboItem {
    private final Audio a;

    public AudioFileComboItem(Audio a) {
        this.a = a;
    }

    @Override
    public String toString() {
        return a.getName() + " (" + a.getParts().size() + ")";
    }

    public Audio getAudio() {
        return a;
    }
}
