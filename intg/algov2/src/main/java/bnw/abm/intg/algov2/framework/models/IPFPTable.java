package bnw.abm.intg.algov2.framework.models;

import java.util.LinkedHashMap;
import java.util.Set;

import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

public class IPFPTable extends ForwardingTable<GroupType, ReferenceAgentType, Double> {

    private final Table<GroupType, ReferenceAgentType, Double> delegate;

    public IPFPTable(Set<GroupType> rowKeys, Set<ReferenceAgentType> columnKeys) {
        this.delegate = Tables.newCustomTable(new LinkedHashMap<>(), LinkedHashMap::new);
        for (GroupType rowKey : rowKeys) {
            for (ReferenceAgentType columnKey : columnKeys) {
                this.delegate.put(rowKey, columnKey, 0.0);
            }
        }
    }

    public IPFPTable(IPFPTable targetCounts) {
        this.delegate = Tables.newCustomTable(targetCounts.delegate().rowMap(), LinkedHashMap::new);
    }

    public IPFPTable() {
        this.delegate = Tables.newCustomTable(new LinkedHashMap<>(), LinkedHashMap::new);
    }

    @Override
    protected Table<GroupType, ReferenceAgentType, Double> delegate() {
        return delegate;

    }

    @Override
    public Double get(Object rowKey, Object columnKey) {
        if (columnKey instanceof AgentType && !(columnKey instanceof ReferenceAgentType)) {
            columnKey = ReferenceAgentType.getExisting(((AgentType) columnKey).getTypeID());
            if (rowKey instanceof GroupType) {
                return delegate.get(rowKey, columnKey);
            }
        } else {
            return delegate.get(rowKey, columnKey);
        }
        throw new UnsupportedOperationException("Works only with GroupType rowKey and AgentType columnKey");

    }

    public Double put(GroupType rowKey, AgentType columnKey, Double value) {
        ReferenceAgentType columnKeyR = ReferenceAgentType.getInstance(columnKey.getTypeID());
        return delegate.put(rowKey, columnKeyR, value);
    }

}
