package bnw.abm.intg.algov2.framework.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.AgentType.TargetAgentType;
import bnw.abm.intg.util.GlobalConstants.EXITCODE;
import bnw.abm.intg.util.Log;

final public class LinkRulesRelative extends ForwardingTable<Integer, LinkType, List<Integer>> implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 6230390086929257906L;
    private Table<Integer, LinkType, List<Integer>> delegate = HashBasedTable.create();

    @Override
    protected Table<Integer, LinkType, List<Integer>> delegate() {
        return delegate;
    }

    public List<Integer> put(ReferenceAgentType rowKey, LinkType columnKey, List<TargetAgentType> value) {
        Comparator<TargetAgentType> byTypeId = (e1, e2) -> new Integer(e1.getTypeID()).compareTo(e2.getTypeID());
        Supplier<Stream<TargetAgentType>> valuesSupplier = () -> value.stream().sorted(byTypeId);
        List<Integer> relativeTargetLocations = new ArrayList<>(value.size());
        valuesSupplier.get().forEach(v -> relativeTargetLocations.add((v.getTypeID() - rowKey.getTypeID())));
        return super.put(rowKey.getTypeID(), columnKey, relativeTargetLocations);
    }

    @Override
    public List<Integer> put(Integer rowKey, LinkType columnKey, List<Integer> value) {
        throw new UnsupportedOperationException("Use put(ReferenceAgentType, LinkType, List<TargetAgentType>) method");
    }

    @Override
    public List<Integer> get(Object rowKey, Object columnKey) {
        int rKey = -1;
        LinkType cKey = null;

        if (rowKey instanceof AgentType) {
            rKey = ((AgentType) rowKey).getTypeID();
        } else {
            Log.errorAndExit("rowKey must be an instance of AgentType", new UnsupportedOperationException("Incompatible rowKey"),
                    EXITCODE.PROGERROR);
        }

        if (columnKey instanceof LinkType) {
            cKey = ((LinkType) columnKey);
        } else {
            Log.errorAndExit("columnKey must be an instance of LinkType", new UnsupportedOperationException("Incompatible columnKey"),
                    EXITCODE.PROGERROR);
        }

        return super.get(rKey, cKey);
    }
}
