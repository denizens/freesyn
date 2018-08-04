package bnw.abm.intg.apps.wdll.A4yrRm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import bnw.abm.intg.algov1.ipfp.GroupsBuilder;
import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.framework.models.IPFPTable;
import bnw.abm.intg.algov2.framework.models.LinkRules.LinkRulesWrapper;
import bnw.abm.intg.algov2.framework.models.Population;
import bnw.abm.intg.algov2.framework.models.Population.MatrixCell.ValueMode;
import bnw.abm.intg.algov2.templates.GroupTypeLogic;
import bnw.abm.intg.algov2.templates.IPFPTableBuilder;
import bnw.abm.intg.apps.wdll.HouseholdTypeLogic;
import bnw.abm.intg.apps.wdll.SummaryUtil;
import bnw.abm.intg.filemanager.csv.CSVWriter;
import bnw.abm.intg.util.BNWProperties;
import bnw.abm.intg.util.Log;
import ch.qos.logback.classic.Level;

public class MainAlgoV3Age4yrRelMarital {
    public static void build(String propertiesFile) {
        Log.createLogger("Init", Level.INFO, "bnw.abm.intg.init.3.log");
        Log.info("Reading program properties");
        BNWProperties props = null;
        try {
            props = new BNWProperties(propertiesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Path ipfpResultsHome = props.readFileOrDirectoryPath("IPFPResultHome");
        Path outputHome = props.readFileOrDirectoryPath("OutputHome");
        String seedCode = props.getProperty("Seed");
        int iterations = Integer.parseInt(props.getProperty("Iterations"));
        String errorMode = props.getProperty("ErrorMode");
        int randomSeed = Integer.parseInt(props.getProperty("RandomSeed"));
        String ageCode = "4yrGapAge";
        String relationshipCode = "MaritalRel";
        String sexCode = "Sex";
        int maxGroupConstructionAttempts = Integer.parseInt(props.getProperty("MaxGroupConstructionAttempts"));

        GroupTypeLogic groupTypeLogic = new HouseholdTypeLogic();

        Log.info("Registering domain constraints");

        new Age4yrRelMaritalInverseLinks(51).register();
        new Age4yrRelMaritalDependentLinks(51).register();
        // new LatchDependentLinks().register();
        // new FamilyTreeDegreeBasedRejection().register();
        // new RelativesBasedRejection().register();
        new Age4yrRelMaritalLinkRules().register();
        new Age4yrMaritalRelGroupRules().register();

        Map<GroupType, Integer> groupSizeMap = new HashMap<>(10);

        CSVWriter csvWriter = new CSVWriter();

        for (int i = 1; i <= 50; i++) {
            String parameterPrefix = relationshipCode + "_" + ageCode + "_" + seedCode;
            Path ipfpProportionsFile = ipfpResultsHome.resolve(i + File.separator + "ipfraw_" + sexCode + "_" + parameterPrefix + ".csv");
            Path ipfpAgentCountsFile = ipfpResultsHome.resolve(i + File.separator + "ipfresult_" + sexCode + "_" + parameterPrefix + ".csv");

            Log.info("Reading in IPFP tables of population " + i);
            IPFPTable ipfpProportionsTable = new IPFPTableBuilder(ipfpProportionsFile).build();
            IPFPTable ipfpAgentCountsTable = new IPFPTableBuilder(ipfpAgentCountsFile).build();

            long startTime = System.currentTimeMillis();
            Log.info("Constructing Groups");
            GroupsBuilder groupsBuilder = new GroupsBuilder(LinkRulesWrapper.getLinkRules(), ipfpAgentCountsTable, ipfpProportionsTable,
                    groupTypeLogic, maxGroupConstructionAttempts);
            Random randomGenerator = new Random(i);
            Population population = groupsBuilder.buildGroupWiseMonteCarlo(randomGenerator);
            if (errorMode.equals("Probability"))
                population = groupsBuilder.hillClimbingProportions(population, iterations, randomGenerator);
            else if (errorMode.equals("Counts"))
                population = groupsBuilder.hillClimbingCounts(population, iterations, randomGenerator);

            long elaspedTime = System.currentTimeMillis() - startTime;
            Log.info("Population construction complete: Elasped time = " + (elaspedTime / 1000) / 60 + "m " + (elaspedTime / 1000) % 60 + "s");

            population.setValueMode(ValueMode.AGENTCOUNT);

            Path outputFile = outputHome.resolve(parameterPrefix + File.separator + i + File.separator + "FullSummary.csv");// Population
            // 2D
            // table
            // (GroupType
            // AgentType)
            Path groupsSummaryOutputFile = outputHome.resolve(parameterPrefix + File.separator + i + File.separator + "GroupsSummary.csv");// Groups
            // summary
            Path agentsSummaryOutputFile = outputHome.resolve(parameterPrefix + File.separator + i + File.separator + "AgentsSummary.csv");
            Path agentsFile = outputHome.resolve(parameterPrefix + File.separator + i + File.separator + "Agents.csv");

            try {
                Files.createDirectories(outputFile.getParent());
            } catch (IOException e) {
                e.printStackTrace();
            }
            SummaryUtil.printFullSummary(population, outputFile);
            SummaryUtil.printGroupsSummary(population, groupsSummaryOutputFile);
            SummaryUtil.printAgentsSummary(population, agentsSummaryOutputFile);
            try {
                SummaryUtil.printAgentRecords(population, agentsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.info("THE END!");
    }
}
