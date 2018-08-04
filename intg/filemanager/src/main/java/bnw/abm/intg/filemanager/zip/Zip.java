package bnw.abm.intg.filemanager.zip;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import bnw.abm.intg.filemanager.Find;
import bnw.abm.intg.util.Log;

public class Zip {

    public static Reader read(Path zipFile, String fileInZip) throws IOException {
        FileSystem fs = FileSystems.newFileSystem(zipFile, null);
        return Files.newBufferedReader(fs.getPath(fileInZip));
    }

    public static void create(Path zipFile, List<Path> componentFiles, boolean deleteOriginal) throws IOException {

        try (ZipOutputStream zipStream = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            for (Path componentFile : componentFiles) {
                ZipEntry zipEntry = new ZipEntry(componentFile.getFileName().toString());
                zipStream.putNextEntry(zipEntry);

                InputStream inStream = Files.newInputStream(componentFile);
                int len;
                byte[] buffer = new byte[2048];
                while ((len = inStream.read(buffer)) > 0) {
                    zipStream.write(buffer, 0, len);
                }
                zipStream.flush();
                zipStream.closeEntry();
            }
        }

        for (Path file : componentFiles) {
            Files.delete(file);
        }
    }

    public static List<Path> findFiles(Path jarFile, String pattern) {
        HashMap<Path, FileSystem> fileSystems = new HashMap<Path, FileSystem>();
        FileSystem fs = fileSystems.get(jarFile);

        try {
            if (fs == null) {
                Map<String, String> env = new HashMap<>();
                env.put("create", "false");
                fs = FileSystems.newFileSystem(URI.create("jar:file:" + jarFile), env);
                fileSystems.put(jarFile, fs);
            }
        } catch (IOException ex) {
            Log.error(ex.toString(), ex);
        }

        Find finder = new Find(pattern, fs);
        try {
            for (Path root : fs.getRootDirectories()) {
                Files.walkFileTree(root, finder);
            }
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        ArrayList<Path> files = finder.getFilePaths();
        return files;
    }
}
