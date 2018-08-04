package bnw.abm.intg.apps.wdll;

import java.util.ArrayList;
import java.util.List;

import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.templates.GroupTypeLogic;

public class HouseholdTypeLogic extends GroupTypeLogic {

    List<GroupType> householdTypes = new ArrayList<>(8);

    public HouseholdTypeLogic() {

    }

    @Override
    public GroupType computeGroupType(GroupTemplate groupTemplate) {
        int hhSize = groupTemplate.size();
        return GroupType.getInstance(hhSize);

    }
}
