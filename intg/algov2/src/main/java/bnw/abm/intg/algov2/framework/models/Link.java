package bnw.abm.intg.algov2.framework.models;

public class Link {
    final private LinkType type;

    public Link(LinkType type) {
        this.type = type;
    }

    public LinkType getType() {
        return this.type;
    }

    public String toString() {
        return this.type.getTypeId();
    }
}
