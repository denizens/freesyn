package bnw.abm.intg.apps.latch.ipucensus;

import bnw.abm.intg.algov1.nonipfp.GroupsOptimiser;
import bnw.abm.intg.algov1.nonipfp.NonIPFMonteCarloGroupsBuilder;
import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.framework.models.IPFPTable;
import bnw.abm.intg.algov2.framework.models.LinkRules;
import bnw.abm.intg.algov2.framework.models.Population;
import bnw.abm.intg.algov2.framework.models.Population.MatrixCell.ValueMode;
import bnw.abm.intg.algov2.templates.GroupTypeLogic;
import bnw.abm.intg.algov2.templates.IPFPTableBuilder;
import bnw.abm.intg.algov2.templates.LinkRulesBuilder;
import bnw.abm.intg.apps.latch.DataReader;
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

public class NonIPFSynthesiserWithIPUData {
    public static void build(String propertiesFile, String sa2s, String seeds) {
        long progStartTime = System.currentTimeMillis();
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
        Path ipfpProportionsFileName = props.readFileOrDirectoryPath("IPFPProportionsFileName");
        Path ipfpAgentCountsFileName = props.readFileOrDirectoryPath("IPFPAgentCountsFileName");
        Path seedFileName = props.readFileOrDirectoryPath("SeedFileName");
        Path populationSummaryFile = props.readFileOrDirectoryPath("PopulationSummaryOutputFileName");
        Path groupSummaryOutput = props.readFileOrDirectoryPath("GroupSummaryOutputFileName");
        Path agentSummaryOutput = props.readFileOrDirectoryPath("AgentSummaryOutputFileName");
        Path agentTypesFileName = props.readFileOrDirectoryPath("AgentTypesDistributionFileName");
        Path groupTypesFileName = props.readFileOrDirectoryPath("GroupTypesDistributionFileName");

        //Optimising
        int maxHillClimbingItrs = Integer.parseInt(props.getProperty("HillClimbingIterations"));
        double temperature = Double.parseDouble(props.getProperty("SimAnTemperature"));
        double coolingRate = Double.parseDouble(props.getProperty("SimAnCoolingRate"));
        double exitTemperature = Double.parseDouble(props.getProperty("SimAnExitTemperature"));


        int[] seedRange;
        if (!seeds.equals("")) {
            seedRange = Stream.of(seeds.split(":")).mapToInt(Integer::parseInt).toArray();
        } else {
            seedRange = Stream.of(props.readColonSeperatedProperties("randomSeedRange")).mapToInt(Integer::parseInt).toArray();
        }

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

//        try {
//            Files.createDirectories(outputHome);
//            Files.write(outputHome.resolve("LinkRules.csv"), LinkRulesWrapper.getLinkRules().toCSVString().getBytes());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
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


            for (int randSeed = seedRange[0]; randSeed <= seedRange[1]; randSeed++) {
                Log.info("Random seed: "+randSeed);
                long runStartTime = System.currentTimeMillis();
                Random rand = new Random(randSeed);
                NonIPFMonteCarloGroupsBuilder groupsBuilder = new NonIPFMonteCarloGroupsBuilder(LinkRules.LinkRulesWrapper.getLinkRules(),
                                                                                                targetGroupTypeCounts,
                                                                                                targetAgentTypeCounts,
                                                                                                groupTypeLogic,
                                                                                                seed,
                                                                                                1000,
                                                                                                rand);
                Population population = groupsBuilder.build();
                GroupsOptimiser groupsOptimiser = new GroupsOptimiser(seed,
                                                                      targetAgentTypeCounts,
                                                                      targetGroupTypeCounts,
                                                                      groupTypeLogic,
                                                                      rand);
                //                groupsOptimiser.simulatedAnnealingProportions(population, rand, temperature, coolingRate, exitTemperature);
                population = groupsOptimiser.hillClimbingCounts(population, maxHillClimbingItrs);
                //                population = groupsOptimiser.simulatedAnnealingCounts(population, rand, temperature, coolingRate,exitTemperature);

                population.setValueMode(ValueMode.AGENTCOUNT);
                Path outputFile = outputHome.resolve(sa2 + File.separator + "population" + File.separator
                                                             + randSeed + File.separator + populationSummaryFile);
                Path groupsOutputFile = outputHome.resolve(sa2 + File.separator + "population" + File.separator + randSeed + File.separator + groupSummaryOutput);
                Path agentsOutputFile = outputHome.resolve(sa2 + File.separator + "population" + File.separator + randSeed + File.separator + agentSummaryOutput);

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
                Log.info("Households file: "+groupsOutputFile);

                try (CSVPrinter p = new CSVPrinter(Files.newBufferedWriter(agentsOutputFile), CSVFormat.DEFAULT)) {
                    List<List<Integer>> agents = new ArrayList<>();
                    for (Entry<ReferenceAgentType, Integer> agent : population.getAgentsSummary().entrySet()) {
                        agents.add(new ArrayList<>(Arrays.asList(agent.getKey().getTypeID(), agent.getValue())));
                    }
                    p.printRecords(agents);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.info("Persons file: "+agentsOutputFile);

                Path errorFile = outputHome.resolve(sa2 + File.separator + "population"+ File.separator + randSeed + File.separator + "error_dist.csv");
                try (CSVPrinter p = new CSVPrinter(Files.newBufferedWriter(errorFile), CSVFormat.DEFAULT)) {
                    p.printRecords(population.getErrorList());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.info("Error evolution file: "+errorFile);

                long popInstanceTime = System.currentTimeMillis() - runStartTime;
                Log.info("Population instance created in: " + (popInstanceTime / 1000) / 60 + "m " + (popInstanceTime / 1000) %
                        60 + "s");
            }
        }
        long progEndTime = progStartTime - System.currentTimeMillis();
        Log.info("Total execution time: " + (progEndTime / 1000) / 60 + "m " + (progEndTime / 1000) % 60 + "s");
        Log.info("THE END!");
    }

}