package bnw.abm.intg.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import bnw.abm.intg.util.GlobalConstants.EXITCODE;

public class BNWProperties extends Properties {
    /**
     * 
     */
    private static final long serialVersionUID = -5371179228765375673L;

    public BNWProperties(String propertyFile) throws IOException {
        this.loadPropertyFile(propertyFile);
    }

    public BNWProperties loadPropertyFile(String propertyFile) throws IOException {

        try (InputStream inputStream = Files.newInputStream(Paths.get(propertyFile))) {

            if (inputStream != null) {
                this.load(inputStream);
            } else {
                Log.errorAndExit("Property file loading : FAILED", EXITCODE.IOERROR);
            }
        } catch (IOException e) {
            Log.errorAndExit(e.toString(), e, EXITCODE.IOERROR);
        }
        return this;
    }

    /**
     * Reads property file entry as a HashMap. Each Map Entry must be separated by a comma and Key Value pair must be separated by a colon. e.g.
     * President=Name:James,Age:16.
     * 
     * @param propertyName
     * @return A HashMap of key value pairs
     */
    public HashMap<String, String> readKeyValuePairs(String propertyName) {
        String[] entries = readCommaSepProperties(propertyName);
        if (entries != null) {
            return splitByColon(entries);
        } else {
            return null;
        }
    }

    /**
     * Reads a property entry's values separated by commas as an array e.g. Names=Bhagya,Sewwandi
     * 
     * @param propertyName
     * @return
     */
    public String[] readCommaSepProperties(String propertyName) {
        String entry = this.getProperty(propertyName);
        entry = resolveEnvvar(entry);
        if (entry != null) {
            return entry.trim().split(",");
        } else {
            return null;
        }
    }

    /**
     * Reads a property entry with multiple values each separated by colons as an array. e.g. CsvFile="Test.csv:2:8"
     * 
     * @param propertyName
     * @return
     */
    public String[] readColonSeperatedProperties(String propertyName) {
        String entry = this.getProperty(propertyName);
        entry = resolveEnvvar(entry);
        if (entry != null) {
            return entry.trim().split(":");
        } else {
            return null;
        }
    }

    private HashMap<String, String> splitByColon(String[] entries) {

        HashMap<String, String> kvEntries = new HashMap<String, String>();
        for (int i = 0; i < entries.length; i++) {
            String[] arr = entries[i].split(":");
            if (arr.length == 1 || arr[1].trim().equals("")) {
                kvEntries.put(arr[0], null);
            } else {
                kvEntries.put(arr[0], arr[1]);
            }
        }
        return kvEntries;
    }

    public Path readFileOrDirectoryPath(String propertyName) {
        String entry = this.getProperty(propertyName);
        entry = resolveEnvvar(entry);
        return Paths.get(entry);
    }

    /**
     * Resolves environment variables that are in a file path
     * 
     * @param filePath
     *            File path String
     * @return returns filePath after replacing environment variable with correct file path
     */
    public static String resolveEnvvar(String filePath) {
        int dollar = filePath.indexOf('$');
        if (dollar != -1) {
            String envVar = filePath.substring(dollar, filePath.indexOf(File.separatorChar));
            String replacement = System.getenv(envVar.substring(1));
            if (replacement == null) {
                Log.errorAndExit(envVar.substring(1) + "  environment variable not found", EXITCODE.ENVVAR);
            }
            return filePath.replace(envVar, replacement);
        } else {
            return filePath;
        }
    }

    /**
     * Reads SA names list from user input
     *
     * @param propertyName       SA list property
     * @param inputDirectory The input data directory
     * @return List of SA2s
     * @throws IOException File reading
     */
    public List<String> getSAList(String propertyName, Path inputDirectory) throws IOException {
        List<String> saList = null;
        String saParam = this.getProperty(propertyName);
        if (saParam.equals("*")) {
            saList = Files.list(inputDirectory)
                          .map(Path::getFileName)
                          .map(Path::toString)
                          .collect(Collectors.toList());
        } else if (Files.exists(Paths.get(saParam))) {
            saList = Files.readAllLines(Paths.get(saParam));
        } else {
            saList = Arrays.asList(saParam.split(","));
        }
        return saList;
    }

}
