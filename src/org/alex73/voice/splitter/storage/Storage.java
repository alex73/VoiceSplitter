package org.alex73.voice.splitter.storage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.alex73.voice.splitter.Settings;

public class Storage {
    static JAXBContext CONTEXT;
    static public Parts all;
    static File storeDir;

    public static void open(File dir) throws Exception {
        if (CONTEXT == null) {
            CONTEXT = JAXBContext.newInstance(Parts.class);
        }
        storeDir = dir;
        File splitFile = new File(dir, "split.xml");
        if (splitFile.exists()) {
            Unmarshaller m = CONTEXT.createUnmarshaller();
            all = (Parts) m.unmarshal(splitFile);
        } else {
            List<Path> fs = Files
                    .find(dir.toPath(), Integer.MAX_VALUE,
                            (p, a) -> a.isRegularFile()
                                    && p.getFileName().toString().toLowerCase().matches(".+\\.(wav|mp3)$"))
                    .sorted().collect(Collectors.toList());
            all = new Parts();
            for (Path p : fs) {
                String fn = dir.toPath().relativize(p).toString().replace('\\', '/');
                Audio a = new Audio();
                a.setName(fn);
                Part np = new Part();
                np.startTime = TimeFormat.t2s(0);
                np.text = "";
                a.getParts().add(np);
                all.getFiles().add(a);
            }
            save();
        }
    }

    public static File getStoreDir() {
        return storeDir;
    }

    public static int getCacheOffset(double time) {
        return (int) Math.round(time / Settings.detectionBlockLength);
    }

    public static void save() throws Exception {
        Marshaller m = CONTEXT.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(all, new File(storeDir, "split.xml"));
    }
}
