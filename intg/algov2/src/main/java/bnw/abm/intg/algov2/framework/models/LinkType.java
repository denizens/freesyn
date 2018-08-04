package bnw.abm.intg.algov2.framework.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import bnw.abm.intg.util.Log;

/**
 * Place holder instance of unique link types
 * 
 * @author Bhagya N. Wickramasinghe
 *
 */
public class LinkType implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -6153736435841561790L;
    private static Map<String, LinkType> linkTypes = new HashMap<>();
    public static final NONELinkType NONE = NONELinkType.getInstance("NONE");
    protected String typeId;
    protected int min, max;
    protected boolean active;

    private LinkType(String linkTypeId) {
        this.typeId = linkTypeId;
        this.min = 1;
        this.max = 1;
        this.active = true;
    }

    /**
     * The constructor for Decorator pattern
     */
    protected LinkType() {
    }

    /**
     * Returns the link type instance of the specified link name. If there is no instance, a new instance is created with provided link type ID.
     * number of Min and Max links are set to 0 by default
     * 
     * @param linkTypeId
     *            Unique ID for the link type. Must be not empty or null
     * @return Instance of the specified link type.
     */
    public static LinkType getInstance(String linkTypeId) {
        LinkType instance = null;
        if (linkTypeId == null || linkTypeId.equals("")) {
            new IllegalArgumentException("Link type ID must not be null or empty");
        }

        if (!linkTypes.containsKey(linkTypeId)) {
            instance = new LinkType(linkTypeId);
            linkTypes.put(linkTypeId, instance);
        } else {
            instance = linkTypes.get(linkTypeId);
            if (instance == null) {
                throw new UnknownError("Link Type is null. LinkTypeId: " + linkTypeId);
            }
        }
        return instance;
    }

    public static LinkType getInstance(String linkTypeId, int min, int max, boolean isActive) {
        LinkType instance = LinkType.getInstance(linkTypeId);
        instance.setMinLinks(min);
        instance.setMaxLinks(max);
        instance.setActive(isActive);
        return instance;
    }

    /**
     * LinkTypes have only one instance from a given type ID. We use decorators for these LinkType instances in various places of the program.
     * This method checks if this instance and LinkType parameter has originated from same original LinkType instance. If this instance and
     * parameter have both originated from same LinkType instance then returns TRUE, otherwise returns FALSE.
     * 
     * @param o
     *            LinkType instance to compare
     * @return True or False depending on original LinkType instances.
     */
    public boolean isSameType(LinkType o) {
        return this.typeId.equals(o.getTypeId());
    }

    /**
     * Returns existing LinkType instance.
     * 
     * @param typeId
     *            Name of the link type to get
     * @return Link type instance. Null if there is no link type by given link name.
     */
    public static LinkType getExisting(String typeId) {
        return linkTypes.get(typeId);
    }

    /**
     * Get the ID of this link type
     * 
     * @return ID of link type
     */
    public String getTypeId() {
        return typeId;
    }

    protected void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    /**
     * Sets number of minimum required links
     * 
     * @param count
     *            minimum number of links
     */
    public void setMinLinks(int count) {
        this.min = count;
    }

    /**
     * Sets number of maximum allowed links
     * 
     * @param count
     *            maximum number of links
     */
    public void setMaxLinks(int count) {
        this.max = count;
    }

    /**
     * Get max number of links
     * 
     * @return max number of links
     */
    public int getMinLinks() {
        return this.min;
    }

    /**
     * Get minimum number of links
     * 
     * @return minimum number of links
     */
    public int getMaxLinks() {
        return this.max;
    }

    /**
     * Checks if this LinkType instance is of ActiveLinkType.
     * 
     * @return True if this is an ActiveLinkType, False if this is an PassiveLinkType, Throws UnsupportedOpertationException if this LinkType is
     *         not decorated as either ActiveLinkType or PassiveLinkType
     */
    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean isActive) {
        this.active = isActive;
    }

    /**
     * Returns String representation. If Log Trace is enabled returns object representation. Otherwise returns LinkType's ID's String
     * representation
     */
    public String toString() {
        if (Log.isTraceEnabled()) {
            return this.getClass().getName() + "@" + Integer.toHexString(this.hashCode());
        } else {
            return getTypeId().toString();
        }
    }

    public static class NONELinkType extends LinkType {

        /**
         * 
         */
        private static final long serialVersionUID = 2849227108303647866L;

        protected NONELinkType(String linkTypeId) {
            this.typeId = linkTypeId;
            this.min = 0;
            this.max = 0;
            this.active = true;
        }


        public static NONELinkType getInstance(String linkTypeId) {
            NONELinkType instance = null;
            if (linkTypeId == null || linkTypeId.equals("")) {
                new IllegalArgumentException("Link type ID must not be null or empty");
            }

            if (!linkTypes.containsKey(linkTypeId)) {
                instance = new NONELinkType(linkTypeId);
                linkTypes.put(linkTypeId, instance);
            } else if (linkTypes.get(linkTypeId) == null) {
                throw new UnknownError("Link Type instance is null. LinkTypeId: " + linkTypeId);
            } else if (linkTypes.get(linkTypeId) instanceof NONELinkType) {
                instance = (NONELinkType) linkTypes.get(linkTypeId);
            } else {
                throw new ClassCastException("The existing link type instance is not a NONELinkType: "+linkTypeId);
            }

            return instance;
        }

        public static NONELinkType getInstance(String linkTypeId, int min, int max, boolean isActive) {
            NONELinkType instance = NONELinkType.getInstance(linkTypeId);
            instance.setMinLinks(min);
            instance.setMaxLinks(max);
            instance.setActive(isActive);
            return instance;
        }
    }
}
