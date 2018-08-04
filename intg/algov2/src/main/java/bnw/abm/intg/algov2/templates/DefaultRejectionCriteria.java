package bnw.abm.intg.algov2.templates;

import java.util.List;

import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.framework.models.AgentType.TargetAgentType;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;
import bnw.abm.intg.algov2.framework.models.LinkRules.LinkRulesWrapper;

public class DefaultRejectionCriteria extends RejectionCriterion {

    @Override
    public Reject validate(GroupTemplate groupTemplate) {
        // See if any member has formed links that they should not have formed
        for (Member reference : groupTemplate.getAllMembers()) {
            for (LinkType refsLinkType : LinkRulesWrapper.row(reference).keySet()) { // Ref's all links
                if (linksValidation(groupTemplate, reference, refsLinkType) == false) {
                    return Reject.YES;
                }
            }
        }
        return Reject.NO;
    }

    private boolean linksValidation(GroupTemplate groupTemplate, Member reference, LinkType refsLinkType) {

        List<Member> targets = groupTemplate.getAdjacentMembers(reference, refsLinkType);

        // Is reference agent type allowed to form a link of this type with every AgentType in target list
        int matchedCount = 0;
        List<TargetAgentType> expectedTargets = LinkRulesWrapper.get(reference, refsLinkType);
        for (Member member : targets) {
            for (TargetAgentType expected : expectedTargets) {
                if (expected.isSameType(member)) {
                    matchedCount++;
                    break;
                }
            }
        }
        if (matchedCount != targets.size()) {
            return false;
        }

        int refsMaxLinks = refsLinkType.getMaxLinks();
        if (refsMaxLinks < targets.size()) { // ref has more links than it should
            return false;
        }
        return true;
    }

}

class DefaultGroupSizeRejectionCriterion extends RejectionCriterion {
    private final int maxGroupSize;

    public DefaultGroupSizeRejectionCriterion(int maxGroupSize) {
        this.maxGroupSize = maxGroupSize;
    }

    @Override
    public Reject validate(GroupTemplate groupTemplate) {
        if (groupTemplate.size() > maxGroupSize) {
            return Reject.YES;
        } else {
            return Reject.NO;
        }
    }
}