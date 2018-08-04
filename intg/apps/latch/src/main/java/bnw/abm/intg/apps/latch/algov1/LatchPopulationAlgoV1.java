package bnw.abm.intg.apps.latch.algov1;

import bnw.abm.intg.algov1.ipfp.GroupsBuilder;
import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.framework.models.IPFPTable;
import bnw.abm.intg.algov2.framework.models.LinkRules.LinkRulesWrapper;
import bnw.abm.intg.algov2.framework.models.Population;
import bnw.abm.intg.algov2.framework.models.Population.MatrixCell.ValueMode;
import bnw.abm.intg.algov2.templates.GroupTypeLogic;
import bnw.abm.intg.algov2.templates.IPFPTableBuilder;
import bnw.abm.intg.algov2.templates.LinkRulesBuilder;
import bnw.abm.intg.apps.latch.algov2.LatchLinkConditions.DependentLinks;
import bnw.abm.intg.apps.latch.algov2.LatchLinkConditions.InverseLinks;
import bnw.abm.intg.apps.latch.algov2.LatchLinkRulesBuilder;
import bnw.abm.intg.apps.latch.algov2.LatchRejectionCriteria.FamilyTreeDegreeBasedRejection;
import bnw.abm.intg.apps.latch.algov2.LatchRejectionCriteria.HouseholdTypeBasedRejection;
import bnw.abm.intg.apps.latch.algov2.LatchRejectionCriteria.RelativesBasedRejection;
import bnw.abm.intg.apps.latch.algov2.MultiFamilyHhTypeLogic;
import bnw.abm.intg.filemanager.csv.CSVWriter;
import bnw.abm.intg.filemanager.zip.Zip;
import bnw.abm.intg.util.BNWProperties;
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
 * Hello world!
 */
public class LatchPopulationAlgoV1 {
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
        String[] sa2Names;
        if (!sa2s.equals("")) {
            sa2Names = sa2s.split(",");
        } else {
            sa2Names = props.readCommaSepProperties("SA2Names");
        }
        Path outputHome = props.readFileOrDirectoryPath("OutputHome");
        Path ipfpProportionsFileName = props.readFileOrDirectoryPath("IPFPProportionsFileName");
        Path ipfpAgentCountsFileName = props.readFileOrDirectoryPath("IPFPAgentCountsFileName");
        Path populationSummaryFile = props.readFileOrDirectoryPath("PopulationSummaryOutputFileName");
        Path groupSummaryOutput = props.readFileOrDirectoryPath("GroupSummaryOutputFileName");
        Path agentSummaryOutput = props.readFileOrDirectoryPath("AgentSummaryOutputFileName");
        Path optmisationErrorOutput = props.readFileOrDirectoryPath("OptimisationErrorChangeOutputFileName");
        int maxGroupConstructionAttempts = Integer.parseInt(props.getProperty("MaxGroupConstructionAttempts"));
        int[] seedRange;
        if (!seeds.equals("")) {
            seedRange = Stream.of(seeds.split(":")).mapToInt(Integer::parseInt).toArray();
        } else {
            seedRange = Stream.of(props.readColonSeperatedProperties("randomSeedRange")).mapToInt(Integer::parseInt).toArray();
        }
        int hillClimbingItrs = Integer.parseInt(props.getProperty("HillClimbingIterations"));
        GroupTypeLogic groupTypeLogic = new MultiFamilyHhTypeLogic();

        Log.info("Registering domain constraints");

        new InverseLinks().register();
        new DependentLinks().register();
        new FamilyTreeDegreeBasedRejection().register();
        new RelativesBasedRejection().register();
        new HouseholdTypeBasedRejection().register();
        new AlgoV1LatchGroupRules().register();

        LinkRulesBuilder algov1LatchRules = new LatchLinkRulesBuilder();
        algov1LatchRules.register();

        Map<GroupType, Integer> groupSizeMap = null;

        CSVWriter csvWriter = new CSVWriter();
        for (String sa2 : sa2Names) {
            for (int randSeed = seedRange[0]; randSeed <= seedRange[1]; randSeed++) {
                Path ipfpProportionsFile = ipfpResultsHome.resolve(sa2 + File.separator + ipfpProportionsFileName);
                Path ipfpAgentCountsFile = ipfpResultsHome.resolve(sa2 + File.separator + ipfpAgentCountsFileName);
                Log.info("Reading in IPFP tables of " + sa2);
                IPFPTable ipfpProportionsTable = new IPFPTableBuilder(ipfpProportionsFile).build();
                IPFPTable ipfpAgentCountsTable = new IPFPTableBuilder(ipfpAgentCountsFile).build();

                long startTime = System.currentTimeMillis();
                Log.info("Constructing Groups");
                GroupsBuilder groupsBuilder = new GroupsBuilder(LinkRulesWrapper.getLinkRules(), ipfpAgentCountsTable, ipfpProportionsTable,
                                                                groupTypeLogic, maxGroupConstructionAttempts);
                Random randomGenerator = new Random(randSeed);

                Population population = groupsBuilder.buildGroupWiseMonteCarlo(randomGenerator);
                Log.info("Started Hill Climbing..");
                population = groupsBuilder.hillClimbingCounts(population, hillClimbingItrs, randomGenerator);

                long elaspedTime = System.currentTimeMillis() - startTime;
                Log.info("Population construction complete: Elasped time = " + (elaspedTime / 1000) / 60 + "m " + (elaspedTime / 1000) % 60
                                 + "s");

                population.setValueMode(ValueMode.AGENTCOUNT);
                // Population as 2D table(GroupType, AgentType)
                Path outputFile = outputHome.resolve(sa2 + File.separator + "population" + File.separator+ randSeed + File.separator + populationSummaryFile);
                // Groups summary
                Path groupsOutputFile = outputHome.resolve(sa2 + File.separator + "population" + File.separator + randSeed + File.separator + groupSummaryOutput);
                Path agentsOutputFile = outputHome.resolve(sa2 + File.separator + "population" + File.separator + randSeed + File.separator + agentSummaryOutput);

                try {
                    Files.createDirectories(outputFile.getParent());
                    csvWriter.writeAsCsv(Files.newBufferedWriter(outputFile), population);
                    Zip.create(Paths.get(outputFile.toString() + ".zip"), new ArrayList<>(Arrays.asList(outputFile)), true);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try (CSVPrinter p = new CSVPrinter(Files.newBufferedWriter(groupsOutputFile), CSVFormat.DEFAULT)) {
                    List<List<Integer>> groups = new ArrayList<>();
                    for (Entry<GroupType, Integer> group : population.getGroupsSummary().entrySet()) {
                        groups.add(Arrays.asList(group.getKey().getID(), group.getValue()));
                    }
                    p.printRecords(groups);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try (CSVPrinter p = new CSVPrinter(Files.newBufferedWriter(agentsOutputFile), CSVFormat.DEFAULT)) {
                    List<List<Integer>> agents = new ArrayList<>();
                    for (Entry<ReferenceAgentType, Integer> agent : population.getAgentsSummary().entrySet()) {
                        agents.add(new ArrayList<>(Arrays.asList(agent.getKey().getTypeID(), agent.getValue())));
                    }
                    p.printRecords(agents);
                } catch (IOException e) {
                    e.printStackTrace();
                }


                Log.info("Agents file written to: " + agentsOutputFile);
                Log.info("Groups file written to: " + groupsOutputFile);
            }
        }
        Log.info("THE END!");
    }
}