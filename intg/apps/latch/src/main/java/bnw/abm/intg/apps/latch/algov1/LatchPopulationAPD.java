package bnw.abm.intg.apps.latch.algov1;

import bnw.abm.intg.algov1.apd.APDGroupsBuilder;
import bnw.abm.intg.algov1.nonipfp.GroupsOptimiser;
import bnw.abm.intg.algov1.nonipfp.NonIPFMonteCarloGroupsBuilder;
import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.framework.models.IPFPTable;
import bnw.abm.intg.algov2.framework.models.LinkRules.LinkRulesWrapper;
import bnw.abm.intg.algov2.framework.models.Population;
import bnw.abm.intg.algov2.framework.models.Population.MatrixCell.ValueMode;
import bnw.abm.intg.algov2.templates.GroupTypeLogic;
import bnw.abm.intg.algov2.templates.IPFPTableBuilder;
import bnw.abm.intg.algov2.templates.LinkRulesBuilder;
import bnw.abm.intg.apps.latch.DataReader;
import bnw.abm.intg.apps.latch.algov2.LatchLinkConditions.DependentLinks;
import bnw.abm.intg.apps.latch.algov2.LatchLinkConditions.InverseLinks;
import bnw.abm.intg.apps.latch.algov2.LatchLinkRulesBuilder;
import bnw.abm.intg.apps.latch.algov2.LatchRejectionCriteria;
import bnw.abm.intg.apps.latch.algov2.LatchRejectionCriteria.FamilyTreeDegreeBasedRejection;
import bnw.abm.intg.apps.latch.algov2.LatchRejectionCriteria.RelativesBasedRejection;
import bnw.abm.intg.apps.latch.algov2.MultiFamilyHhTypeLogic;
import bnw.abm.intg.filemanager.csv.CSVWriter;
import bnw.abm.intg.util.BNWProperties;
import bnw.abm.intg.util.Log;
import ch.qos.logback.classic.Level;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

/**
 * Hello world!
 */
