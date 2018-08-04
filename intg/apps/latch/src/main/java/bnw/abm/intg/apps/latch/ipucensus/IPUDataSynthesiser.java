package bnw.abm.intg.apps.latch.ipucensus;

import bnw.abm.intg.algov1.ipfp.GroupsBuilder;
import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.framework.models.IPFPTable;
import bnw.abm.intg.algov2.framework.models.LinkRules;
import bnw.abm.intg.algov2.framework.models.Population;
import bnw.abm.intg.algov2.templates.GroupTypeLogic;
import bnw.abm.intg.algov2.templates.IPFPTableBuilder;
import bnw.abm.intg.algov2.templates.LinkRulesBuilder;
import bnw.abm.intg.apps.latch.DataReader;
import bnw.abm.intg.filemanager.csv.CSVWriter;
import bnw.abm.intg.filemanager.zip.Zip;
import bnw.abm.intg.util.BNWProperties;
import bnw.abm.intg.util.GlobalConstants;
import bnw.abm.intg.util.Log;
import ch.qos.logback.classic.Level;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

/**
 * @author wniroshan 13 May 2018
 */
public class IPUDataSynthesiser {
    public static void build(String propertiesFile, String sa2s, String seeds) {
        Log.createLogger("Init", Level.INFO, "bnw.abm.intg.init.3.log");
        Log.info("Reading program properties");
        BNWProperties props = null;
        try {
            props = new BNWProperties(propertiesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.info("Current directory: " + System.getProperty("user.dir"));
        Path ipfpResultsHome = props.readFileOrDirectoryPath("IPFPResultHome");
        List<String> sa2Names = null;
        if (!sa2s.equals("")) {
            sa2Names = new ArrayList<>(Arrays.asList(sa2s.split(",")));
        } else {
            try {
                sa2Names = props.getSAList("SA2Names", ipfpResultsHome);
            } catch (IOException e) {
                Log.errorAndExit("SA2 names reading failed", e, GlobalConstants.EXITCODE.USERINPUT);
            }
        }
        assert sa2Names != null;
        Path outputHome = props.readFileOrDirectoryPath("OutputHome");

        Path ipfpProportionsFileName = props.readFileOrDirectoryPath("IPFPProportionsFileName");
        Path ipfpAgentCountsFileName = props.readFileOrDirectoryPath("IPFPAgentCountsFileName");
        Path populationSummaryFile = props.readFileOrDirectoryPath("PopulationSummaryOutputFileName");
        Path groupSummaryOutput = props.readFileOrDirectoryPath("GroupSummaryOutputFileName");
        Path agentSummaryOutput = props.readFileOrDirectoryPath("AgentSummaryOutputFileName");
        int maxGroupConstructionAttempts = Integer.parseInt(props.getProperty("MaxGroupConstructionAttempts"));
        int[] seedRange;
        if (!seeds.equals("")) {
            seedRange = Stream.of(seeds.split(":")).mapToInt(Integer::parseInt).toArray();
        } else {
            seedRange = Stream.of(props.readColonSeperatedProperties("randomSeedRange")).mapToInt(Integer::parseInt).toArray();
        }
        int hillClimbingItrs = Integer.parseInt(props.getProperty("HillClimbingIterations"));
        GroupTypeLogic groupTypeLogic = new IPUDataGroupType();

        Log.info("Registering domain constraints");

        new IPUDataLinkConditions.InverseLinks().register();
        new IPUDataLinkConditions.DependentLinks().register();
        new IPUDataRejectionCriteria.FamilyTreeDegreeBasedRejection().register();
        new IPUDataRejectionCriteria.RelativesBasedRejection().register();
        new IPUDataRejectionCriteria.HouseholdTypeBasedRejection().register();
        new IPUDataGroupRules().register();

        LinkRulesBuilder sa4LinkRules = new IPUDataLinkRulesBuilder();
        sa4LinkRules.register();

        Map<GroupType, Integer> groupSizeMap = null;

        CSVWriter csvWriter = new CSVWriter();
        for (String sa2 : sa2Names) {

            Path ipfpProportionsFile = ipfpResultsHome.resolve(sa2 + File.separator + ipfpProportionsFileName);
            Path ipfpAgentCountsFile = ipfpResultsHome.resolve(sa2 + File.separator + ipfpAgentCountsFileName);


            Log.info("Reading in IPFP tables of " + sa2);

            IPFPTable ipfpProportionsTable = new IPFPTableBuilder(ipfpProportionsFile).build();
            IPFPTable ipfpAgentCountsTable = new IPFPTableBuilder(ipfpAgentCountsFile).build();

            long startTime = System.currentTimeMillis();
            Log.info("Constructing Groups");
            for (int randSeed = seedRange[0]; randSeed <= seedRange[1]; randSeed++) {

                GroupsBuilder groupsBuilder = new GroupsBuilder(LinkRules.LinkRulesWrapper.getLinkRules(),
                                                                ipfpAgentCountsTable,
                                                                ipfpProportionsTable,
                                                                groupTypeLogic,
                                                                maxGroupConstructionAttempts);
                Random randomGenerator = new Random(randSeed);

                Population population = groupsBuilder.buildGroupWiseMonteCarlo(randomGenerator);
                if(population.size() > 0) {
                    Log.info("Started Hill Climbing..");
                    population = groupsBuilder.hillClimbingCounts(population, hillClimbingItrs, randomGenerator);
                }else{
                    Log.info("Population is empty: not running hill climbing");
                }
                long elaspedTime = System.currentTimeMillis() - startTime;
                Log.info("Population construction complete: Elasped time = " + (elaspedTime / 1000) / 60 + "m " + (elaspedTime / 1000) % 60
                                 + "s");

                population.setValueMode(Population.MatrixCell.ValueMode.AGENTCOUNT);
                // Population as 2D table(GroupType, AgentType)
                Path outputFile = outputHome.resolve(sa2 + File.separator + "population" + File.separator + randSeed + File.separator + populationSummaryFile);
                // Groups summary
                Path groupsOutputFile = outputHome.resolve(sa2 + File.separator + "population" + File.separator + randSeed + File.separator + groupSummaryOutput);
                Path agentsOutputFile = outputHome.resolve(sa2 + File.separator + "population" + File.separator + randSeed + File.separator + agentSummaryOutput);

                try {
                    Files.createDirectories(outputFile.getParent());
                    csvWriter.writeAsCsv(Files.newBufferedWriter(outputFile), population);
                    Zip.create(Paths.get(outputFile.toString() + ".zip"), new ArrayList<>(Arrays.asList(outputFile)), true);
                } catch (Exception e) {
                    Log.error("File write failed", e);
                }

                try (CSVPrinter p = new CSVPrinter(Files.newBufferedWriter(groupsOutputFile), CSVFormat.DEFAULT)) {
                    List<List<Integer>> groups = new ArrayList<>();
                    for (Entry<GroupType, Integer> group : population.getGroupsSummary().entrySet()) {
                        groups.add(Arrays.asList(group.getKey().getID(), group.getValue()));
                    }
                    p.printRecords(groups);
                } catch (Exception e) {
                    Log.error("File write failed", e);
                }
                try {
                    Zip.create(Paths.get(groupsOutputFile.toString() + ".zip"), new ArrayList<>(Arrays.asList(groupsOutputFile)), true);
                } catch (Exception e) {
                    Log.error("File write failed", e);
                }

                try (CSVPrinter p = new CSVPrinter(Files.newBufferedWriter(agentsOutputFile), CSVFormat.DEFAULT)) {
                    List<List<Integer>> agents = new ArrayList<>();
                    for (Entry<ReferenceAgentType, Integer> agent : population.getAgentsSummary().entrySet()) {
                        agents.add(new ArrayList<>(Arrays.asList(agent.getKey().getTypeID(), agent.getValue())));
                    }
                    p.printRecords(agents);
                } catch (Exception e) {
                    Log.error("File write failed", e);
                }
                try {
                    Zip.create(Paths.get(agentsOutputFile.toString() + ".zip"), new ArrayList<>(Arrays.asList(agentsOutputFile)), true);
                } catch (Exception e) {
                    Log.error("File write failed", e);
                }

                Log.info("Agents file written to: " + agentsOutputFile);
                Log.info("Groups file written to: " + groupsOutputFile);
            }
        }
        Log.info("THE END!");
    }
}
