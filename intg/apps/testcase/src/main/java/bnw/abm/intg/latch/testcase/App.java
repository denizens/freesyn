package bnw.abm.intg.latch.testcase;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.templates.GroupTemplatesBuilder;
import bnw.abm.intg.algov2.templates.GroupTypeLogic;
import bnw.abm.intg.latch.testcase.LinkConditionsBuilder.DependentLinks;
import bnw.abm.intg.latch.testcase.LinkConditionsBuilder.InverseLink;
import bnw.abm.intg.util.BNWProperties;
import bnw.abm.intg.util.Log;
import bnw.abm.intg.latch.testcase.CsvLinkRulesBuilder;
import ch.qos.logback.classic.Level;



public class App {
    public static void main(String[] args) {
        testCase(args[0]);
    }

    static void testCase(String propertiesFile) {
        Log.createLogger("Init", Level.INFO, "bnw.abm.intg.init.3.log");
        Log.info("Reading program properties");
        BNWProperties props = null;
        try {
            props = new BNWProperties(propertiesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int maxGroupSize = Integer.parseInt(props.getProperty("MaxGroupSize"));
        int populationSize = Integer.parseInt(props.getProperty("PopulationSize"));
        int numberOfThreads = Integer.parseInt(props.getProperty("NumberOfBuilderThreads"));
        Path linkRulesFile = props.readFileOrDirectoryPath("LinkRulesFile");
        Path ipfpProportionsFile = Paths.get("IPFPResult.csv");
        Path populationOutput = props.readFileOrDirectoryPath("PopulationOutput");
        Path groupSummaryOutput = props.readFileOrDirectoryPath("GroupSummaryOutput");
        Path agentSummaryOutput = props.readFileOrDirectoryPath("AgentSummaryOutput");
        Path allTemplatesOutput = props.readFileOrDirectoryPath("TemplatesFile");

        Log.info("Registering domain constraints");
        new InverseLink().register();
        new DependentLinks().register();

        Log.info("Constructing group templates");
        CsvLinkRulesBuilder csvLinkRulesBuilder = new CsvLinkRulesBuilder(linkRulesFile);
        csvLinkRulesBuilder.register();
        GroupTemplatesBuilder templateBuider = null;
        int numberOfTemplates = 0;
        try {
            templateBuider = new GroupTemplatesBuilder(new SizeBasedGroupType(), maxGroupSize, allTemplatesOutput);
            numberOfTemplates = templateBuider.build(0, 12);
            Log.info("Total number of Group Templates: " + numberOfTemplates);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        // Log.info("Reading in IPFP table");
        // IPFPTableBuilder ipfpTableBuilder = new IPFPTableBuilder(ipfpProportionsFile);
        // IPFPTable ipfpTable = ipfpTableBuilder.build();
        //
        // Log.info("Constructing Population");
        // PopulationBuilder pb = new PopulationByRMSE(ipfpTable, groupTemplates, populationSize);
        // Population pop = pb.build();
        //
        // CSVWriter csvWriter = new CSVWriter();
        // pop.setValueMode(ValueMode.AGENTCOUNT);
        // try {
        // csvWriter.writeAsCsv(Files.newBufferedWriter(populationOutput), pop);
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        Log.info("THE END!");
    }
}

class SizeBasedGroupType extends GroupTypeLogic {

    @Override
    public GroupType computeGroupType(GroupTemplate groupTemplate) {
        return GroupType.getInstance(groupTemplate.size());
    }
}