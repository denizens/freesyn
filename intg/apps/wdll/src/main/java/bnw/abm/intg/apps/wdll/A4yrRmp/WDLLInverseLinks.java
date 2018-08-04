package bnw.abm.intg.apps.wdll.A4yrRmp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;
import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.templates.LinkCondition;
import bnw.abm.intg.util.GlobalConstants.EXITCODE;
import bnw.abm.intg.util.Log;

public class WDLLInverseLinks extends LinkCondition {

    Map<LinkType, LinkType> inversions;
    private final int maxMaleIndex;;

    /**
     * Constructs inverse links link condition instance
     * 
     * @param maxMaleIndex
     *            maximum male index (inclusive)
     */
    public WDLLInverseLinks(int maxMaleIndex) {
        this.maxMaleIndex = maxMaleIndex;
        this.inversions = new HashMap<>();
        inversions.put(LinkType.getInstance("NONE"), LinkType.getInstance("NONE"));
        inversions.put(LinkType.getInstance("Married"), LinkType.getInstance("Married"));
    }

    @Override
    protected void applyCondition(GroupTemplate groupTemplate, Member reference, LinkType linkType, List<Member> targets) {
        LinkType inverseLinkType = null;
        if (linkType.isSameType(LinkType.getExisting("Married"))) {
            inverseLinkType = LinkType.getExisting("Married");
        } else if (linkType.isSameType(LinkType.getExisting("ParentOfU15Child"))
                || linkType.isSameType(LinkType.getExisting("ParentOfO15Child"))) {
            if (reference.getTypeID() <= this.maxMaleIndex) {
                // reference is male
                inverseLinkType = LinkType.getExisting("ChildOfFather");
            } else {
                inverseLinkType = LinkType.getExisting("ChildOfMother");
            }
        } else if (linkType.isSameType(LinkType.getExisting("ChildOfFather")) | linkType.isSameType(LinkType.getExisting("ChildOfMother"))) {
            if (reference.getTypeID() <= 3 || (208 <= reference.getTypeID() && reference.getTypeID() <= 211)) {
                inverseLinkType = LinkType.getExisting("ParentOfU15Child");
            } else {
                inverseLinkType = LinkType.getExisting("ParentOfO15Child");
            }
        } else if (linkType.isSameType(LinkType.NONE)) {
            inverseLinkType = LinkType.NONE;

        } else {
            Log.errorAndExit("Unrecognised LinkType:" + linkType, EXITCODE.PROGERROR);
        }

        for (Member targetMember : targets)
            groupTemplate.put(targetMember, inverseLinkType, reference);
    }
}
