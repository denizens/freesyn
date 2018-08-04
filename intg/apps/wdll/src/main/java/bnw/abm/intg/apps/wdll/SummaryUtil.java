package bnw.abm.intg.apps.wdll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import bnw.abm.intg.algov2.framework.models.Agent;
import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.Group;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.framework.models.Link;
import bnw.abm.intg.algov2.framework.models.LinkRules.LinkRulesWrapper;
import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.framework.models.Population;
import bnw.abm.intg.filemanager.csv.CSVWriter;
import bnw.abm.intg.util.Log;

public class SummaryUtil {
    
    public static void printFullSummary(Population population, Path fullSummaryOutputFile){
        try {
            new CSVWriter().writeAsCsv(Files.newBufferedWriter(fullSummaryOutputFile), population);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void printGroupsSummary(Population population, Path groupsSummaryOutputFile) {
        try (CSVPrinter p = new CSVPrinter(Files.newBufferedWriter(groupsSummaryOutputFile), CSVFormat.DEFAULT)) {
            List<List<Integer>> groups = new ArrayList<>();
            for (Entry<GroupType, Integer> group : population.getGroupsSummary().entrySet()) {
                Log.info(group.getKey().getID() + " " + group.getValue());
                groups.add(Arrays.asList(group.getKey().getID(), group.getValue()));
            }
            p.printRecords(groups);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.info("Groups summary file written to: " + groupsSummaryOutputFile);
    }

    public static void printAgentsSummary(Population population, Path agentsSummaryOutputFile) {
        try (CSVPrinter p = new CSVPrinter(Files.newBufferedWriter(agentsSummaryOutputFile), CSVFormat.DEFAULT)) {
            List<List<Integer>> agents = new ArrayList<>();
            for (Entry<ReferenceAgentType, Integer> agent : population.getAgentsSummary().entrySet()) {
                agents.add(new ArrayList<>(Arrays.asList(agent.getKey().getTypeID(), agent.getValue())));
            }
            p.printRecords(agents);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.info("Agents summary file written to: " + agentsSummaryOutputFile);
    }

    public static void printAgentRecords(Population population, Path agentsFile) throws IOException {
        List<LinkedHashMap<String, Object>> agents = new ArrayList<>();
        LinkedHashMap<String, Object> agentTemplate = new LinkedHashMap<>();
        agentTemplate.put("AgentId", null);
        agentTemplate.put("Type", null);
        agentTemplate.put("GroupType", null);
        for (LinkType ltype : LinkRulesWrapper.getLinkRules().columnKeySet()) {
            agentTemplate.put(ltype.toString(), null);
        }

        for (Group group : population.getGroups()) {
            for (Agent a : group.getMembers()) {
                LinkedHashMap<String, Object> agent = new LinkedHashMap<>(agentTemplate);
                agent.put("AgentId", Integer.toString(a.getID()));
                agent.put("Type", Integer.toString(a.getType()));
                agent.put("GroupType", group.type());
                for (Entry<Link, List<Agent>> e : group.getStructure().row(a).entrySet()) {
                    agent.put(e.getKey().toString(), e.getValue().toString());
                }
                agents.add(agent);
            }
        }

        CSVWriter csvw = new CSVWriter();
        csvw.writeLinkedMapAsCsv(Files.newBufferedWriter(agentsFile), agents);
        Log.info("Agents written to: " + agentsFile);
    }
}
