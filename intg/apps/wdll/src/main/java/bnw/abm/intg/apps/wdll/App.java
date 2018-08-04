package bnw.abm.intg.apps.wdll;

import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.templates.GroupTypeLogic;
import bnw.abm.intg.apps.wdll.A4yrRm.MainAlgoV3Age4yrRelMarital;
import bnw.abm.intg.apps.wdll.A4yrRmp.MainAlgoV3Age4yrRelMaritalParental;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {

        if (args.length >= 2) {
            switch (args[1]) {
            case "WDLL":
                WDLL_4yrMariralRel.run(args[0]);
                break;
            case "WDLL_LatchAgeMaritalRel":
                WDLL_LatchAgeMarritalRel.run(args[0], args[1]);
                break;
            case "WDLL_4yrAgeMaritalParentalRel":
                MainAlgoV3Age4yrRelMaritalParental.build(args[0]);
                break;
            case "WDLL_4yrAgeMaritalRel":
                MainAlgoV3Age4yrRelMarital.build(args[0]);
            default:
                break;
            }
        }
    }
}

class SizeBasedGroupType extends GroupTypeLogic {

    @Override
    public GroupType computeGroupType(GroupTemplate groupTemplate) {
        return GroupType.getInstance(groupTemplate.size());
    }
}