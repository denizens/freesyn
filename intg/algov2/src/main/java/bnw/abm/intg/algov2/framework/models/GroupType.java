package bnw.abm.intg.algov2.framework.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import bnw.abm.intg.util.Log;

public class GroupType implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -5595432875372058271L;

    private transient static Map<Integer, GroupType> groupTypes = new HashMap<>(20);

    private final int typeId;

    private GroupType(int typeId) {
        this.typeId = typeId;
        groupTypes.put(typeId, this);
    }

    public static GroupType getInstance(int typeId) {
        if (groupTypes.containsKey(typeId)) {
            return groupTypes.get(typeId);
        } else {
            GroupType gt = new GroupType(typeId);
            return gt;
        }
    }

    public static GroupType getExisting(int typeId) {
        if (groupTypes.containsKey(typeId)) {
            return groupTypes.get(typeId);
        } else {
            return null;
        }
    }

    public String toString() {
        if (Log.isTraceEnabled()) {
            return super.toString();
        } else {
            return String.valueOf(this.typeId);
        }
    }

    public int getID() {
        return this.typeId;
    }
}