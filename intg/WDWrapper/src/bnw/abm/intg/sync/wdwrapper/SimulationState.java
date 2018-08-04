package bnw.abm.intg.sync.wdwrapper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import WeddingGrid.Builder;
import WeddingGrid.Female;
import WeddingGrid.Human;
import WeddingGrid.HumanStyle;
import WeddingGrid.Male;
import WeddingGrid.TheWorld;

public class SimulationState implements ISimStatus {

    private static final Logger logger = Logger.getLogger(SimulationState.class.getName());
    /*
     * agentStates attributes Integers : gid,lid and age String: sex boolean: dead Integer Global IDs of: mother,father and partner
     */
    private ArrayList<Map<String, Object>> agentStates = null;

    /* Map of wrapper human object (WrapperHuman) and Human's id (local id) in the simulation. */
    ConcurrentHashMap<Number, WrapperHuman> wrapperPeople = new ConcurrentHashMap<>();

    private double tickcount = -1;

    public SimulationState(Context<Object> context) {
        saveStates(context);

    }

    /***
     * Retrieve current status of the agents in the simulation context object. Agents id, age, sex and partner's id are retrieved from agent
     * objects.
     * 
     * @param context
     *            context of Repast simulation
     * @return An ArrayList of Map objects containing info about the agent. Each Map object has agent id, age, sex and partner id.
     */
    public void saveStates(Context<Object> context) {
        // if (tickcount == RunEnvironment.getInstance().getCurrentSchedule().getTickCount()) {
        // return; // We have already done this once for this tick
        // } else {
        // tickcount = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
        // }
        ArrayList<Map<String, Object>> arrL = new ArrayList<Map<String, Object>>();

        /*
         * Mark agents who has died during the last step. If this is just after initialisation, wrapperPeople is empty (not null)
         */
        for (Number key : wrapperPeople.keySet()) {
            WrapperHuman wh = wrapperPeople.get(key);
            if (wh.isDead() == false) {
                Human h = wh.getHuman();
                // if(h.getID() == 1570){
                // System.out.println("");
                // }
                Field fld = null;
                int yearOfDeath = 0;
                boolean isIll = false;
                ArrayList<Human> children = null;
                try {
                    /*
                     * Simulation only instantiates Male or Female objects. yearOfDeath belongs to Human Super class. yearOfDeath is access
                     * restricted. so we have to use reflection #BNW
                     */
                    fld = h.getClass().getSuperclass().getDeclaredField("yearOfDeath");
                    fld.setAccessible(true);
                    yearOfDeath = fld.getInt(h);
                    Field fld2 = h.getClass().getSuperclass().getDeclaredField("isIll");
                    fld2.setAccessible(true);
                    isIll = (boolean) fld2.get(h);
                    Field fld3 = h.getClass().getSuperclass().getDeclaredField("children");
                    fld3.setAccessible(true);
                    children = (ArrayList<Human>) fld3.get(h);
                } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                    ex.printStackTrace();
                }

                if (yearOfDeath > 0) {
                    wh.setDead(true, yearOfDeath);
                }
                wh.setIll(isIll);

                for (Human child : children) {
                    wh.addChild(getWrapperHuman(context, child));
                }
            }
        }

