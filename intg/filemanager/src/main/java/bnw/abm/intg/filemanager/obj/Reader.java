package bnw.abm.intg.filemanager.obj;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;

public class Reader {
    public static Object readObjectLZMA(Path filePath) throws IOException, ClassNotFoundException {
        Object inObject = null;
        try (ObjectInputStream objInStream = new ObjectInputStream(new LZMACompressorInputStream(new BufferedInputStream(
                Files.newInputStream(filePath))))) {
            inObject = objInStream.readObject();
        }
        return inObject;
    }
}
