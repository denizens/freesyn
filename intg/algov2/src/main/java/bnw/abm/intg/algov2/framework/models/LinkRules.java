package bnw.abm.intg.algov2.framework.models;

import java.io.Serializable;
import java.util.*;

import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.AgentType.TargetAgentType;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

/**
 * Stores LinkRules of the system in a Guava HashBasedTable
 * 
 * @author Bhagya N. Wickramasinghe
 *
 */
public class LinkRules extends ForwardingTable<ReferenceAgentType, LinkType, List<TargetAgentType>> implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 9150573818143157342L;
    private Table<ReferenceAgentType, LinkType, List<TargetAgentType>> delegate;
    private LinkRulesRelative relativeLocations = new LinkRulesRelative();

    final private LinkedHashSet<AgentType> agentTypes = new LinkedHashSet<>();

    public LinkRules(int size) {
        delegate = Tables.newCustomTable(new LinkedHashMap<>(size, 1), LinkedHashMap::new);
    }

    public LinkRules() {
        delegate = Tables.newCustomTable(new LinkedHashMap<>(), LinkedHashMap::new);
    }

    @Override
    protected Table<ReferenceAgentType, LinkType, List<TargetAgentType>> delegate() {
        return delegate;
    }

    public String toCSVString() {
        StringBuilder sb = new StringBuilder();
        for (Cell<ReferenceAgentType, LinkType, List<TargetAgentType>> cell : delegate.cellSet()) {
            sb.append("\"[" + cell.getRowKey().toString() + "]\",\"." + cell.getColumnKey().toString() + ".\",\"" + cell.getValue() + "\"\n");
        }
        return sb.toString();
    }

    public String toFormattedString() {
        StringBuilder sb = new StringBuilder(), targets = new StringBuilder();
        for (Cell<ReferenceAgentType, LinkType, List<TargetAgentType>> cell : delegate.cellSet()) {
            LinkType link = cell.getColumnKey();
            cell.getValue().stream().forEach(v -> targets.append("[" + v.getTypeID() + "]"));
            if (targets.length() == 0)
                targets.append("[]");
            sb.append("[" + cell.getRowKey().toString() + "]." + link.toString() + "." + targets + "\n");
            targets.delete(0, targets.length());
        }
        return sb.toString();
    }

    /**
     * Gets the list of target agents in the specified rule
     * 
     * @param referenceAgentType
     *            ReferenceAgentType instance of the rule
     * @param activeLinkType
     *            LinkType of the rule
     * @return List of TargetAgentType instances specified by the rule
     */
    List<TargetAgentType> getTargets(AgentType referenceAgentType, LinkType activeLinkType) {
        return delegate().get(referenceAgentType, activeLinkType);
    }

    /**
     * Method to accept AgentType instead of Original method that accepts ReferenceAgentType. This method converts AgentType to its
     * ReferenceAgentType instance internally
     * 
     * @param rowKey
     *            Table's row key AgentType
     * @return Map of LinkType (as keys) and List of TargetAgentTypes (as values) that can be formed by AgentType specified as row key
     */
    public Map<LinkType, List<TargetAgentType>> row(AgentType rowKey) {
        ReferenceAgentType refInstance = ReferenceAgentType.getInstance(rowKey.getTypeID());
        if (refInstance == null) {
            return new HashMap<>(0);
        } else {
            return delegate().row(refInstance);
        }
    }

    /**
     * Note this method is overridden to return an empty map if rowKey is null. {@inheritDoc}
     */
    @Override
    public Map<LinkType, List<TargetAgentType>> row(ReferenceAgentType rowKey) {
        if (rowKey == null) {
            return new HashMap<>(0);
        } else {
            return super.row(rowKey);
        }
    }

    /**
     * Converts rowKey to ReferenceAgentType internally before performing get() method. columnKey is not changed. WARNING: Map stores
     * ActiveLinkType and PassiveLinkType instances. Passing different decorations of LinkType instances may not give the anticipated results.
     * {@inheritDoc}
     * 
     * @return List of TargetAgentTypes mapped by rowKey and columnKey
     */
    @Override
    public List<TargetAgentType> get(Object rowKey, Object columnKey) {
        if (!(rowKey instanceof ReferenceAgentType)) {
            rowKey = ReferenceAgentType.getInstance(((AgentType) rowKey).getTypeID());
        }
        return delegate().get(rowKey, columnKey);
    }

    @Override
    public boolean contains(Object rowKey, Object columnKey) {
        if (!(rowKey instanceof ReferenceAgentType)) {
            rowKey = ReferenceAgentType.getInstance(((AgentType) rowKey).getTypeID());
        }
        return delegate().contains(rowKey, columnKey);
    }

    /**
     * columKey type checked put function {@inheritDoc}
     * 
     */
    @Override
    public List<TargetAgentType> put(ReferenceAgentType rowKey, LinkType columnKey, List<TargetAgentType> value) {
        relativeLocations.put(rowKey, columnKey, value);
        return super.put(rowKey, columnKey, value);
    }

    public LinkedHashSet<AgentType> getAllAgentTypes() {
        return agentTypes;
    }

    public LinkRulesRelative getRelativeLocationsTable() {
        return relativeLocations;
    }

    final public static class LinkRulesWrapper {
        private static LinkRules linkRules, linkRulesOriginal;

        public static void setLinkRules(LinkRules linkRules) {
            LinkRulesWrapper.linkRules = linkRules;
        }

        public static List<TargetAgentType> get(Object rowKey, Object columnKey) {
            return new ArrayList<>(linkRules.get(rowKey, columnKey));
        }

        public static Map<LinkType, List<TargetAgentType>> row(AgentType rowKey) {
            return  new LinkedHashMap<>(linkRules.row(rowKey));
        }

        public static boolean contains(AgentType rowKey, LinkType columnKey) {
            return linkRules.contains(rowKey, columnKey);
        }

        public static LinkRules getLinkRules() {
            return linkRules;
        }

        public static Set<ReferenceAgentType> rowKeySet() {
            return new LinkedHashSet<>(linkRules.rowKeySet());
        }

    }
}