        /*
         * We check for agents who are not in wrapperPeople. If there are, that means they are new and we should update local records and IS.
         * These can be newborns or just after initialisation
         */
        WrapperHuman wh = null;
        int agentCount = 0;
        for (Object obj : context) {
            if (obj instanceof Human) {
                agentCount++;
                Human h = ((Human) obj);
                // if(h.getID() == 1570){
                // System.out.println("");
                // }
                wh = getWrapperHuman(context, h);
                Map<String, Object> agent = new HashMap<String, Object>();
                agent.put("gid", wh.getGlobalID()); // Can be null until
                                                    // Integration Server
                                                    // assigns one
                agent.put("lid", h.getID());
                agent.put("age", h.getAge());
                agent.put("sex", GlobalConcept.convert2GlobalFormat(h.getGender(), GlobalConcept.SEX));
                agent.put("mother", (wh.getMother() != null) ? wh.getMother().getLocalID() : null);
                agent.put("father", (wh.getFather() != null) ? wh.getFather().getLocalID() : null);
                agent.put("dead", wh.isDead());
                /* Find current parnter's global ID */
                Number partnerID = null;
                WrapperHuman partner = null;
                if (h.getPartner() == null) {
                    partnerID = null;
                } else if (wrapperPeople.get(h.getPartner().getID()) != null) {

                    // We need to get the current partner. Instead of directly
                    // searching in wrapperPeople we look in current agent
                    // population so we won't miss any updates
                    partner = wrapperPeople.get(h.getPartner().getID());
                    partnerID = partner.getLocalID();
                    wh.setPartner(partner);
                } else {
                    partner = getWrapperHuman(context, h.getPartner());
                    partnerID = partner.getLocalID();
                    wh.setPartner(partner);
                }
                agent.put("partner", partnerID);

                arrL.add(agent);
            }
        }
        setAgentStatuses(arrL);
    }

    /**
     * Finds the corrosponding WrapperHuman Object of Human H. If WrapperHuman object is not present, a new one is created and returned
     * 
     * @param context
     *            Current Repast context
     * @param h
     *            Human whom we want to find WrapperHuman
     * @return Corresponding WrapperHuman objectelse if(){
     * 
     *         }
     */
    public WrapperHuman getWrapperHuman(Context<Object> context, Human h) {
        WrapperHuman wh = null, mother = null, father = null;
        if (h == null) {
            return null;
        } else if (wrapperPeople.containsKey(h.getID())) {
            return wrapperPeople.get(h.getID());
        } else {
            /*
             * If there is no entry in people hashmap for this Human agent, then add one. This can happen at the initialisation or because of a
             * new birth. To create WrapperHuman objects we need to find father and mother. Human objects do not have fields for father and
             * mother. So we have to iterate through all the females in the context to find the female who has our agent as her child. ArrayList
             * of children is a protected variable. So we have to use java reflection to access it. #BNW
             */
            for (Object ob : context) {
                if (ob instanceof Female) {
                    Female mom = (Female) ob;
                    ArrayList<Human> childrenLst = null;
                    try {
                        Field fld = mom.getClass().getSuperclass().getDeclaredField("children");
                        fld.setAccessible(true);
                        childrenLst = (ArrayList<Human>) fld.get(mom); // (ArrayList<Human>)
                                                                       // Human.class.getField("children").get(mom);
                    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                        e.printStackTrace();
                    }

                    for (Human child : childrenLst) {
                        if (child.getID() == h.getID()) {
                            mother = getWrapperHuman(context, mom);// This is
                                                                   // ugly -
                                                                   // recursive
                                                                   // call
                            if (mom != null) {
                                father = getWrapperHuman(context, mom.getPartner());
                                // System.out.println(mother.getID());
                            }
                            break;
                        }
                    }
                    if (mother != null) {
                        break;
                    }
                }
            }
            if (mother == null) {
                wh = new WrapperHuman(h, null, null);
            } else {
                wh = new WrapperHuman(h, mother, father);
            }
            wrapperPeople.put(h.getID(), wh);
            return wh;
        }

    }

    public ArrayList<Map<String, Object>> getAgentStatuses() {
        return agentStates;
    }

    public void setAgentStatuses(ArrayList<Map<String, Object>> agentStatuses) {
        this.agentStates = agentStatuses;
    }

    /**
     * Received GIDs from IS and updates local SimulationState with GIDs. Nothing more. This method assumes that no new agents are spawned by IS
     * 
     * @param agentLst
     *            List of hash maps with updated agent attributes
     */
    public void updateAgentStates(ArrayList<Map<String, Object>> agentLst) {
        for (Map<String, Object> agnt : agentLst) {
            WrapperHuman wh = wrapperPeople.get((int) (long) agnt.get("lid"));
            wh.setGlobalID((int) (long) agnt.get("gid"));
            // TODO: only global IDs are updated. Others need to be updated too
        }
        agentStates = agentLst;
    }

    public void updateAgentParents(ArrayList<Map<String, Object>> agentLst, Context context) {
        Iterator<Map<String, Object>> itr = agentLst.iterator();
        while (itr.hasNext()) {
            HashMap agnt = (HashMap) itr.next();
            WrapperHuman whChild = wrapperPeople.get(agnt.get("lid"));

            /*
             * This can be true during initialisation. Orphan kids are given new parents in LL. So we have to update WD to reflect adoptions
             */
            if (whChild.getMother() == null && agnt.get("mother") != null) {
                adoptOrphans(whChild, agnt);

            }
            /*
             * This agent already have a mother. So let's see if he has changed the mother. This could happen if agent is a child and have become
             * orphan because both parents died
             */
            else if (whChild.getMother() != null && !(whChild.getMother().getLocalID().equals((Number) agnt.get("mother")))) {
                adoptOrphans(whChild, agnt);

            }

        }

    }

    public void deleteAllAgents(Context<Object> context) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        TheWorld theWorld = null;
        for (Object obj : context) {
            if (obj instanceof Builder) {
                theWorld = ((Builder) obj).theWorld;
            }
        }

        while (context.getObjects(Human.class).size() > 0) {
            Object ob = context.getObjects(Human.class).get(0);
            if (ob instanceof Human) {
                Human human = (Human) ob;

                // Observer pattern related calls.
                Method setChanged = null;
                try {
                    setChanged = human.getClass().getSuperclass().getSuperclass().getDeclaredMethod("setChanged");
                    setChanged.setAccessible(true);
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                setChanged.invoke(human);

                human.notifyObservers("Died");

                // Removing display of this agent from context
                HumanStyle display = null;
                try {
                    Field displayField = human.getClass().getSuperclass().getDeclaredField("display");
                    displayField.setAccessible(true);
                    display = (HumanStyle) displayField.get(human);
                } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                context.remove(display);
                context.remove(human);

                try {
                    Field fld = null;

                    fld = human.getClass().getSuperclass().getDeclaredField("yearOfDeath");
                    fld.setAccessible(true);
                    fld.setInt(human, 1);
                } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }

                theWorld.censusCheck--;
            }
        }
    }

    public void resetSimulationCountersTo0(Context<Object> context) {
        TheWorld world = (TheWorld) context.getObjects(TheWorld.class).get(0);
        world.popsize = 0;

        for (int a = 0; a <= world.maxAge; a++) {
            /**
             * Presents mother's age when a child was born in a particular year.
             */
            world.birthsByAgeAndYear[a][world.year] = 0;
            world.humanByAgeAndYear[a][world.year] = 0;
            /**
             * Gives how many people(male and female) in a given age got married in each year
             */
            world.marriagesByAgeAndYear[a][world.year] = 0;
            world.humanByAgeAndYear[a][world.year] = 0;
            /**
             * Gives number of children born to married mothers in a given age in each year.
             */
            world.marriedBirthsByAgeAndYear[a][world.year] = 0;
            world.birthsByAgeAndYear[a][world.year] = 0;
            world.womenByAgeAndYear[a][world.year] = 0;

            world.totalHumanInAge[a / 5] = 0;
            world.totalMarriedInAge[a / 5] = 0;
            world.totalMarriedWomanInAge[a / 5] = 0;
            world.totalWomanInAge[a / 5] = 0;
        }
        world.cohorts = new ArrayList<>();
        world.cohortCount = 0;
    }

    private void adoptOrphans(WrapperHuman whChild, HashMap agnt) {
        Number newMomID = (Number) agnt.get("mother");
        WrapperHuman wNewMom = wrapperPeople.get(newMomID);

        /* set wNewMom as whChild's WH-Mother, and then set father */
        whChild.setMother(wNewMom);
        whChild.setFather(wrapperPeople.get(wNewMom.getHuman().getPartner().getID()));

        /* set whChild.getHuman() as wNewMom.getHuman()'s child */
        Female mom = (Female) wNewMom.getHuman();

        ArrayList<Human> childrenLst = null;
        try {
            Field fld = mom.getClass().getSuperclass().getDeclaredField("children");
            fld.setAccessible(true);
            childrenLst = (ArrayList<Human>) fld.get(mom); // #BNW reflection
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
        childrenLst.add(whChild.getHuman());

        /* TODO: Now move the child to Human mother's location */

        /* Next update details of the father */
        Male dad = (Male) wNewMom.getHuman().getPartner();
        childrenLst = null;
        try {
            Field fld = dad.getClass().getSuperclass().getDeclaredField("children");
            fld.setAccessible(true);
            childrenLst = (ArrayList<Human>) fld.get(dad); // #BNW reflection
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
        childrenLst.add(whChild.getHuman());
    }

    public void adoptOrphansL(Context<Object> context, int seed) {
        saveStates(context);
        WDRandomHelper randomHelper = new WDRandomHelper(seed);
        // Random random =new Random(seed);

        ArrayList<WrapperHuman> orphans = new ArrayList<WrapperHuman>();
        ArrayList<WrapperHuman> potentialMothers = new ArrayList<WrapperHuman>();
        for (Number key : wrapperPeople.keySet()) {
            WrapperHuman person = wrapperPeople.get(key);
            if (!person.isDead() && person.getHuman().getAge() < 15 && (person.getMother() == null || person.getMother().isDead())
                    && (person.getFather() == null || person.getFather().isDead())) {
                orphans.add(person);
            } else if (!person.isDead() && person.getHuman().getGender().equals("Female") && person.getHuman().getAge() >= 15
                    && person.getHuman().getPartner() != null && !(person.isIll() || person.getPartner().isIll())) {
                potentialMothers.add(person);
            }

        }
        // System.out.println("Orphans count "+orphans.size());
        for (WrapperHuman whChild : orphans) {
            // min + (int) (Math.random() * ((max - min)))
            int rand = randomHelper.nextIntFromTo(0, potentialMothers.size() - 1);
            // int rand = (int)getRandomFromTo(0, potentialMothers.size());
            WrapperHuman wNewMom = potentialMothers.get(rand);

            // Set WrapperHuman versions of new father and mother
            whChild.setMother(wNewMom);
            whChild.setFather(wrapperPeople.get(wNewMom.getHuman().getPartner().getID()));

            // Add orphan to new mother's children list
            Female mom = (Female) wNewMom.getHuman();
            ArrayList<Human> childrenLst = null;
            try {
                Field fld = mom.getClass().getSuperclass().getDeclaredField("children");
                fld.setAccessible(true);
                childrenLst = (ArrayList<Human>) fld.get(mom); // #BNW
                                                               // reflection
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
            childrenLst.add(whChild.getHuman());

            // Add orphan to new father's children list
            Male dad = (Male) wNewMom.getHuman().getPartner();
            childrenLst = null;
            try {
                Field fld = dad.getClass().getSuperclass().getDeclaredField("children");
                fld.setAccessible(true);
                childrenLst = (ArrayList<Human>) fld.get(dad); // #BNW
                                                               // reflection
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
            childrenLst.add(whChild.getHuman());

            // TODO: Move child to mother's vicinity
            ContinuousSpace<Object> childSpace = null;
            Grid<Object> childGrid = null;
            HumanStyle childDisplay = null;
            HumanStyle momDisplay = null;
            Human child = whChild.getHuman();
            double sd = (Double) RunEnvironment.getInstance().getParameters().getValue("sd");

            try {
                Field fldSpace = child.getClass().getSuperclass().getDeclaredField("space");
                Field fldGrid = child.getClass().getSuperclass().getDeclaredField("grid");
                Field fldChildDis = child.getClass().getSuperclass().getDeclaredField("display");
                Field fldMomDis = mom.getClass().getSuperclass().getDeclaredField("display");

                fldSpace.setAccessible(true);
                fldGrid.setAccessible(true);
                fldChildDis.setAccessible(true);
                fldMomDis.setAccessible(true);
                childSpace = (ContinuousSpace<Object>) fldSpace.get(child); // #BNW
                                                                            // reflection
                childGrid = (Grid<Object>) fldGrid.get(child);
                childDisplay = (HumanStyle) fldChildDis.get(child);
                momDisplay = (HumanStyle) fldMomDis.get(mom);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }

            double yLocation = whChild.isYAxisAge() ? 0 : // getRandomFromTo(mom.getY()
                                                          // - sd * 2,
                                                          // mom.getY() + sd *
                                                          // 2);
                    randomHelper.nextDoubleFromTo(mom.getY() - sd * 2, mom.getY() + sd * 2);
            childSpace.moveTo(childDisplay, randomHelper.nextDoubleFromTo(mom.getX() - sd * 2, mom.getX() + sd * 2), yLocation);
            childGrid.moveTo(childDisplay, (int) mom.getX(), (int) mom.getY());

            mom.parity = mom.parity + 1;
        }

    }

    static int RNc = 5; // In relationship with no children
    static int RU15c = 6; // In relationship with under 15 children
    static int RO15c = 7; // In relationship with over 15 children
    static int RUO15c = 8; // In relationship with both under and over 15
                           // children
    static int SNc = 1; // Single adults (Over 15 persons)
    static int SU15c = 2; // Single with U15 children
    static int SO15c = 3; // Single with O15 children
    static int SUO15c = 4; // Single with both U15 and O15 children
    static int Chld = 0; // Children (under 15 persons)

    public Map getIndividualRelationshipNoAge(Context<Object> context) {
        Map<String, Map<String, Integer>> IndType = new LinkedHashMap<String, Map<String, Integer>>(9);
        Map<String, Integer> sexmap = new LinkedHashMap<String, Integer>(2);
        saveStates(context);
        String relStatus = null;

        sexmap.put("Male", 0);
        sexmap.put("Female", 0);
        IndType.put("Chld", new LinkedHashMap<String, Integer>(sexmap));
        IndType.put("SNc", new LinkedHashMap<String, Integer>(sexmap));
        IndType.put("SU15c", new LinkedHashMap<String, Integer>(sexmap));
        IndType.put("SO15c", new LinkedHashMap<String, Integer>(sexmap));
        IndType.put("SUO15c", new LinkedHashMap<String, Integer>(sexmap));
        IndType.put("RNc", new LinkedHashMap<String, Integer>(sexmap));
        IndType.put("RU15c", new LinkedHashMap<String, Integer>(sexmap));
        IndType.put("RO15c", new LinkedHashMap<String, Integer>(sexmap));
        IndType.put("RUO15c", new LinkedHashMap<String, Integer>(sexmap));

        ArrayList<String> relAl = new ArrayList<String>(IndType.keySet());
        int yearOfDeath = 0, pYearOfDeath = 0;
        int pplCount = 0, indvCount = 0, livingWithPartner = 0, livingWithPartner2 = 0;
        Human h = null, p = null;
        for (Object obj : context) {
            if (obj instanceof Human) {
                h = (Human) obj;
                try {
                    Field fld = h.getClass().getSuperclass().getDeclaredField("yearOfDeath");
                    fld.setAccessible(true);
                    yearOfDeath = fld.getInt(h);
                    if (yearOfDeath == 0) {
                        pplCount++;

                        p = h.getPartner();
                        if (p == null) {
                            continue;
                        }
                        Field fld2 = p.getClass().getSuperclass().getDeclaredField("yearOfDeath");
                        fld2.setAccessible(true);
                        pYearOfDeath = fld2.getInt(p);
                        if (pYearOfDeath == 0) {
                            livingWithPartner++;
                        }
                    }
                } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                    ex.printStackTrace();
                }
            }
        }

        for (Number id : wrapperPeople.keySet()) {

            WrapperHuman wh = wrapperPeople.get(id);
            if (wh.isDead() == true) {
                continue;
            }
            indvCount++;
            if (wh.getHuman().getAge() > 15) {

                if (wh.getHuman().getPartner() == null || wh.getPartner().isDead()) {
                    if (wh.getChildren().isEmpty()) {
                        relStatus = relAl.get(SNc);
                    }

                    boolean hasU15 = false, hasO15 = false;
                    for (WrapperHuman chld : wh.getChildren()) {
                        if (chld.getHuman().getAge() <= 15) {
                            hasU15 = true;
                        } else {
                            hasO15 = true;
                        }
                    }
                    if (hasU15 & hasO15) {
                        relStatus = relAl.get(SUO15c);
                    } else if (hasO15) {
                        relStatus = relAl.get(SO15c);
                    } else if (hasU15) {
                        relStatus = relAl.get(SU15c);
                    }

                } else {
                    livingWithPartner2++;
                    if (wh.getChildren().isEmpty()) {
                        relStatus = relAl.get(RNc);
                    }

                    boolean hasU15 = false, hasO15 = false;
                    for (WrapperHuman chld : wh.getChildren()) {
                        if (chld.getHuman().getAge() <= 15) {
                            hasU15 = true;
                        } else {
                            hasO15 = true;
                        }
                    }
                    if (hasU15 & hasO15) {
                        relStatus = relAl.get(RUO15c);
                    } else if (hasO15) {
                        relStatus = relAl.get(RO15c);
                    } else if (hasU15) {
                        relStatus = relAl.get(RU15c);
                    }
                }
            } else {
                relStatus = relAl.get(Chld);
            }

            String gender = "";
            if (wh.getHuman() instanceof Female) {
                gender = "Female";
            } else {
                gender = "Male";
            }

            int count = IndType.get(relStatus).get(gender);
            IndType.get(relStatus).put(gender, count + 1);

            // ageMap = IndType.get(maritalStatus).get(gender);

        }
        return IndType;

    }

    /**
     * Categorises individuals according to their family status <br />
     * static int RNc = 1; In relationship with no children <br />
     * static int RU15c = 2; In relationship with under 15 children <br />
     * static int RO15c = 3; In relationship with over 15 children <br />
     * static int RUO15c = 4; In relationship with both under and over 15 children <br />
     * static int SA = 5; Single adults (Over 15 persons) <br />
     * static int SU15c = 6; Single with U15 children <br />
     * static int SO15c = 7; Single with O15 children <br />
     * static int SUO15c = 8; Single with both U15 and O15 children <br />
     * static int Chld = 9; Children (under 15 persons)
     * 
     * @param context
     */
    public Map<String, Map<String, Map<String, Integer>>> getIndividualData_Sex_MaritalParentRel_4yrGapAge(Context<Object> context) {
        int SNc = 0; // Single adults (Over 15 persons)
        int SU15c = 1; // Single with U15 children
        int SO15c = 2; // Single with O15 children
        int SUO15c = 3; // Single with both U15 and O15 children
        int RNc = 4; // In relationship with no children
        int RU15c = 5; // In relationship with under 15 children
        int RO15c = 6; // In relationship with over 15 children
        int RUO15c = 7; // In relationship with both under and over 15
                        // children

        // This Map records number of agents and their gender
        Map<String, Integer> ageMap = new LinkedHashMap<>(26);
        ageMap.put("0-3", 0);
        ageMap.put("4-7", 0);
        ageMap.put("8-11", 0);
        ageMap.put("12-15", 0);
        ageMap.put("16-19", 0);
        ageMap.put("20-23", 0);
        ageMap.put("24-27", 0);
        ageMap.put("28-31", 0);
        ageMap.put("32-35", 0);
        ageMap.put("36-39", 0);
        ageMap.put("40-43", 0);
        ageMap.put("44-47", 0);
        ageMap.put("48-51", 0);
        ageMap.put("52-55", 0);
        ageMap.put("56-59", 0);
        ageMap.put("60-63", 0);
        ageMap.put("64-67", 0);
        ageMap.put("68-71", 0);
        ageMap.put("72-75", 0);
        ageMap.put("76-79", 0);
        ageMap.put("80-83", 0);
        ageMap.put("84-87", 0);
        ageMap.put("88-91", 0);
        ageMap.put("92-95", 0);
        ageMap.put("96-99", 0);
        ageMap.put("100++", 0);
        ArrayList<String> ageAl = new ArrayList<String>(ageMap.keySet());

        Map<String, Map<String, Integer>> relMap1 = new LinkedHashMap<>();
        relMap1.put("SNc", new LinkedHashMap<String, Integer>(ageMap));
        relMap1.put("SU15c", new LinkedHashMap<String, Integer>(ageMap));
        relMap1.put("SO15c", new LinkedHashMap<String, Integer>(ageMap));
        relMap1.put("SUO15c", new LinkedHashMap<String, Integer>(ageMap));
        relMap1.put("RNc", new LinkedHashMap<String, Integer>(ageMap));
        relMap1.put("RU15c", new LinkedHashMap<String, Integer>(ageMap));
        relMap1.put("RO15c", new LinkedHashMap<String, Integer>(ageMap));
        relMap1.put("RUO15c", new LinkedHashMap<String, Integer>(ageMap));

        Map<String, Map<String, Integer>> relMap2 = new LinkedHashMap<>();
        relMap2.put("SNc", new LinkedHashMap<String, Integer>(ageMap));
        relMap2.put("SU15c", new LinkedHashMap<String, Integer>(ageMap));
        relMap2.put("SO15c", new LinkedHashMap<String, Integer>(ageMap));
        relMap2.put("SUO15c", new LinkedHashMap<String, Integer>(ageMap));
        relMap2.put("RNc", new LinkedHashMap<String, Integer>(ageMap));
        relMap2.put("RU15c", new LinkedHashMap<String, Integer>(ageMap));
        relMap2.put("RO15c", new LinkedHashMap<String, Integer>(ageMap));
        relMap2.put("RUO15c", new LinkedHashMap<String, Integer>(ageMap));

        ArrayList<String> relAl = new ArrayList<String>(relMap1.keySet());

        // Map<Gender,Map<RelState,Map <AgeBracket,AgentCount>>>
        Map<String, Map<String, Map<String, Integer>>> IndType = new LinkedHashMap<>(2);
        IndType.put("Male", new LinkedHashMap<String, Map<String, Integer>>(relMap1));
        IndType.put("Female", new LinkedHashMap<String, Map<String, Integer>>(relMap2));

        // saveStates(context);
        String relStatus = null;
        String ageCat = "";
        int indcount = 0;
        for (Number id : wrapperPeople.keySet()) {
            WrapperHuman wh = wrapperPeople.get(id);
            if (wh.isDead() == true) {
                continue;
            }
            indcount++;
            if (wh.getHuman().getPartner() == null || wh.getPartner().isDead()) {
                if (wh.getChildren().isEmpty()) {
                    relStatus = relAl.get(SNc);

                }
                boolean hasU15 = false, hasO15 = false;
                for (WrapperHuman chld : wh.getChildren()) {
                    if (chld.getHuman().getAge() <= 15) {
                        hasU15 = true;
                    } else {
                        hasO15 = true;
                    }
                }
                if (hasU15 & hasO15) {
                    relStatus = relAl.get(SUO15c);
                } else if (hasO15) {
                    relStatus = relAl.get(SO15c);
                } else if (hasU15) {
                    relStatus = relAl.get(SU15c);
                }

            } else {
                if (wh.getChildren().isEmpty()) {
                    relStatus = relAl.get(RNc);
                }

                boolean hasU15 = false, hasO15 = false;
                for (WrapperHuman chld : wh.getChildren()) {
                    if (chld.getHuman().getAge() <= 15) {
                        hasU15 = true;
                    } else {
                        hasO15 = true;
                    }
                }
                if (hasU15 & hasO15) {
                    relStatus = relAl.get(RUO15c);
                } else if (hasO15) {
                    relStatus = relAl.get(RO15c);
                } else if (hasU15) {
                    relStatus = relAl.get(RU15c);
                }
            }

            int age = wh.getHuman().getAge();
            ageCat = ageAl.get(age / 4);

            /*
             * if (age < 16) { ageCat = "0-15"; } else if (age < 26) { ageCat = "16-25"; } else if (age < 36) { ageCat = "26-35"; } else if (age
             * < 46) { ageCat = "36-45"; } else if (age < 56) { ageCat = "46-55"; } else if (age < 66) { ageCat = "56-65"; } else if (age < 76) {
             * ageCat = "66-75"; } else if (age < 86) { ageCat = "76-85"; } else if (age < 96) { ageCat = "86-95"; } else { ageCat = "96++"; }
             */
            String gender = "";
            if (wh.getHuman() instanceof Female) {
                gender = "Female";
            } else {
                gender = "Male";
            }

            if (!IndType.get(gender).containsKey(relStatus)) {
                IndType.get(gender).get(relStatus).put(ageCat, 1);
            } else {
                int count = IndType.get(gender).get(relStatus).get(ageCat);
                IndType.get(gender).get(relStatus).put(ageCat, count + 1);
            }
            // ageMap = IndType.get(maritalStatus).get(gender);

        }
        return IndType;
    }

    public Map<String, Map<String, Map<String, Integer>>> getIndividualData_Sex_MaritalRel_4yrGapAge(Context<Object> context) {

        // This Map records number of agents and their gender

        Map<String, Integer> ageMap = new LinkedHashMap<>();
        ageMap.put("0-3", 0);
        ageMap.put("4-7", 0);
        ageMap.put("8-11", 0);
        ageMap.put("12-15", 0);
        ageMap.put("16-19", 0);
        ageMap.put("20-23", 0);
        ageMap.put("24-27", 0);
        ageMap.put("28-31", 0);
        ageMap.put("32-35", 0);
        ageMap.put("36-39", 0);
        ageMap.put("40-43", 0);
        ageMap.put("44-47", 0);
        ageMap.put("48-51", 0);
        ageMap.put("52-55", 0);
        ageMap.put("56-59", 0);
        ageMap.put("60-63", 0);
        ageMap.put("64-67", 0);
        ageMap.put("68-71", 0);
        ageMap.put("72-75", 0);
        ageMap.put("76-79", 0);
        ageMap.put("80-83", 0);
        ageMap.put("84-87", 0);
        ageMap.put("88-91", 0);
        ageMap.put("92-95", 0);
        ageMap.put("96-99", 0);
        ageMap.put("100++", 0);
        ArrayList<String> ageAl = new ArrayList<String>(ageMap.keySet());

        Map<String, Map<String, Integer>> relMap1 = new LinkedHashMap<>();
        relMap1.put("Single", new LinkedHashMap<String, Integer>(ageMap));
        relMap1.put("Married", new LinkedHashMap<String, Integer>(ageMap));

        Map<String, Map<String, Integer>> relMap2 = new LinkedHashMap<>();
        relMap2.put("Single", new LinkedHashMap<String, Integer>(ageMap));
        relMap2.put("Married", new LinkedHashMap<String, Integer>(ageMap));

        Map<String, Map<String, Map<String, Integer>>> IndType = new LinkedHashMap<>(9);
        IndType.put("Male", new LinkedHashMap<String, Map<String, Integer>>(relMap1));
        IndType.put("Female", new LinkedHashMap<String, Map<String, Integer>>(relMap2));

        saveStates(context);
        String relStatus = null;
        String ageCat = "";
        for (Number id : wrapperPeople.keySet()) {
            WrapperHuman wh = wrapperPeople.get(id);
            if (wh.isDead() == true) {
                continue;
            }

            if (wh.getHuman().getPartner() == null || wh.getPartner().isDead()) {
                relStatus = "Single";
            } else {
                relStatus = "Married";
                if (wh.getHuman().getAge() <= 15) {
                    System.out.println("Error");
                }
            }

            int age = wh.getHuman().getAge();
            ageCat = ageAl.get(age / 4);

            String gender = "";
            if (wh.getHuman() instanceof Female) {
                gender = "Female";
            } else {
                gender = "Male";
            }

            int count = IndType.get(gender).get(relStatus).get(ageCat);
            IndType.get(gender).get(relStatus).put(ageCat, count + 1);

        }
        return IndType;
    }

    public Map<String, Map<String, Map<String, Integer>>> getIndividualData_Sex_ParentalMaritalRel_LatchAge(Context<Object> context) {
        int SNc = 0; // Single adults (Over 15 persons)
        int SU15c = 1; // Single with U15 children
        int SO15c = 2; // Single with O15 children
        int SUO15c = 3; // Single with both U15 and O15 children
        int RNc = 4; // In relationship with no children
        int RU15c = 5; // In relationship with under 15 children
        int RO15c = 6; // In relationship with over 15 children
        int RUO15c = 7; // In relationship with both under and over 15
                        // children

        Map<String, Map<String, Map<String, Integer>>> IndType = new LinkedHashMap<>(9);
        // Map <IndividualType, Map <Gender,Map <AgeBracket,AgentCount> > >
        // This Map records number of agents and their gender

        int indCount = 0;
        Map<String, Integer> ageMap = new LinkedHashMap<>();
        ageMap.put("0-14", 0);
        ageMap.put("15-24", 0);
        ageMap.put("25-39", 0);
        ageMap.put("40-54", 0);
        ageMap.put("55-69", 0);
        ageMap.put("70-84", 0);
        ageMap.put("85-99", 0);
        ageMap.put("100++", 0);
        ArrayList<String> ageAl = new ArrayList<String>(ageMap.keySet());

        Map<String, Map<String, Integer>> relMap1 = new LinkedHashMap<>();
        relMap1.put("SNc", new LinkedHashMap<>(ageMap));
        relMap1.put("SU15c", new LinkedHashMap<>(ageMap));
        relMap1.put("SO15c", new LinkedHashMap<>(ageMap));
        relMap1.put("SUO15c", new LinkedHashMap<>(ageMap));
        relMap1.put("RNc", new LinkedHashMap<>(ageMap));
        relMap1.put("RU15c", new LinkedHashMap<>(ageMap));
        relMap1.put("RO15c", new LinkedHashMap<>(ageMap));
        relMap1.put("RUO15c", new LinkedHashMap<>(ageMap));

        Map<String, Map<String, Integer>> relMap2 = new LinkedHashMap<>();
        relMap2.put("SNc", new LinkedHashMap<>(ageMap));
        relMap2.put("SU15c", new LinkedHashMap<>(ageMap));
        relMap2.put("SO15c", new LinkedHashMap<>(ageMap));
        relMap2.put("SUO15c", new LinkedHashMap<>(ageMap));
        relMap2.put("RNc", new LinkedHashMap<>(ageMap));
        relMap2.put("RU15c", new LinkedHashMap<>(ageMap));
        relMap2.put("RO15c", new LinkedHashMap<>(ageMap));
        relMap2.put("RUO15c", new LinkedHashMap<>(ageMap));

        ArrayList<String> relAl = new ArrayList<String>(relMap1.keySet());

        IndType.put("Male", new LinkedHashMap<>(relMap1));
        IndType.put("Female", new LinkedHashMap<>(relMap2));

        saveStates(context);
        String relStatus = null;
        String ageCat = "";

        for (Number id : wrapperPeople.keySet()) {
            WrapperHuman wh = wrapperPeople.get(id);
            if (wh.isDead() == true) {
                continue;
            }

            if (wh.getHuman().getPartner() == null || wh.getPartner().isDead()) {
                if (wh.getChildren().isEmpty()) {
                    relStatus = relAl.get(SNc);

                }
                boolean hasU15 = false, hasO15 = false;
                for (WrapperHuman chld : wh.getChildren()) {
                    if (chld.getHuman().getAge() <= 15) {
                        hasU15 = true;
                    } else {
                        hasO15 = true;
                    }
                }
                if (hasU15 & hasO15) {
                    relStatus = relAl.get(SUO15c);
                } else if (hasO15) {
                    relStatus = relAl.get(SO15c);
                } else if (hasU15) {
                    relStatus = relAl.get(SU15c);
                }

            } else {
                if (wh.getChildren().isEmpty()) {
                    relStatus = relAl.get(RNc);
                }

                boolean hasU15 = false, hasO15 = false;
                for (WrapperHuman chld : wh.getChildren()) {
                    if (chld.getHuman().getAge() <= 15) {
                        hasU15 = true;
                    } else {
                        hasO15 = true;
                    }
                }
                if (hasU15 & hasO15) {
                    relStatus = relAl.get(RUO15c);
                } else if (hasO15) {
                    relStatus = relAl.get(RO15c);
                } else if (hasU15) {
                    relStatus = relAl.get(RU15c);
                }
            }

            int age = wh.getHuman().getAge();
            if (age <= 14) {
                ageCat = ageAl.get(0);
            } else if (15 <= age && age <= 24) {
                ageCat = ageAl.get(1);
            } else {
                ageCat = ageAl.get(age >= 100 ? 7 : ((age - 24) / 15) + 2);
            }

            /*
             * if (age < 16) { ageCat = "0-15"; } else if (age < 26) { ageCat = "16-25"; } else if (age < 36) { ageCat = "26-35"; } else if (age
             * < 46) { ageCat = "36-45"; } else if (age < 56) { ageCat = "46-55"; } else if (age < 66) { ageCat = "56-65"; } else if (age < 76) {
             * ageCat = "66-75"; } else if (age < 86) { ageCat = "76-85"; } else if (age < 96) { ageCat = "86-95"; } else { ageCat = "96++"; }
             */
            String gender = "";
            if (wh.getHuman() instanceof Female) {
                gender = "Female";
            } else {
                gender = "Male";
            }

            if (!IndType.get(gender).containsKey(relStatus)) {
                IndType.get(gender).get(relStatus).put(ageCat, 1);
            } else {
                int count = IndType.get(gender).get(relStatus).get(ageCat);
                IndType.get(gender).get(relStatus).put(ageCat, count + 1);
            }
            // ageMap = IndType.get(maritalStatus).get(gender);

        }
        return IndType;
    }

    public Map<String, Map<String, Map<String, Integer>>> getIndividualData_Sex_MaritalRel_LatchAge(Context<Object> context) {
        Map<String, Map<String, Map<String, Integer>>> IndType = new LinkedHashMap<>(9);
        // Map <IndividualType, Map <Gender,Map <AgeBracket,AgentCount> > >
        // This Map records number of agents and their gender

        Map<String, Integer> ageMap = new LinkedHashMap<>();
        ageMap.put("0-14", 0);
        ageMap.put("15-24", 0);
        ageMap.put("25-39", 0);
        ageMap.put("40-54", 0);
        ageMap.put("55-69", 0);
        ageMap.put("70-84", 0);
        ageMap.put("85-99", 0);
        ageMap.put("100++", 0);
        ArrayList<String> ageAl = new ArrayList<String>(ageMap.keySet());

        Map<String, Map<String, Integer>> relMap1 = new LinkedHashMap<>();
        relMap1.put("Single", new LinkedHashMap<String, Integer>(ageMap));
        relMap1.put("Married", new LinkedHashMap<String, Integer>(ageMap));

        Map<String, Map<String, Integer>> relMap2 = new LinkedHashMap<>();
        relMap2.put("Single", new LinkedHashMap<String, Integer>(ageMap));
        relMap2.put("Married", new LinkedHashMap<String, Integer>(ageMap));

        IndType.put("Male", relMap1);
        IndType.put("Female", relMap2);

        saveStates(context);
        String relStatus = null;
        String ageCat = "";
        for (Number id : wrapperPeople.keySet()) {
            WrapperHuman wh = wrapperPeople.get(id);
            if (wh.isDead() == true) {
                continue;
            }

            if (wh.getHuman().getPartner() == null || wh.getPartner().isDead()) {
                relStatus = "Single";
            } else {
                relStatus = "Married";
                if (wh.getHuman().getAge() <= 15) {
                    System.out.println("Error");
                }
            }

            int age = wh.getHuman().getAge();
            if (age <= 14) {
                ageCat = ageAl.get(0);
            } else if (15 <= age && age <= 24) {
                ageCat = ageAl.get(1);
            } else {
                ageCat = ageAl.get(age >= 100 ? 7 : ((age - 24) / 15) + 2);
            }

            String gender = "";
            if (wh.getHuman() instanceof Female) {
                gender = "Female";
            } else {
                gender = "Male";
            }

            int count = IndType.get(gender).get(relStatus).get(ageCat);
            IndType.get(gender).get(relStatus).put(ageCat, count + 1);
        }
        return IndType;
    }

    public Map AgentsByAge(Context<Object> context) {
        int indCount = 0;
        Map ageMap = new LinkedHashMap<String, Integer>();
        for (int i = 0; i <= 100; i++) {
            ageMap.put(i, 0);
        }

        saveStates(context);
        String relStatus = null;
        String ageCat = "";
        String health;
        for (Number id : wrapperPeople.keySet()) {
            WrapperHuman wh = wrapperPeople.get(id);
            if (wh.isDead() == true) {
                continue;
            }
            int count = (int) ageMap.get(wh.getHuman().getAge());
            ageMap.put(wh.getHuman().getAge(), ++count);
        }
        return ageMap;
    }

    /**
     * Random value [min.max) i.e. including min, excluding max
     * 
     * @param min
     * @param max
     * @return
     */
    double getRandomFromTo(double min, double max) {
        return min + (Math.random() * ((max - min)));
    }

    public Map<Integer, Integer> getAgentIDMap() {
        HashMap<Integer, Integer> hmp = new HashMap<>();
        for (Map<String, Object> mp : agentStates) {
            hmp.put((Integer) mp.get("lid"), (Integer) mp.get("gid"));
        }
        return hmp;
    }

}