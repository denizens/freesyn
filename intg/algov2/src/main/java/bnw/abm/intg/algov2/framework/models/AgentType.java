package bnw.abm.intg.algov2.framework.models;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import bnw.abm.intg.util.Log;

/**
 * AgentType
 * 
 * @author Bhagya N. Wickramasinghe
 *
 */
abstract public class AgentType implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 3147014194443332723L;

    final private int typeID;

    protected AgentType(int agentType) {
        this.typeID = agentType;
    }

    /**
     * @return AgentType identifier
     */
    public int getTypeID() {
        return this.typeID;
    }

    public String toString() {
        if (Log.isTraceEnabled()) {
            return this.getClass().getName() + "@" + Integer.toHexString(this.hashCode());
        } else {
            return String.valueOf(this.typeID);
        }

    }

    abstract public String getIdentifier();

    /**
     * Checks if parameter's agent type matches to this
     * 
     * @param agentType
     *            AgentType instance to compare
     * @return True if both instances have same type, False if not.
     */
    public boolean isSameType(AgentType agentType) {
        if (agentType.getTypeID() == this.typeID) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * Decorates AgentType as TargetAgentType. Constructor is private, so instantiable only within LinkRulesBuilder class.
     * 
     * @author Bhagya N. Wickramasinghe
     *
     */
    final public static class TargetAgentType extends AgentType {

        /**
         * 
         */
        private static final long serialVersionUID = -2714812552994841103L;
        private final double weight;
        private final int id;
        private static Integer idCounter = 0;

        public TargetAgentType(int type, double weight) {
            super(type);
            this.weight = weight;
            synchronized (idCounter) {
                this.id = idCounter;
                idCounter++;
            }

        }

        /**
         * Returns the weight of this target agent's link
         * 
         * @return Double weight value
         */
        public double getWeight() {
            return this.weight;
        }

        @Override
        public String toString() {
            if (Log.isTraceEnabled()) {
                return this.getClass().getName() + "@" + Integer.toHexString(this.hashCode());
            } else {
                return this.getIdentifier();
            }
        }

        @Override
        public String getIdentifier() {
            return "T" + String.valueOf(super.typeID);
        }
    }

    /**
     * Decorates AgentType as ReferenceAgentType. Constructor is private, so instantiable only within LinkRulesBuilder class.
     * 
     * @author Bhagya N. Wickramasinghe
     *
     */
    final public static class ReferenceAgentType extends AgentType {

        /**
         * 
         */
        private static final long serialVersionUID = -5932669442594871969L;
        private static final Map<Integer, ReferenceAgentType> existingRefAgents = new ConcurrentHashMap<>();
        private final int id;
        private static Integer idCounter = 0;

        /**
         * Creates a new instance of ReferenceAgentType decorating AgentType parameter's original AgentType instance.
         * 
         * @param agentType
         *            Type of the new ReferenceAgentType
         */
        private ReferenceAgentType(int agentType) {
            super(agentType);
            synchronized (idCounter) {
                this.id = idCounter;
                idCounter++;
            }

        }

        /**
         * Returns the existing decorated ReferenceAgenType instance of the given agentType parameter. agentType parameter's original AgentType
         * instance used internally.
         * 
         * @param agentType
         *            Agent type instance to be decorated
         * @return instance of ReferenceAgentType. Null if there is no decorated ReferenceAgentType instance
         */
        public static ReferenceAgentType getInstance(int agentType) {
            if (existingRefAgents.containsKey(agentType)) {
                return existingRefAgents.get(agentType);
            } else {
                ReferenceAgentType newRef = new ReferenceAgentType(agentType);
                existingRefAgents.put(agentType, newRef);
                return newRef;
            }
        }

        /**
         * Finds the corresponding ReferenceAgentType using agentType ID
         * 
         * @param agentTypeID
         *            ID of the agent type
         * @return ReferenceAgentType instance
         */
        public static ReferenceAgentType getExisting(int agentTypeID) {
            return existingRefAgents.getOrDefault(agentTypeID, null);
        }

        @Override
        public String toString() {
            if (Log.isTraceEnabled()) {
                return this.getClass().getName() + "@" + Integer.toHexString(this.hashCode());
            } else {
                return this.getIdentifier();
            }
        }

        @Override
        public String getIdentifier() {
            return "R" + String.valueOf(super.typeID);
        }
    }

}