package bnw.abm.intg.apps.latch;

import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.util.Log;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.zip.GZIPInputStream;

/**
 * @author wniroshan 15 May 2018
 */
public class DataReader {

    public static LinkedHashMap<GroupType, Integer> readGroupTypes(Path groupTypesFile) throws IOException {
        LinkedHashMap<GroupType, Integer> groupsDist;
        try (CSVParser csvParser = new CSVParser(Files.newBufferedReader(groupTypesFile), CSVFormat.EXCEL.withHeader())) {
            csvParser.getHeaderMap().keySet();
            groupsDist = loadGroupRecords(csvParser);
        }
        return groupsDist;
    }

    public static LinkedHashMap<GroupType, Integer> readGroupTypesGz(Path groupTypesFile) throws IOException {
        LinkedHashMap<GroupType, Integer> groupsDist;
        try (CSVParser csvParser = new CSVParser(new InputStreamReader(new GZIPInputStream(new BufferedInputStream(Files.newInputStream(
                groupTypesFile)))), CSVFormat.EXCEL.withHeader())) {
            csvParser.getHeaderMap().keySet();
            groupsDist = loadGroupRecords(csvParser);
        }
        return groupsDist;
    }

    public static LinkedHashMap<ReferenceAgentType, Integer> readAgentTypes(Path agentTypesFile) throws IOException {
        LinkedHashMap<ReferenceAgentType, Integer> agentsDist;
        try (CSVParser csvParser = new CSVParser(Files.newBufferedReader(agentTypesFile), CSVFormat.EXCEL.withHeader())) {
            agentsDist = loadAgentRecords(csvParser);
        }
        return agentsDist;
    }

    public static LinkedHashMap<ReferenceAgentType, Integer> readAgentTypesGz(Path agentTypesFile) throws IOException {
        LinkedHashMap<ReferenceAgentType, Integer> agentsDist;
        try (CSVParser csvParser = new CSVParser(new InputStreamReader(new GZIPInputStream(new BufferedInputStream(Files.newInputStream(
                agentTypesFile)))), CSVFormat.EXCEL.withHeader())) {
            agentsDist = loadAgentRecords(csvParser);
        }
        return agentsDist;
    }

    private static LinkedHashMap<ReferenceAgentType, Integer> loadAgentRecords(CSVParser csvParser) {
        LinkedHashMap<ReferenceAgentType, Integer> agentsDist = new LinkedHashMap<>();
        int row = 0;
        for (CSVRecord csvRec : csvParser) {
            int personsCount = Integer.parseInt(csvRec.get("Persons count"));
            ReferenceAgentType aType = ReferenceAgentType.getExisting(row);
            if (aType == null) {
                Log.warn("New Agent Type created: " + row);
                aType = ReferenceAgentType.getInstance(row);
            }
            row++;
            agentsDist.put(aType, personsCount);
        }
        return agentsDist;
    }

    private static LinkedHashMap<GroupType, Integer> loadGroupRecords(CSVParser csvParser) {
        LinkedHashMap<GroupType, Integer> groupsDist = new LinkedHashMap<>();
        int row = 0;
        for (CSVRecord csvRec : csvParser) {
            int groupsCount = Integer.parseInt(csvRec.get("Households count"));
            GroupType gType = GroupType.getExisting(row);
            if (gType == null) {
                Log.warn("New Group Type created: " + row);
                gType = GroupType.getInstance(row);
            }
            row++;
            groupsDist.put(gType, groupsCount);
        }
        return groupsDist;
    }

}