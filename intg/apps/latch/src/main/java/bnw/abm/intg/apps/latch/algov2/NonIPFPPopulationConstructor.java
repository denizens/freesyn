package bnw.abm.intg.apps.latch.algov2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import bnw.abm.intg.algov2.framework.models.AgentType;
import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.framework.models.Population;
import bnw.abm.intg.algov2.framework.models.Population.MatrixCell.ValueMode;
import bnw.abm.intg.algov2.nonipfp.population.NonIPFPPopulationBuilder;
import bnw.abm.intg.apps.latch.LatchUtils;
import bnw.abm.intg.filemanager.csv.CSVWriter;
import bnw.abm.intg.filemanager.obj.Reader;
import bnw.abm.intg.util.BNWProperties;
import bnw.abm.intg.util.Log;
import ch.qos.logback.classic.Level;

public class NonIPFPPopulationConstructor {
    public static void construct(String propertiesFile) {

        DateFormat dateFormat = new SimpleDateFormat("dd:MM:yy-HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        String logSuffix = dateFormat.format(cal.getTime());
        Log.createLogger("LatchAlgoV2Population", Level.INFO, "bnw.abm.intg.apps.latch.algov2.nonipfp." + logSuffix + ".log");

        BNWProperties props = null;
        try {
            props = new BNWProperties(propertiesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Path outputHome = props.readFileOrDirectoryPath("OutputHome");
        Path groupSummaryOutput = props.readFileOrDirectoryPath("GroupSummaryOutputFileName");
        Path agentSummaryOutput = props.readFileOrDirectoryPath("AgentSummaryOutputFileName");
        Path populationSummaryFile = props.readFileOrDirectoryPath("PopulationSummaryOutputFileName");
        Path combinedCleanedTemplates = props.readFileOrDirectoryPath("CombinedCleanedTemplates");

        Path seedFileName = props.readFileOrDirectoryPath("SeedFileName");
        Path agentTypesFileName = props.readFileOrDirectoryPath("AgentTypesDistributionFileName");
        Path groupTypesFileName = props.readFileOrDirectoryPath("GroupTypesDistributionFileName");
        Path ipfpResultsHome = props.readFileOrDirectoryPath("IPFPResultHome");
        String[] sa2Names = props.readCommaSepProperties("SA2Names");
        int templatesProcessorThreadPoolSize = Integer.parseInt(props.getProperty("TemplatesProcessorThreadPoolSize"));
        int maxTemplatesInAThread = Integer.parseInt(props.getProperty("MaxTemplatesInAProcessorThread"));

        Map<GroupType, List<GroupTemplate>> groupTemplates = null;
        try {
            groupTemplates = (Map<GroupType, List<GroupTemplate>>) Reader.readObjectLZMA(combinedCleanedTemplates);
        } catch (ClassNotFoundException | IOException e2) {
            e2.printStackTrace();
        }

        groupTemplates = TemplateUtil.readGroupTemplatesFromFiles(props);

        long startTime = 0, elaspedTime = 0;
        CSVWriter csvWriter = new CSVWriter();
        for (String sa2 : sa2Names) {
            Path agentTypesFile = ipfpResultsHome.resolve(sa2 + File.separator + agentTypesFileName);
            Path groupTypesFile = ipfpResultsHome.resolve(sa2 + File.separator + groupTypesFileName);
            // Path seedFile = ipfpResultsHome.resolve(sa2 + File.separator + seedFileName);
            // IPFPTable seed = new IPFPTableBuilder(seedFile).build();

            Map<ReferenceAgentType, Integer> targetAgentTypeCounts = null;
            Map<GroupType, Integer> targetGroupTypeCounts = null;
            try {
                targetAgentTypeCounts = LatchUtils.readAgentTypesDistribution(agentTypesFile);
                targetGroupTypeCounts = LatchUtils.readHhsDistribution(groupTypesFile);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            startTime = System.currentTimeMillis();
            Log.info("Constructing Population");
            int populationSize = (int) Math.round(targetAgentTypeCounts.values().stream().mapToDouble(v -> v).sum());
            NonIPFPPopulationBuilder nonIpfpPopulationBuilder = new NonIPFPPopulationBuilder(targetGroupTypeCounts, targetAgentTypeCounts,
                    groupTemplates, populationSize) {

                @Override
                protected double calculateError(GroupTemplate groupTemplate) {
                    // Using Root Mean Squared Error
                    double squaredSum = 0;
                    for (AgentType agentType : targetAgentTypeCounts.keySet()) {
                        int numberOfAgentsOfCurrentTypeInGroup = 0;
                        for (Member m : groupTemplate.getAllMembers()) {
                            if (m.isSameType(agentType)) {
                                numberOfAgentsOfCurrentTypeInGroup++;
                            }
                        }
                        if (numberOfAgentsOfCurrentTypeInGroup > 0) {
                            // (y -Y)^2
                            squaredSum += Math.pow(targetGroupTypeCounts.get(agentType)
                                    - ((population.getAgentsSummary().get(agentType) + numberOfAgentsOfCurrentTypeInGroup)), 2);
                        } else {
                            squaredSum += Math.pow(targetGroupTypeCounts.get(agentType) - (population.getAgentsSummary().get(agentType)), 2);
                        }
                    }
                    return Math.sqrt(squaredSum / (double) population.cellCount());
                }

                @Override
                protected double calculateError(GroupType groupType) {
                    double squaredSum = 0;
                    if (targetGroupTypeCounts.get(groupType) != 0) {
                        for (GroupType gType : targetGroupTypeCounts.keySet()) {
                            if (gType != groupType) {
                                squaredSum += Math.pow(targetGroupTypeCounts.get(gType) - (population.getGroupsSummary().get(gType)), 2);
                            } else {
                                squaredSum += Math.pow(targetGroupTypeCounts.get(gType) - (population.getGroupsSummary().get(gType) + 1), 2);
                            }
                        }
                    } else {
                        squaredSum = Double.POSITIVE_INFINITY;
                    }
                    return Math.sqrt(squaredSum / targetGroupTypeCounts.size());
                }

            };
            Population pop = null;
            try {
                pop = nonIpfpPopulationBuilder.build(templatesProcessorThreadPoolSize, maxTemplatesInAThread);
            } catch (InterruptedException | ExecutionException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            elaspedTime = System.currentTimeMillis() - startTime;
            Log.info("Population construction complete: Elasped time = " + (elaspedTime / 1000) / 60 + "m " + (elaspedTime / 1000) % 60 + "s");

            pop.setValueMode(ValueMode.AGENTCOUNT);
            Path outputFile = outputHome.resolve("AlgoV2.1" + File.separator + sa2 + File.separator + populationSummaryFile);// Population as 2D

            Path groupsOutputFile = outputHome.resolve("AlgoV2.1" + File.separator + sa2 + File.separator + groupSummaryOutput);// Groups summary
            Path agentsOutputFile = outputHome.resolve("AlgoV2.1" + File.separator + sa2 + File.separator + agentSummaryOutput);
            try {
                Files.createDirectories(outputFile.getParent());
                csvWriter.writeAsCsv(Files.newBufferedWriter(outputFile), pop);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try (CSVPrinter p = new CSVPrinter(Files.newBufferedWriter(groupsOutputFile), CSVFormat.DEFAULT)) {
                List<List<Integer>> groups = new ArrayList<>();
                for (Entry<GroupType, Integer> group : pop.getGroupsSummary().entrySet()) {
                    groups.add(Arrays.asList(group.getKey().getID(), group.getValue()));
                }
                p.printRecords(groups);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try (CSVPrinter p = new CSVPrinter(Files.newBufferedWriter(agentsOutputFile), CSVFormat.DEFAULT)) {
                List<List<Integer>> agents = new ArrayList<>();
                for (Entry<ReferenceAgentType, Integer> agent : pop.getAgentsSummary().entrySet()) {
                    agents.add(new ArrayList<>(Arrays.asList(agent.getKey().getTypeID(), agent.getValue())));
                }
                p.printRecords(agents);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.info("THE END!");

    }

}