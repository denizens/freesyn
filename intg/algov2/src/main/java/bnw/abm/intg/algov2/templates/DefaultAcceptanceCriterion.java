package bnw.abm.intg.algov2.templates;

import java.util.List;

import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.framework.models.AgentType.TargetAgentType;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;
import bnw.abm.intg.algov2.framework.models.LinkRules.LinkRulesWrapper;
import bnw.abm.intg.algov2.framework.models.LinkType.NONELinkType;

/**
 * Validates whether links of agents in the validated group template. Checks whether (1) links of an agents are its legal ones, (2) there are
 * only one type NONELinkType links in the group, (3)agents have formed at least the required number of compulsory links and (4) agents have not
 * exceeded the maximum allowed number of links of a given type
 * 
 * @author Bhagya N. Wickramasinghe
 *
 */
public class DefaultAcceptanceCriterion extends AcceptanceCriterion {

    @SuppressWarnings("rawtypes")
    public Accept validate(GroupTemplate groupTemplate) {

        Class noneLinkClass = null;

        for (Member reference : groupTemplate.getAllMembers()) {

            // We want to check if the given link type is one of reference agent's legal links.
            for (LinkType existingLinkType : groupTemplate.row(reference).keySet()) {
                if (!LinkRulesWrapper.contains(reference, existingLinkType)) {
                    throw new IllegalStateException("In "+groupTemplate.getGroupType()+" " +groupTemplate+" "+ groupTemplate.toStringFullMode() + ", " + reference + " has formed link type " + existingLinkType
                            + " with " + groupTemplate.get(reference, existingLinkType) + ", which is illegal according to LinkRules");
                }

                // NONELinkType - there must not be two different NONELinkType instances in same group
                if (existingLinkType instanceof NONELinkType) {
                    if (noneLinkClass != null && noneLinkClass != existingLinkType.getClass()) {
                        throw new IllegalStateException("In " + groupTemplate + ", " + reference + " has two different NONELinkTypes: "
                                + noneLinkClass + " and " + existingLinkType.getClass() + ". Only one NONELinkType must be allowed in a group");
                    } else if (noneLinkClass == null) {
                        noneLinkClass = existingLinkType.getClass();
                    }
                } else if (noneLinkClass != null) {
                    throw new IllegalStateException("In " + groupTemplate + ", " + reference + " has a NONELinkType: " + noneLinkClass
                            + " and a normal LinkType" + existingLinkType.getClass()
                            + ". A group must not have both NONELinkType and normal LinkType links");
                } else if (!(existingLinkType instanceof NONELinkType) && noneLinkClass == null) {
                    // All good
                } else {
                    throw new IllegalStateException("Unexpected state in group: " + groupTemplate);
                }
            }
            if (noneLinkClass == null) {
                for (LinkType refsLinkType : LinkRulesWrapper.row(reference).keySet()) { // Ref's all links
                    if (!(refsLinkType instanceof NONELinkType)) {
                        if (!this.linksValidation(groupTemplate, reference, refsLinkType))
                            return Accept.NO;
                    }
                }
            } else {
                for (LinkType refsLinkType : LinkRulesWrapper.row(reference).keySet()) { // Ref's all links
                    if (refsLinkType instanceof NONELinkType) {
                        if (!this.linksValidation(groupTemplate, reference, refsLinkType))
                            return Accept.NO;
                    }
                }
            }
        }
        return Accept.YES;
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

        // Has each reference agent fulfilled all its compulsory links
        int refsMinLinks = refsLinkType.getMinLinks();
        int refsMaxLinks = refsLinkType.getMaxLinks();
        if (targets.size() < refsMinLinks | refsMaxLinks < targets.size()) {
            return false;
        }
        return true;
    }
}
