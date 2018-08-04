package bnw.abm.intg.sync.wdwrapper.reload;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunState;
import repast.simphony.random.RandomHelper;
import WeddingGrid.Cohort;
import WeddingGrid.Female;
import WeddingGrid.Human;
import WeddingGrid.Male;
import WeddingGrid.TheWorld;
import bnw.abm.intg.filemanager.csv.CSVReader;

public class MaritalRel4yrAgeLoader extends PopulationLoader {

    public MaritalRel4yrAgeLoader(Context<Object> context) {
        super(context);
    }

    @Override
    public void reloadMergedPopulation(Path agentsFile) throws IOException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {

        Map<Integer, Map<String, Object>> csvAgent2HumanAgentMap = new HashMap<>();// Map csv agent id to csv agent and Human agent, bookkeeping
        BufferedReader reader = Files.newBufferedReader(agentsFile);
        CSVReader csvr = new CSVReader(CSVFormat.DEFAULT);
        ArrayList<LinkedHashMap<String, Object>> csvAgentRecords = csvr.readCsvGroupByRow(reader, 0);
        boolean isYAxisAge = false;
        LinkedHashMap<Integer, List<Human>> agentsByAgeCats = new LinkedHashMap<>();

        for (LinkedHashMap<String, Object> csvAgent : csvAgentRecords) {

            // bookkeeping need to keep track of agent instances
            Map<String, Object> agentDetails = new HashMap<>(3);
            csvAgent2HumanAgentMap.put(Integer.parseInt((String) csvAgent.get("AgentId")), agentDetails);
            agentDetails.put("CSV", csvAgent);

            // Spawn agents and assign them age and gender.
            Human human;
            int type = Integer.parseInt((String) csvAgent.get("Type"));
            int age = age(type, 4, 26);
            if (type <= 51) {
                human = new Male(context, space, grid, theWorld.startYear - age, age, isYAxisAge);
            } else {
                human = new Female(context, space, grid, theWorld.startYear - age, age, isYAxisAge);
                theWorld.totalWomanInAge[human.getAge() / 5]++;
            }
            theWorld.totalHumanInAge[human.getAge() / 5]++;

            human.addObserver(theWorld);
            human.setID(theWorld.getID());

            // Initially place agents in random locations
            if (isYAxisAge) {
                double width = space.getDimensions().getWidth();
                space.moveTo(human.display, RandomHelper.nextDoubleFromTo(0, width), age);
            } else {
                double width = space.getDimensions().getWidth();
                double height = space.getDimensions().getHeight();
                space.moveTo(human.display, RandomHelper.nextDoubleFromTo(0, width), RandomHelper.nextDoubleFromTo(0, height));
            }

            agentDetails.put("HUMAN", human);// Bookkeeping

            this.updateCounters(human);

            if (agentsByAgeCats.get(human.getAge()) == null) {
                List<Human> agents = new ArrayList<>();
                agents.add(human);
                agentsByAgeCats.put(human.getAge(), agents);
            } else {
                agentsByAgeCats.get(human.getAge()).add(human);
            }
        }
        System.out.println(theWorld.popsize);
        /* We have spawned all the agents. Now lets establish relationships between agents and move them to suitable locations accordingly. */

        // First of all parental relationships. If person has the mother, place person near her. If person only has the father, then place the
        // person near him.

        for (LinkedHashMap<String, Object> csvAgent : csvAgentRecords) {
            int id = Integer.parseInt((String) csvAgent.get("AgentId"));
            Human me = (Human) csvAgent2HumanAgentMap.get(id).get("HUMAN");

            String partnerIdstr = ((String) csvAgent.get("Married")).trim().replace("[", "").replace("]", "");
            if (!partnerIdstr.equals("") && me instanceof Female) {
                int partnerId = Integer.parseInt(partnerIdstr);
                Human myPartner = (Human) csvAgent2HumanAgentMap.get(partnerId).get("HUMAN");
                myPartner.getMarried(me);
                me.getMarried(myPartner);

            }

            String fatherIdstr = ((String) csvAgent.get("ChildOfFather")).trim().replace("[", "").replace("]", "");
            String motherIdstr = ((String) csvAgent.get("ChildOfMother")).trim().replace("[", "").replace("]", "");
            int motherId = -1, fatherId = -1;
            Female mother = null;
            Male father = null;
            if (!motherIdstr.equals("")) {
                motherId = Integer.parseInt(motherIdstr);
                mother = (Female) csvAgent2HumanAgentMap.get(motherId).get("HUMAN");
                mother.children.add(me);
                mother.parity = mother.parity + 1;
                // NdPoint pt = space.getLocation(mother.display);
                // double yLocation = isYAxisAge ? 0 : RandomHelper.nextDoubleFromTo(pt.getY() - Human.sd * 2, pt.getY() + Human.sd * 2);
                // space.moveTo(me.display, RandomHelper.nextDoubleFromTo(pt.getX() - Human.sd * 2, pt.getX() + Human.sd * 2), yLocation);
                // pt = space.getLocation(me.display);
                // grid.moveTo(me.display, (int) pt.getX(), (int) pt.getY());
                // mother.parity = mother.parity + 1;
                // for(Human child: me.children){
                //
                // }
            }

            if (!fatherIdstr.equals("")) {
                fatherId = Integer.parseInt(fatherIdstr);
                father = (Male) csvAgent2HumanAgentMap.get(fatherId).get("HUMAN");
                father.children.add(me);
                // if (mother == null) {
                // NdPoint pt = space.getLocation(father.display);
                // double yLocation = isYAxisAge ? 0 : RandomHelper.nextDoubleFromTo(pt.getY() - Human.sd * 2, pt.getY() + Human.sd * 2);
                // space.moveTo(me.display, RandomHelper.nextDoubleFromTo(pt.getX() - Human.sd * 2, pt.getX() + Human.sd * 2), yLocation);
                // pt = space.getLocation(me.display);
                // grid.moveTo(me.display, (int) pt.getX(), (int) pt.getY());
                // }
            }
        }
        this.setupCohorts(agentsByAgeCats, theWorld.cohorts);
    }

