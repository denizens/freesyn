package bnw.abm.intg.apps.wdll;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import bnw.abm.intg.algov2.templates.GroupTemplatesBuilder;
import bnw.abm.intg.apps.wdll.A4yrRmp.WDLLDependentLinks;
import bnw.abm.intg.apps.wdll.A4yrRmp.WDLLInverseLinks;
import bnw.abm.intg.filemanager.csv.CSVWriter;
import bnw.abm.intg.util.BNWProperties;
import bnw.abm.intg.util.Log;
import ch.qos.logback.classic.Level;

public class WDLL_LatchAgeMarritalRel {

    public static void run(String propertiesFile, String logSuffix) {
        DateFormat dateFormat = new SimpleDateFormat("dd:MM:yy-HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        logSuffix = logSuffix + dateFormat.format(cal.getTime());
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
        Path populationOutput = Paths.get("PopulationOutput_" + logSuffix + ".csv");
        Path groupSummaryOutput = props.readFileOrDirectoryPath("GroupSummaryOutput");
        Path agentSummaryOutput = props.readFileOrDirectoryPath("AgentSummaryOutput");
        Path allTemplatesOutput = props.readFileOrDirectoryPath("TemplatesFile");

        Log.info("Registering domain constraints");
        new WDLLInverseLinks(63).register();
        new WDLLDependentLinks(63).register();
        new WDLL_LatchAgeMaritalRelRules().register();

        long startTime = System.currentTimeMillis();
        Log.info("Constructing group templates");
        GroupTemplatesBuilder templateBuider = null;
        int numOfTemplates = 0;
        try {
            templateBuider = new GroupTemplatesBuilder( new SizeBasedGroupType(), maxGroupSize,
                    allTemplatesOutput);
            numOfTemplates = templateBuider.build(0, 12);
            Log.info("Total number of Group Templates: " + numOfTemplates);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        long elaspedTime = System.currentTimeMillis() - startTime;
        Log.info("GroupTemplates construction complete: Elasped time = " + (elaspedTime / 1000) / 60 + "m " + (elaspedTime / 1000) % 60 + "s");

        // Log.info("Reading in IPFP table");
        // IPFPTableBuilder ipfpTableBuilder = new IPFPTableBuilder(ipfpProportionsFile);
        // IPFPTable ipfpTable = ipfpTableBuilder.build();
        //
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
