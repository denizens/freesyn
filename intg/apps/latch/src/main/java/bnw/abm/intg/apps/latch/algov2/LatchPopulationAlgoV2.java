package bnw.abm.intg.apps.latch.algov2;

import bnw.abm.intg.algov2.framework.models.AgentType;
import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.framework.models.IPFPTable;
import bnw.abm.intg.algov2.framework.models.LinkRules.LinkRulesWrapper;
import bnw.abm.intg.algov2.framework.models.Population.MatrixCell;
import bnw.abm.intg.algov2.population.PopulationBuilder;
import bnw.abm.intg.algov2.templates.GroupTemplatesBuilder;
import bnw.abm.intg.algov2.templates.LinkRulesBuilder;
import bnw.abm.intg.apps.latch.algov2.LatchRejectionCriteria.FamilyTreeDegreeBasedRejection;
import bnw.abm.intg.apps.latch.algov2.LatchRejectionCriteria.RelativesBasedRejection;
import bnw.abm.intg.filemanager.BNWFiles;
import bnw.abm.intg.util.BNWProperties;
import bnw.abm.intg.util.Log;
import ch.qos.logback.classic.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class LatchPopulationAlgoV2 {
    public static void build(String propertiesFile, int fromAgentType, int toAgentType) {
        DateFormat dateFormat = new SimpleDateFormat("dd:MM:yy-HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        String logSuffix = dateFormat.format(cal.getTime());
        Log.createLogger("LatchAlgoV2", Level.INFO, "bnw.abm.intg.apps.latch.algov2." + logSuffix + ".log");
        Log.info("Reading program properties");

        BNWProperties props = null;
        try {
            props = new BNWProperties(propertiesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int maxGroupSize = Integer.parseInt(props.getProperty("MaxGroupSize"));
        Path templateFilesDirectory = props.readFileOrDirectoryPath("TemplateFilesDirectory");
        Path templatesOutputFileName = props.readFileOrDirectoryPath("TemplatesOutputFileName");
        Path ipfpResultsHome = props.readFileOrDirectoryPath("IPFPResultHome");
        Path agentTypesFileName = props.readFileOrDirectoryPath("AgentTypesDistributionFileName");

        Log.info("Registering domain constraints");
        new LatchLinkConditions.DependentLinks().register();
        new LatchLinkConditions.InverseLinks().register();
        new FamilyTreeDegreeBasedRejection().register();
        new RelativesBasedRejection().register();

        long startTime = System.currentTimeMillis();
        Log.info("Constructing group templates");
        LinkRulesBuilder lrb = new LatchLinkRulesBuilder();
        lrb.register();

        //        Path agentTypesFile = ipfpResultsHome.resolve("Alphington - Fairfield" + File.separator + agentTypesFileName);
        //        Map<ReferenceAgentType, Integer> expectedAgentTypeCounts = null;
        //        Map<GroupType, Integer> targetGroupTypeCounts = null;
        //        try {
        //            expectedAgentTypeCounts = LatchUtils.readAgentTypesDistribution(agentTypesFile);
        //        } catch (IOException ioe) {
        //            ioe.printStackTrace();
        //        }
        //        lrb.filterEmptyAgentTypes(expectedAgentTypeCounts);

        GroupTemplatesBuilder templateBuider = null;
        int numOfTemplates = 0;

        Path templatesOutputFilePath = null;
        if (templateFilesDirectory == null) {
            templatesOutputFilePath = Paths.get(BNWFiles.getFileName(templatesOutputFileName) + "_" + fromAgentType + "_" + toAgentType +
                                                        "."
                                                        + BNWFiles.getFileExtention(templatesOutputFileName));
        } else {
            templatesOutputFilePath = templateFilesDirectory.resolve(BNWFiles.getFileName(templatesOutputFileName) + "_" + fromAgentType
                                                                             + "_"
                                                                             + toAgentType + "." + BNWFiles.getFileExtention(
                    templatesOutputFileName));
        }
        try {
            templateBuider = new GroupTemplatesBuilder(new MultiFamilyHhTypeLogic(), maxGroupSize, templatesOutputFilePath);
            Files.write(Paths.get("LinkRules.txt"), LinkRulesWrapper.getLinkRules().toFormattedString().getBytes());
            numOfTemplates = templateBuider.build(fromAgentType, toAgentType);
            Log.info("Total number of Group Templates: " + numOfTemplates);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        long elaspedTime = System.currentTimeMillis() - startTime;
        Log.info("GroupTemplates construction complete: Elasped time = " + (elaspedTime / 1000) / 60 + "m " + (elaspedTime / 1000) % 60 +
                         "s");
    }
}

class PopulationByRMSE extends PopulationBuilder {

    public PopulationByRMSE(IPFPTable populationDistribution, List<GroupTemplate> groupTemplates, long popSize) {
        super(populationDistribution, groupTemplates, popSize);
    }

    protected double calculateError(GroupTemplate group) {
        // Using Root Mean Squared Error
        double squaredSum = 0;
        for (GroupType row : population.groupTypes()) {
            for (AgentType col : population.agentTypes()) {
                MatrixCell c = population.get(row, col);
                if (group.getGroupType() == row) {
                    int numberOfAgentsOfCurrentTypeInGroup = 0;
                    for (Member m : group.getAllMembers()) {
                        if (m.isSameType(col)) {
                            numberOfAgentsOfCurrentTypeInGroup++;
                        }
                    }
                    // (y -Y)^2
                    squaredSum += Math.pow(c.getTargetProportion()
                                                   - ((c.getAgentCount() + numberOfAgentsOfCurrentTypeInGroup) /
                                                   getExptectedPopulationSize()),
                                           2);
                } else {
                    squaredSum += Math.pow(c.getTargetProportion() - (c.getAgentCount() / getExptectedPopulationSize()), 2);
                }
            }
        }
        return Math.sqrt(squaredSum / (double) population.cellCount());
    }
}