    public void updateCounters(Human human) {
        TheWorld world = (TheWorld) RunState.getInstance().getMasterContext().getObjects(TheWorld.class).get(0);
        world.popsize++;
        int lastExecutedYear = world.year - 1; // Simulation has already incremented to next year. So, we need to take -1
        int age = human.getAge();
        world.totalHumanInAge[age / 5]++;

        if (human.isMarried()) {
            world.totalMarriedInAge[age / 5]++;
            if (human instanceof Female) {
                /**
                 * Number of women married by age
                 */
                world.totalMarriedWomanInAge[age / 5]++;
            }
        }
        if (human instanceof Female) {
            world.totalWomanInAge[age / 5]++;
            for (Human child : human.children) {
                int momAgeAtBirth = human.getAge() - child.getAge();
                int bornYear = lastExecutedYear - child.getAge();

            }
        }
    }

    private void setupCohorts(Map<Integer, List<Human>> agentsByAgeGroup, List<Cohort> cohorts) {
        agentsByAgeGroup = agentsByAgeGroup.entrySet().stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        for (List<Human> agentList : agentsByAgeGroup.values()) {
            Cohort cohort =new Cohort((ArrayList<Human>) agentList);
            cohort.marriageable = (int) agentList.stream().filter(a -> !a.isMarried()).count();
            cohort.married = agentList.size() - cohort.marriageable;
            cohort.count = agentList.get(0).getAge();
            cohort.newlyMarried = (int) agentList.stream().filter(a -> a.marDur == 1).count();
            cohorts.add(cohort);
        }
        TheWorld world = (TheWorld)RunState.getInstance().getMasterContext().getObjects(TheWorld.class).get(0);
        world.cohortCount = agentsByAgeGroup.size();
    }

    /**
     * Finds the age when given agent type and age gap. Assumes age has a uniform distribution within age gap
     * 
     * @param agentType
     *            Index of the agent type
     * @param ageGap
     *            number of years in each age gap
     * @param ageCategories
     *            number of age categories
     * @return
     */
    int age(int agentType, int ageGap, int ageCategories) {
        int low = (agentType % ageCategories) * ageGap;
        int high = low + ageGap - 1;
        return RandomHelper.getUniform().nextIntFromTo(low, high);
    }
}
