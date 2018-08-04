package bnw.abm.intg.apps.latch.algov2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import ch.qos.logback.classic.Level;
import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.population.LoadGroupTemplates;
import bnw.abm.intg.filemanager.BNWFiles;
import bnw.abm.intg.filemanager.obj.Writer;
import bnw.abm.intg.util.BNWProperties;
import bnw.abm.intg.util.Log;

public class TemplateUtil {

    public static Map<GroupType, List<GroupTemplate>> readGroupTemplatesFromFiles(String propertiesFile) throws IOException {
        BNWProperties properties = new BNWProperties(propertiesFile);
        return readGroupTemplatesFromFiles(properties);
    }

    public static Map<GroupType, List<GroupTemplate>> readGroupTemplatesFromFiles(BNWProperties properties) {
        DateFormat dateFormat = new SimpleDateFormat("dd:MM:yy-HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        String logSuffix = dateFormat.format(cal.getTime());
        Log.createLogger("LatchAlgoV2TemplateProcess", Level.INFO, "bnw.abm.intg.apps.latch.tp." + logSuffix + ".log");

        Path combinedCleanedTemplates = properties.readFileOrDirectoryPath("CombinedCleanedTemplates");
        Path templateFilesDirectory = properties.readFileOrDirectoryPath("TemplateFilesDirectory");
        List<Path> templateFiles = BNWFiles.find(templateFilesDirectory, "*.obj.lzma");
        int templatesProcessorThreadPoolSize = Integer.parseInt(properties.getProperty("TemplatesProcessorThreadPoolSize"));
        int maxTemplatesInAThread = Integer.parseInt(properties.getProperty("MaxTemplatesInAProcessorThread"));

        Log.info("Reading stored Group Templates from... ");
        templateFiles.forEach(p -> Log.info(p.toString()));

        LoadGroupTemplates templatesLoader = new LoadGroupTemplates() {

            @Override
            public boolean filter(GroupType groupType) {
                if (groupType.equals(GroupType.getExisting(MultiFamilyHhTypeLogic.getGroupTypes().size() - 1))) {
                    return false;
                }
                return true;
            }
        };

        HashMap<GroupType, List<GroupTemplate>> groupTemplates = new HashMap<>();
        try {
            Files.deleteIfExists(combinedCleanedTemplates);
            templatesLoader.load(groupTemplates, templateFiles, templatesProcessorThreadPoolSize, maxTemplatesInAThread);
            Writer.createAndWriteToFileLZMA(groupTemplates, combinedCleanedTemplates);
        } catch (ClassNotFoundException | IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        Log.info("Processed Templates file written to: " + combinedCleanedTemplates);
        return groupTemplates;
    }
}
