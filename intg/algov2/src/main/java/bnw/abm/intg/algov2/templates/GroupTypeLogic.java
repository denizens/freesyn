package bnw.abm.intg.algov2.templates;

import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.GroupType;

abstract public class GroupTypeLogic {

    abstract public GroupType computeGroupType(GroupTemplate groupTemplate);

    public static class GroupTypeLogicCaller {

        private static GroupTypeLogic gtl;

        public static void setGroupTypeLogic(GroupTypeLogic gtl) {
            GroupTypeLogicCaller.gtl = gtl;
        }

        public static GroupType computeGroupType(GroupTemplate groupTemplate) {
            return gtl.computeGroupType(groupTemplate);
        }
    }
}
