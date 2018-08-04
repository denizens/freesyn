package bnw.abm.intg.apps.latch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.AgentType.TargetAgentType;
import bnw.abm.intg.algov2.framework.models.GroupType;

public class LatchUtils {
    public static Map<GroupType, Integer> readHhsDistribution(Path householdsFile) throws IOException {
        Map<GroupType, Integer> groupTypeCounts = new LinkedHashMap<>(112);
        try (CSVParser csvParser = new CSVParser(Files.newBufferedReader(householdsFile), CSVFormat.DEFAULT)) {
            int i = -2;
            for (CSVRecord csvRec : csvParser) {
                i++;
                if (i == -1) {
                    continue;
                }
                groupTypeCounts.put(GroupType.getInstance(i), Integer.parseInt(csvRec.get(4)));
            }
        }
        return groupTypeCounts;
    }

    public static Map<ReferenceAgentType, Integer> readAgentTypesDistribution(Path agentsFile) throws IOException {
        Map<ReferenceAgentType, Integer> agentTypeCounts = new LinkedHashMap<>(112);
        try (CSVParser csvParser = new CSVParser(Files.newBufferedReader(agentsFile), CSVFormat.DEFAULT)) {
            int i = -2;
            for (CSVRecord csvRec : csvParser) {
                i++;
                if (i == -1) {
                    continue;
                }
                agentTypeCounts.put(ReferenceAgentType.getInstance(i), Integer.parseInt(csvRec.get(5)));
            }
        }
        return agentTypeCounts;
    }
    

}
