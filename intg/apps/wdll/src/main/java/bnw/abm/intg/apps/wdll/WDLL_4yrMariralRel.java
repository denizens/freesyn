package bnw.abm.intg.apps.wdll;

import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import bnw.abm.intg.algov2.framework.models.AgentType.TargetAgentType;
import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.templates.GroupTemplatesBuilder;
import bnw.abm.intg.algov2.templates.LinkRulesBuilder;
import bnw.abm.intg.apps.wdll.A4yrRmp.WDLLDependentLinks;
import bnw.abm.intg.apps.wdll.A4yrRmp.WDLLInverseLinks;
import bnw.abm.intg.filemanager.csv.CSVWriter;
import bnw.abm.intg.util.BNWProperties;
import bnw.abm.intg.util.Log;
import ch.qos.logback.classic.Level;

public class WDLL_4yrMariralRel {
    public static void  run(String propertiesFile) {
        DateFormat dateFormat = new SimpleDateFormat("dd:MM:yy-HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        String logSuffix = dateFormat.format(cal.getTime());
        Log.createLogger("Init", Level.INFO, "bnw.abm.intg.init." + logSuffix + ".log");
        Log.info("Reading program properties");
        CSVWriter csvWriter = new CSVWriter();
        BNWProperties props = null;
        try {
            props = new BNWProperties(propertiesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int maxGroupSize = Integer.parseInt(props.getProperty("MaxGroupSize"));
        int populationSize = Integer.parseInt(props.getProperty("PopulationSize"));
        int numberOfThreads = Integer.parseInt(props.getProperty("NumberOfBuilderThreads"));
        Path ipfpProportionsFile = props.readFileOrDirectoryPath("IPFPproportions");
        Path populationOutput = props.readFileOrDirectoryPath("PopulationOutput");
        Path groupSummaryOutput = props.readFileOrDirectoryPath("GroupSummaryOutput");
        Path agentSummaryOutput = props.readFileOrDirectoryPath("AgentSummaryOutput");
        Path templatesOutput = props.readFileOrDirectoryPath("TemplatesFile");

        Log.info("Registering domain constraints");
        new WDLLInverseLinks(52).register();
        new WDLLDependentLinks(52).register();

        new WDLLCoarseRules().register();
        long startTime = System.currentTimeMillis();
        Log.info("Constructing group templates");
        GroupTemplatesBuilder templateBuider = null;
        int numberOfTemplates = 0;
        try {
            templateBuider = new GroupTemplatesBuilder(new SizeBasedGroupType(), maxGroupSize, templatesOutput);
            numberOfTemplates = templateBuider.build(0, 12);
            Log.info("Total number of Group Templates: " + numberOfTemplates);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        long elaspedTime = System.currentTimeMillis() - startTime;
        Log.info("GroupTemplates construction complete: Elasped time = " + (elaspedTime / 1000) / 60 + "m " + (elaspedTime / 1000) % 60 + "s");

        // Log.info("Reading in IPFP table");
        // IPFPTableBuilder ipfpTableBuilder = new IPFPTableBuilder(ipfpProportionsFile);
        // IPFPTable ipfpTable = ipfpTableBuilder.build();

        // startTime = System.currentTimeMillis();
        // Log.info("Constructing Population");
        // PopulationBuilder pb = new PopulationByRMSE(ipfpTable, groupTemplates, populationSize);
        // Population pop = pb.build();
        // elaspedTime = System.currentTimeMillis() - startTime;
        // Log.info("Population construction complete: Elasped time = " + (elaspedTime / 1000) / 60 + "m " + (elaspedTime / 1000) % 60 + "s");
        //
        // pop.setValueMode(ValueMode.AGENTCOUNT);
        // try {
        // csvWriter.writeAsCsv(Files.newBufferedWriter(populationOutput), pop);
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        Log.info("THE END!");
    }

}

class WDLLCoarseRules extends LinkRulesBuilder {

    @Override
    public void build() {
        ArrayList<TargetAgentType> targets;
        for (int i = 0; i < 104; i++) {
            if (30 <= i & i <= 51) {
                addRule(createReferenceAgentType(i), LinkType.getInstance("Married", 1, 1, true), getTargets(82, 104));
            }
            if (82 <= i & i <= 103) {
                addRule(createReferenceAgentType(i), LinkType.getInstance("Married", 1, 1, true), getTargets(30, 52));
            }
            if ((4 <= i & i <= 25) | (30 <= i & i <= 51) | (56 <= i & i <= 77) | (82 <= i & i <= 103)) {
                targets = new ArrayList<>();
                targets.addAll(getTargets(0, (i % 26) - 3));
                targets.addAll(getTargets(26, (i % 26) + 26 - 3));
                targets.addAll(getTargets(52, (i % 26) + (2 * 26) - 3));
                targets.addAll(getTargets(78, (i % 26) + (3 * 26) - 3));
                targets.trimToSize();
                addRule(createReferenceAgentType(i), LinkType.getInstance("ParentOf", 1, 4, true), targets);
            }
            targets = new ArrayList<>();
            targets.addAll(getTargets((i % 26) + 4, 26));
            targets.addAll(getTargets((i % 26) + 4 + 26, 52));
            targets.trimToSize();
            addRule(createReferenceAgentType(i), LinkType.getInstance("ChildOfFather", 1, 1, false), targets);

            targets = new ArrayList<>();
            targets.addAll(getTargets((i % 26) + 4 + 52, 78));
            targets.addAll(getTargets((i % 26) + 4 + 52 + 26, 104));
            targets.trimToSize();
            addRule(createReferenceAgentType(i), LinkType.getInstance("ChildOfMother", 1, 1, false), targets);
        }

    }

    List<TargetAgentType> getTargets(int fromIDInclusive, int toIDExclusive) {
        List<TargetAgentType> tars = new ArrayList<>((toIDExclusive < fromIDInclusive) ? 0 : toIDExclusive - fromIDInclusive);
        for (int i = fromIDInclusive; i < toIDExclusive; i++) {
            tars.add(createTargetAgentTypeInstance(i, 1));
        }
        return tars;
    }

}