public class LatchPopulationAPD {
    private static final String ALGO_VERSION = "apd";
    public static void build(String propertiesFile, String sa2s, String seeds) {
        Log.createLogger("Init", Level.INFO, "bnw.abm.intg.init.3.log");
        Log.info("Reading program properties");
        BNWProperties props = null;
        try {
            props = new BNWProperties(propertiesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] sa2Names;
        if (!sa2s.equals("")) {
            sa2Names = sa2s.split(",");
        } else {
            sa2Names = props.readCommaSepProperties("SA2Names");
        }

        Path ipfpResultsHome = props.readFileOrDirectoryPath("IPFPResultHome");
        Path outputHome = props.readFileOrDirectoryPath("OutputHome");
        Path ipfpProportionsFileName = props.readFileOrDirectoryPath("IPFPproportionsFileName");
        Path ipfpAgentCountsFileName = props.readFileOrDirectoryPath("IPFPAgentCountsFileName");
        Path seedFileName = props.readFileOrDirectoryPath("SeedFileName");
        Path populationSummaryFile = props.readFileOrDirectoryPath("PopulationSummaryOutputFileName");
        Path groupSummaryOutput = props.readFileOrDirectoryPath("GroupSummaryOutputFileName");
        Path agentSummaryOutput = props.readFileOrDirectoryPath("AgentSummaryOutputFileName");
        Path agentTypesFileName = props.readFileOrDirectoryPath("AgentTypesDistributionFileName");
        Path groupTypesFileName = props.readFileOrDirectoryPath("GroupTypesDistributionFileName");
        int maxHillClimbingItrs = Integer.parseInt(props.getProperty("HillClimbingIterations"));
        int[] seedRange;
        if (!seeds.equals("")) {
            seedRange = Stream.of(seeds.split(":")).mapToInt(Integer::parseInt).toArray();
        } else {
            seedRange = Stream.of(props.readColonSeperatedProperties("randomSeedRange")).mapToInt(Integer::parseInt).toArray();
        }

        GroupTypeLogic groupTypeLogic = new MultiFamilyHhTypeLogic();

        Log.info("Registering domain constraints");

        new InverseLinks().register();
        new DependentLinks().register();
        new FamilyTreeDegreeBasedRejection().register();
        new RelativesBasedRejection().register();
        new LatchRejectionCriteria.HouseholdTypeBasedRejection().register();
        new AlgoV1LatchGroupRules().register();

        LinkRulesBuilder algov1LatchRules = new LatchLinkRulesBuilder();
        algov1LatchRules.register();

        try {
            Files.write(outputHome.resolve("LinkRules.csv"),LinkRulesWrapper.getLinkRules().toCSVString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<GroupType, Integer> groupSizeMap = null;

        CSVWriter csvWriter = new CSVWriter();
        for (String sa2 : sa2Names) {
            Log.info("Constructing Groups: " + sa2);
            System.out.println("Constructing Groups: " + sa2);
            Path seedFile = ipfpResultsHome.resolve(sa2 + File.separator + seedFileName);
            Path agentTypesFile = ipfpResultsHome.resolve(sa2 + File.separator + agentTypesFileName);
            Path groupTypesFile = ipfpResultsHome.resolve(sa2 + File.separator + groupTypesFileName);
            IPFPTable seed = new IPFPTableBuilder(seedFile).build();

            LinkedHashMap<ReferenceAgentType, Integer> targetAgentTypeCounts = null;
            LinkedHashMap<GroupType, Integer> targetGroupTypeCounts = null;
            try {
                targetAgentTypeCounts = DataReader.readAgentTypesGz(agentTypesFile);
                targetGroupTypeCounts = DataReader.readGroupTypesGz(groupTypesFile);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            if (groupSizeMap == null) {
                groupSizeMap = new HashMap<>();
                for (GroupType gType : targetGroupTypeCounts.keySet()) {
                    int size = (gType.getID() / 14) + 1;
                    groupSizeMap.put(gType, size);
                }
            }

            long startTime = System.currentTimeMillis();
            for (int randSeed = seedRange[0]; randSeed <= seedRange[1]; randSeed++) {
                Random rand = new Random(randSeed);
                APDGroupsBuilder groupsBuilder = new APDGroupsBuilder(LinkRulesWrapper.getLinkRules(),
                                                                                   targetGroupTypeCounts,
                                                                                   targetAgentTypeCounts,
                                                                                   groupTypeLogic,
                                                                                   seed,
                                                                                   1000,
                                                                                   rand);
                Population population = groupsBuilder.build();
//                GroupsOptimiser groupsOptimiser = new GroupsOptimiser(seed, expectedAgentTypeCounts, expectedGroupTypeCounts, groupTypeLogic, rand);
//                groupsOptimiser.hillClimbingCounts(population,maxHillClimbingItrs);

                long elaspedTime = System.currentTimeMillis() - startTime;
                Log.info("Population construction complete: Elasped time = " + (elaspedTime / 1000) / 60 + "m " + (elaspedTime / 1000) %
                        60 + "s");

                population.setValueMode(ValueMode.AGENTCOUNT);
                Path outputFile = outputHome.resolve(sa2 + File.separator + "population" + File.separator + ALGO_VERSION + File.separator
                                                             + randSeed + File.separator + populationSummaryFile);
                Path groupsOutputFile = outputHome.resolve(sa2 + File.separator + "population" + File.separator + ALGO_VERSION + File
                        .separator + randSeed + File.separator + groupSummaryOutput);
                Path agentsOutputFile = outputHome.resolve(sa2 + File.separator + "population" + File.separator + ALGO_VERSION + File
                        .separator + randSeed + File.separator + agentSummaryOutput);
                try {
                    Files.createDirectories(outputFile.getParent());
                    csvWriter.writeAsCsv(Files.newBufferedWriter(outputFile), population);
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
            }
        }
        Log.info("THE END!");
    }


}