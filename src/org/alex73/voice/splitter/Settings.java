package org.alex73.voice.splitter;

import java.io.InputStream;
import java.util.Properties;

public class Settings {
    public static double partTimeMin, partTimeMax;
    public static double silenceMinLength;
    public static int silenceLevel;
    public static double detectionBlockLength;

    static {
        Properties p = new Properties();
        try (InputStream in = Settings.class.getResourceAsStream("/settings.properties")) {
            p.load(in);
            partTimeMin = Double.parseDouble(p.getProperty("partTimeMin"));
            partTimeMax = Double.parseDouble(p.getProperty("partTimeMax"));
            silenceMinLength = Double.parseDouble(p.getProperty("silenceMinLength"));
            silenceLevel = Integer.parseInt(p.getProperty("silenceLevel"));
            detectionBlockLength = Double.parseDouble(p.getProperty("detectionBlockLength"));
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}
