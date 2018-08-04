package bnw.abm.intg.algov2.templates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import bnw.abm.intg.algov2.framework.models.GroupTemplate;

abstract public class AcceptanceCriterion {
    private static List<AcceptanceCriterion> rulesSet = Collections.synchronizedList(new ArrayList<AcceptanceCriterion>());

    abstract public Accept validate(GroupTemplate groupTemplate);

    public boolean register() {
        if (!rulesSet.contains(this))
            return rulesSet.add(this);
        else
            return false;
    }

    public static Accept validateAll(GroupTemplate groupTemplate) {
        for (AcceptanceCriterion r : rulesSet) {
            if (r.validate(groupTemplate) == Accept.NO) {
                return Accept.NO;
            }
        }
        return Accept.YES;
    }

    public enum Accept {
        YES,
        NO;
    }
}
