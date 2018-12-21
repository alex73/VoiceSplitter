package org.alex73.voice.splitter.storage;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Clip;

public class AudioFile {
    private final Path f;
    private byte[] data;
    public short fmtFormat, fmtChannels, fmtBlockAlign;
    public short fmtBitsPerSample; // 8 bit - unsigned, 16 bit - signed
    public int fmtSampleRate, fmtByteRate;
    private int dataOffset, dataSize;

    public AudioFile(Path f) throws Exception {
        this.f = f;
        if (f.toString().toLowerCase().endsWith(".wav")) {
            readWAV(f);
        } else {
            readOther(f);
        }
        ByteBuffer mapped = ByteBuffer.wrap(data);
        mapped.order(ByteOrder.LITTLE_ENDIAN);
        int markRiff = mapped.getInt();
        if (markRiff != 0x46464952) {
            throw new Exception("Audio file format error: no RIFF mark");
        }
        int riffChunkSize = mapped.getInt();
        int markWave = mapped.getInt();
        if (markWave != 0x45564157) {
            throw new Exception("Audio file format error: no WAVE mark");
        }

        d: while (true) {
            int mark = mapped.getInt();
            int size = mapped.getInt();
            switch (mark) {
            case 0x20746d66:// fmt
                int pos = mapped.position();
                fmtFormat = mapped.getShort();
                fmtChannels = mapped.getShort();
                fmtSampleRate = mapped.getInt();
                fmtByteRate = mapped.getInt();
                fmtBlockAlign = mapped.getShort();
                fmtBitsPerSample = mapped.getShort();
                mapped.position(pos + size);
                break;
            case 0x61746164:// data
                dataOffset = mapped.position();
                dataSize = size;
                break d;
            default:
                mapped.position(mapped.position() + size);
                break;
            }
        }
        if (fmtFormat != 1) {
            throw new Exception("PCM audio is supported only");
        }
        if (fmtChannels != 1) {
            throw new Exception("Mono audio is supported only");
        }
        if (fmtSampleRate < 4000 || fmtSampleRate > 98000) {
            throw new Exception("Wrong sample rate in file: " + fmtSampleRate);
        }
        if (fmtBitsPerSample != 16) {
            throw new Exception("Wrong bits count in file: " + fmtBitsPerSample);
        }
        if (dataOffset < 0 || dataOffset > data.length) {
            throw new Exception("Wrong data offset");
        }
        if (dataSize < 0) {
            dataSize = data.length - dataOffset;
        } else if (dataSize < 0 || dataOffset + dataSize > data.length) {
            throw new Exception("Wrong data size");
        }
    }

    public Path getFile() {
        return f;
    }

    private void readWAV(Path f) throws Exception {
        data = Files.readAllBytes(f);
    }

    public void export(String out, double start, double length) throws Exception {
        ByteBuffer mapped = ByteBuffer.wrap(data);
        mapped.order(ByteOrder.LITTLE_ENDIAN);
        int startFrame = (int) Math.round(start * fmtSampleRate);
        int lengthFrames = (int) Math.round(length * fmtSampleRate);
        mapped.position(dataOffset + fmtBitsPerSample / 8 * startFrame);
        mapped.limit(mapped.position() + fmtBitsPerSample / 8 * lengthFrames);

        ByteBuffer header = ByteBuffer.wrap(new byte[44]);
        header.order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(0x46464952);
        header.putInt(fmtBitsPerSample / 8 * lengthFrames + 36); // total file size - 8
        header.putInt(0x45564157);
        header.putInt(0x20746d66); // fmt
        header.putInt(16);

        header.putShort(fmtFormat);
        header.putShort(fmtChannels);
        header.putInt(fmtSampleRate);
        header.putInt(fmtByteRate);
        header.putShort(fmtBlockAlign);
        header.putShort(fmtBitsPerSample);

        header.putInt(0x61746164); // data
        header.putInt(fmtBitsPerSample / 8 * lengthFrames);
        header.position(0);

        RandomAccessFile stream = new RandomAccessFile(out, "rw");
        FileChannel channel = stream.getChannel();
        channel.write(header);
        channel.write(mapped);
        channel.truncate(channel.position());
        stream.close();
        channel.close();
    }

    private void readOther(Path f) throws Exception {
        Process p = Runtime.getRuntime()
                .exec(new String[] { "ffmpeg", "-i", f.toString(), "-ac", "1", "-f", "wav", "-" });
        OutputReader stdin = new OutputReader(p.getInputStream());
        OutputReader stderr = new OutputReader(p.getErrorStream());
        stdin.start();
        stderr.start();
        int r = p.waitFor();
        stdin.join();
        if (r != 0) {
            throw new Exception("Error convert " + f + ": " + r);
        }
        data = stdin.getContent();
    }

    public double getDuration() {
        return 1.0 * dataSize / fmtSampleRate / (fmtBitsPerSample / 8);
    }

    /**
     * Returns volumes as possible values from 0 to 10000.
     */
    public short[] getVolume(double start, double time, int samples) {
        ByteBuffer mapped = ByteBuffer.wrap(data);
        mapped.order(ByteOrder.LITTLE_ENDIAN);
        int startFrame = (int) Math.round(start * fmtSampleRate);
        int endFrame = (int) Math.round((start + time) * fmtSampleRate);
        if (fmtBitsPerSample / 8 * endFrame > dataSize) {
            endFrame = dataSize / (fmtBitsPerSample / 8);
        }

        short[] result = new short[samples];
        mapped.position(dataOffset + fmtBitsPerSample / 8 * startFrame);
        for (int fr = startFrame; fr < endFrame; fr++) {
            int i = (int) (((long) fr - startFrame) * samples / (endFrame - startFrame));
            int v;
            if (fmtBitsPerSample == 16) {
                v = Math.abs(mapped.getShort()) * 10000 / 32768;
            } else {
                throw new RuntimeException("Wrong file format");
            }
            result[i] = (short) Math.max(result[i], v);
        }
        return result;
    }

    public void output(Clip clip, double start, double time) throws Exception {
        int startFrame = (int) Math.round(start * fmtSampleRate);
        int sizeFrame = (int) Math.round(time * fmtSampleRate);
        int offset = dataOffset + fmtBitsPerSample / 8 * startFrame;
        int size = fmtBitsPerSample / 8 * sizeFrame;
        if (offset + size > dataSize) {
            size = dataSize - offset;
        }
        AudioFormat fmt = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, fmtSampleRate, fmtBitsPerSample, fmtChannels,
                fmtBitsPerSample / 8, fmtSampleRate, false);
        clip.open(fmt, data, offset, size);
    }

    class OutputReader extends Thread {
        private final InputStream in;
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private volatile Exception ex;

        public OutputReader(InputStream in) {
            this.in = in;
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[65536];
                while (true) {
                    int len = in.read(buffer);
                    if (len < 0) {
                        break;
                    }
                    out.write(buffer, 0, len);
                }
            } catch (Exception e) {
                ex = e;
            }
        }

        public byte[] getContent() throws Exception {
            if (ex != null) {
                throw ex;
            }
            return out.toByteArray();
        }
    }
}
