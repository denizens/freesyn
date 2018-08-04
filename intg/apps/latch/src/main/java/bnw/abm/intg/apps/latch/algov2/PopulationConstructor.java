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
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.framework.models.IPFPTable;
import bnw.abm.intg.algov2.framework.models.Population;
import bnw.abm.intg.algov2.framework.models.Population.MatrixCell.ValueMode;
import bnw.abm.intg.algov2.population.PopulationBuilder;
import bnw.abm.intg.algov2.templates.IPFPTableBuilder;
import bnw.abm.intg.filemanager.csv.CSVWriter;
import bnw.abm.intg.filemanager.obj.Reader;
import bnw.abm.intg.util.BNWProperties;
import bnw.abm.intg.util.Log;
import ch.qos.logback.classic.Level;

public class PopulationConstructor {

    public static void construct(String propertiesFile) {

        DateFormat dateFormat = new SimpleDateFormat("dd:MM:yy-HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        String logSuffix = dateFormat.format(cal.getTime());
        Log.createLogger("LatchAlgoV2Population", Level.INFO, "latch.algov2.population." + logSuffix + ".log");

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

        Path ipfpResultsHome = props.readFileOrDirectoryPath("IPFPResultHome");
        Path ipfpProportionsFileName = props.readFileOrDirectoryPath("IPFPproportionsFileName");
        Path ipfpAgentCountsFileName = props.readFileOrDirectoryPath("IPFPAgentCountsFileName");
        String[] sa2Names = props.readCommaSepProperties("SA2Names");
        int templatesProcessorThreadPoolSize = Integer.parseInt(props.getProperty("TemplatesProcessorThreadPoolSize"));
        int maxTemplatesInAThread = Integer.parseInt(props.getProperty("MaxTemplatesInAProcessorThread"));

        Map<GroupType, List<GroupTemplate>> groupTemplates = null;
        try {
            groupTemplates = (Map<GroupType, List<GroupTemplate>>) Reader.readObjectLZMA(combinedCleanedTemplates);
        } catch (ClassNotFoundException | IOException e2) {
            e2.printStackTrace();
        }

        long startTime = 0, elaspedTime = 0;
        CSVWriter csvWriter = new CSVWriter();
        for (String sa2 : sa2Names) {
            Path ipfpProportionsFile = ipfpResultsHome.resolve(sa2 + File.separator + ipfpProportionsFileName);
            Path ipfpAgentCountFile = ipfpResultsHome.resolve(sa2 + File.separator + ipfpAgentCountsFileName);
            Log.info("Reading in IPFP table of " + sa2);

            IPFPTable ipfpTable = new IPFPTableBuilder(ipfpProportionsFile).build();
            IPFPTable ipfpAgentCounts = new IPFPTableBuilder(ipfpAgentCountFile).build();
            int populationSize = (int) Math.round(ipfpAgentCounts.values().stream().mapToDouble(v -> v).sum());

            startTime = System.currentTimeMillis();
            Log.info("Constructing Population");
            PopulationBuilder pb = new PopulationByRMSE(ipfpTable, groupTemplates.values().stream().flatMap(List::stream)
                    .collect(Collectors.toList()), populationSize);
            Population pop = null;
            try {
                pop = pb.build(templatesProcessorThreadPoolSize, maxTemplatesInAThread);
            } catch (InterruptedException | ExecutionException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            elaspedTime = System.currentTimeMillis() - startTime;
            Log.info("Population construction complete: Elasped time = " + (elaspedTime / 1000) / 60 + "m " + (elaspedTime / 1000) % 60 + "s");

            pop.setValueMode(ValueMode.AGENTCOUNT);
            Path outputFile = outputHome.resolve("AlgoV2" + File.separator + sa2 + File.separator + populationSummaryFile);// Population as 2D

            Path groupsOutputFile = outputHome.resolve("AlgoV2" + File.separator + sa2 + File.separator + groupSummaryOutput);// Groups summary
            Path agentsOutputFile = outputHome.resolve("AlgoV2" + File.separator + sa2 + File.separator + agentSummaryOutput);
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
                    agents.add(Arrays.asList(agent.getKey().getTypeID(), agent.getValue()));
                }
                p.printRecords(agents);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.info("THE END!");
    }
}
