package bnw.abm.intg.latch.testcase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.AgentType.TargetAgentType;
import bnw.abm.intg.algov2.templates.LinkRulesBuilder;
import bnw.abm.intg.util.GlobalConstants.EXITCODE;
import bnw.abm.intg.util.Log;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

public class CsvLinkRulesBuilder extends LinkRulesBuilder {
    private final Path linkRulesCsv;

    public CsvLinkRulesBuilder(Path linkRulesCsv) {
        this.linkRulesCsv = linkRulesCsv;
    }

    @Override
    public void build() {
        readLinkRules(linkRulesCsv);
    }

    /**
     * Reads in link rules from a csv file
     * 
     * @param linkRulesFile
     *            Path object to csv file with link rules
     * @return LinkRules of this population
     * @throws IOException
     *             Failing to read the csv file
     */
    private void readLinkRules(Path linkRulesFile) {
        try (CSVParser csvParser = new CSVParser(Files.newBufferedReader(linkRulesFile), CSVFormat.EXCEL.withHeader())) {

            for (CSVRecord record : csvParser) {
                ReferenceAgentType referenceAgentType = createReferenceAgentType(record.get("ReferenceAgentType"));

                // Read in link type's name
                String linkName = record.get("LinkType");
                if (linkName == null || linkName.equals("")) {
                    Log.warn("Empty Link Type detected: Not adding " + record.toMap() + " to LinkRules");
                    continue;
                }
                // Read max and min number of links
                Integer min = Ints.tryParse(record.get("Min"));
                if (min == null) {
                    Log.errorAndExit("Min value is not an integer in " + record.toMap(), EXITCODE.USERINPUT);
                }

                Integer max = Ints.tryParse(record.get("Max"));
                if (max == null) {
                    Log.errorAndExit("Max value is not an integer in " + record.toMap(), EXITCODE.USERINPUT);
                }

                // Active link type
                boolean isActive = false;
                if (record.get("Active").equals("TRUE")) {
                    isActive = true;
                } else if (record.get("Active").equals("FALSE")) {
                    isActive = false;
                } else {
                    Log.errorAndExit("Value for Active must be TRUE/FALSE: " + record, new UnrecognizedOptionException(
                            "Value for Active field must be TRUE/FALSE"), EXITCODE.USERINPUT);
                }

                LinkType linkType = LinkType.getInstance(linkName);
                linkType.setMinLinks(min);
                linkType.setMaxLinks(max);
                linkType.setActive(isActive);
                // Read in target agent types
                List<TargetAgentType> targetAgentTypes = getTargetAgentTypesList(record.get("TargetAgentTypes"));

                // Add a new link rule
                addRule(referenceAgentType, linkType, targetAgentTypes);
            }

        } catch (Exception ioe) {
            Log.errorAndExit("Reading link rules csv file failed", ioe, EXITCODE.IOERROR);
        }
    }

    /**
     * This function reads in target agent types in link rules input
     * 
     * @param agentTypesStr
     *            Csv input string
     * @return list of AgentTypes
     */
    private List<TargetAgentType> getTargetAgentTypesList(String agentTypesStr) {
        List<TargetAgentType> targetAgentTypes = new ArrayList<>();
        Matcher m = Pattern.compile("\\[([^]]+)\\]").matcher(agentTypesStr);
        while (m.find()) {
            String[] props = m.group(1).split(",");// Split AgentTypeID and weight
            Double weight = Doubles.tryParse(props[1]);
            if (weight == null) {
                Log.errorAndExit("Non-numeric value given to weight in " + agentTypesStr, EXITCODE.USERINPUT);
            }
            targetAgentTypes.add(super.createTargetAgentTypeInstance(Short.parseShort(props[0]), Double.parseDouble(props[1])));
        }

        return targetAgentTypes;
    }

    /**
     * Read in the reference agent types from link rules input
     * 
     * @param agentTypesStr
     *            Csv input string
     * @return list of AgentTypes
     */
    private ReferenceAgentType createReferenceAgentType(String agentTypesStr) {
        agentTypesStr = agentTypesStr.replace("[", "").replace("]", "");
        return super.createReferenceAgentType(Short.parseShort(agentTypesStr));

    }
}