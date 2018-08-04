package bnw.abm.intg.algov2.framework.models;

public class Agent {
    private static int idcounter = 0;
    final private Integer type;

    final private int id;

    public Agent(AgentType type) {
        this.id = idcounter;
        idcounter++;
        this.type = type.getTypeID();
    }

    public Integer getType() {
        return this.type;
    }

    public String toString() {
        return Integer.toString(id);
    }

    public int getID() {
        return this.id;
    }
}
