package bnw.abm.intg.filemanager.obj;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import bnw.abm.intg.filemanager.BNWFiles;
import bnw.abm.intg.util.GlobalConstants.EXITCODE;
import bnw.abm.intg.util.Log;

public class Writer {

    private Path filePath = null;

    public static Writer createAndWriteToFile(Object object, Path filePath) throws IOException {
        Writer writer = new Writer();
        writer.filePath = filePath;
        writer.writeToFile(object, filePath);
        return writer;
    }

    public static Writer createAndWriteToFileLZMA(Serializable object, Path filePath) throws IOException, InterruptedException {
        Writer writer = new Writer();
        writer.filePath = filePath;

        // Removing lzma extension for LZMA compression workaround - Apache commons that now way to compress file using lzma
        String fileNameWithoutLzma = BNWFiles.getFileName(filePath);
        Path parent = filePath.getParent();
        if (parent == null) {
            filePath = Paths.get(fileNameWithoutLzma);
        } else {
            filePath = parent.resolve(fileNameWithoutLzma);
            Files.createDirectories(parent);
        }
        
        try (ObjectOutputStream outStream = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(filePath,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)))) {
            // compressedOutStream = new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.LZMA, outStream);
            // compressedOutStream.write(SerializationUtils.serialize(object));
            outStream.writeObject(object);
        } catch (Exception e) {
            e.printStackTrace();
            Log.errorAndExit("File write error", EXITCODE.PROGERROR);
        }

        // This is an ugly workaround for apache's lack of LZMA Compression output
        String parentStr = "", fileName = filePath.getFileName().toString();
        Process process;
        if (parent == null) {
            Runtime runtime = Runtime.getRuntime();
            process = runtime.exec("lzma " + fileName);

        } else {
            parentStr = parent.toString();
            String cmd = "lzma "+fileName;
            File workingDir = new File(parentStr); 
            process = Runtime.getRuntime().exec(cmd, null, workingDir);
        }
        process.waitFor();
        return writer;
    }

    public Path getFilePath() {
        return filePath;
    }

    private void writeToFile(Object object, Path filePath) throws IOException {
        ObjectOutputStream outStream = null;
        try {
            outStream = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(filePath, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)));
            synchronized (outStream) {
                outStream.writeObject(object);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.errorAndExit("File write error", EXITCODE.PROGERROR);
        } finally {
            outStream.flush();
            outStream.close();
        }
    }

    public void appendToFile(Object object) throws IOException {
        AppendingObjectOutputStream outStream = null;
        try {
            outStream = new AppendingObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(this.filePath, StandardOpenOption.APPEND)));
            synchronized (outStream) {
                outStream.writeObject(object);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.errorAndExit("File write error", EXITCODE.PROGERROR);
        } finally {
            outStream.flush();
            outStream.close();
        }
    }
}
