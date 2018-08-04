package bnw.abm.intg.algov1;

import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.GroupRules.GroupRuleKeys;
import bnw.abm.intg.algov2.framework.models.GroupRules.GroupRulesWrapper;
import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.GroupTemplate.Member;
import bnw.abm.intg.algov2.framework.models.LinkRules.LinkRulesWrapper;
import bnw.abm.intg.algov2.framework.models.LinkType;
import bnw.abm.intg.algov2.templates.AcceptanceCriterion;
import bnw.abm.intg.algov2.templates.RejectionCriterion;

public class Algov1Criteria {
    public static class DefaultGroupSizeAccept extends AcceptanceCriterion {

        @Override
        public Accept validate(GroupTemplate groupTemplate) {
            int membersCount = (Integer) GroupRulesWrapper.get(groupTemplate.getGroupType(), GroupRuleKeys.MAX_MEMBERS);
            if (groupTemplate.size() == membersCount)
                return Accept.YES;
            else
                return Accept.NO;
        }
    }

    public static class DefaultGroupSizeReject extends RejectionCriterion {

        @Override
        public Reject validate(GroupTemplate groupTemplate) {

            int maxMembers = (Integer) GroupRulesWrapper.get(groupTemplate.getGroupType(), GroupRuleKeys.MAX_MEMBERS);
            if (groupTemplate.size() > maxMembers) {
                return Reject.YES;
            } else {
                for (Member member : groupTemplate.getAllMembers()) {
                    for (LinkType linkType : LinkRulesWrapper.row(ReferenceAgentType.getExisting(member.getTypeID())).keySet()) {
                        int existingLinks = groupTemplate.getAdjacentMembers(member, linkType).size();
                        int requiredButUnformedLinks = (linkType.getMinLinks() > existingLinks) ? (linkType.getMinLinks() - existingLinks) : 0;
                        if ((maxMembers - groupTemplate.size()) < requiredButUnformedLinks) {
                            return Reject.YES;
                        }
                    }
                }
                return Reject.NO;
            }
        }
    }
}
