package bnw.abm.intg.algov2.framework.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class Group {

    private final Table<Agent, Link, List<Agent>> adjacencyList;
    private Map<Member, Agent> memberMap = new HashMap<>();
    private final GroupTemplate template;

    public Group(GroupTemplate template) {
        adjacencyList = HashBasedTable.create(template.size(), 4);
        this.template = template;
        for (Member m : template.getAllMembers()) {
            memberMap.put(m, new Agent(m));
        }

        for (Member ref : template.getAllMembers()) {
            Map<LinkType, List<Member>> targetMap = template.getAdjacentMembers(ref);
            for (LinkType lType : targetMap.keySet()) {
                List<Agent> targets = targetMap.get(lType).stream().map(t -> memberMap.get(t)).collect(Collectors.toList());
                Agent refAgent = memberMap.get(ref);
                Link link = new Link(lType);
                adjacencyList.put(refAgent, link, targets);
            }
        }
    }

    public int size() {
        return memberMap.size();
    }

    public String toString() {
        return this.template.toString();
    }

    public String toFullString() {
        return this.template.toStringFullMode();
    }

    public String json() throws JsonProcessingException {
        return this.template.json();
    }

    public GroupType type() {
        return template.getGroupType();
    }

    public List<Agent> getMembers() {
        return new ArrayList<>(memberMap.values());
    }

    public Table<Agent, Link, List<Agent>> getStructure() {
        return adjacencyList;
    }
}
