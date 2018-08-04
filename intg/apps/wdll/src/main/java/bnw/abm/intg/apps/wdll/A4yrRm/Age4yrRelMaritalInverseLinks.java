package bnw.abm.intg.apps.wdll.A4yrRm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;
import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.templates.LinkCondition;
import bnw.abm.intg.util.GlobalConstants.EXITCODE;
import bnw.abm.intg.util.Log;

public class Age4yrRelMaritalInverseLinks extends LinkCondition {

    Map<LinkType, LinkType> inversions;
    private final int maxMaleIndex;;

    /**
     * Constructs inverse links link condition instance
     * 
     * @param maxMaleIndex
     *            maximum male index (inclusive)
     */
    public Age4yrRelMaritalInverseLinks(int maxMaleIndex) {
        this.maxMaleIndex = maxMaleIndex;
        this.inversions = new HashMap<>();
        inversions.put(LinkType.getInstance("NONE"), LinkType.getInstance("NONE"));
        inversions.put(LinkType.getInstance("Married"), LinkType.getInstance("Married"));
        inversions.put(LinkType.getInstance("ParentOf"), LinkType.getInstance("ChildOf"));
        inversions.put(LinkType.getInstance("ChildOfFather"), LinkType.getInstance("ParentOf"));
        inversions.put(LinkType.getInstance("ChildOfMother"), LinkType.getInstance("ParentOf"));
    }

    @Override
    protected void applyCondition(GroupTemplate groupTemplate, Member reference, LinkType linkType, List<Member> targets) {
        LinkType inverseLinkType = inversions.get(linkType);
        if (inverseLinkType != null) {
            if (inverseLinkType.isSameType(LinkType.getExisting("ChildOf"))) {
                if (reference.getTypeID() <= this.maxMaleIndex) {
                    // reference is male
                    inverseLinkType = LinkType.getExisting("ChildOfFather");
                } else {
                    inverseLinkType = LinkType.getExisting("ChildOfMother");
                }
            }
        } else {
            Log.errorAndExit("Unrecognised LinkType:" + linkType + ". Its inverse link returned null.", EXITCODE.PROGERROR);
        }

        for (Member targetMember : targets)
            groupTemplate.put(targetMember, inverseLinkType, reference);
    }

}
