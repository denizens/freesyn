package bnw.abm.intg.sync.wdwrapper;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import WeddingGrid.CSVHandler;
import WeddingGrid.StatisticCollector;
import bnw.abm.intg.sync.wdwrapper.reload.MaritalRel4yrAgeLoader;
import bnw.abm.intg.util.Log;
import ch.qos.logback.classic.Level;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunState;
import repast.simphony.parameter.Parameters;
import repast.simphony.parameter.ParametersParser;

public class RECMainClass {

    /*
     * public static void main(String[] args) { System.out.println("Started!"); String scenario[] = new
     * String[]{"/home/bhagya/workspace/jzombies/jzombies.rs"}; repast.simphony.runtime.RepastMain.main(scenario); } }
     */

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        Log.createLogger("Init", Level.INFO, "bnw.abm.intg.sync.wdwrapper.log");
        Log.info("Initialising Wedding Doughnut set-up");

        String scenarioFileLoc = args[0];
        double endStep = Integer.parseInt(args[1]); // Last step of the run
        int seed = Integer.parseInt(args[2]); // Seed for the 1st run
        int seedIncrements = Integer.parseInt(args[3]); // Number of seed
                                                        // increments

        File scenarioFile = new File(scenarioFileLoc); // the scenario
                                                       // dir
        System.out.println(scenarioFile.toString());
        File parametersFile = new File(scenarioFile.getPath() + "/parameters.xml");
        ParametersParser parameterParser;
        Parameters params = null;

        try {
            parameterParser = new ParametersParser(parametersFile);
            params = parameterParser.getParameters();

        } catch (ParserConfigurationException | SAXException | IOException e1) {
            e1.printStackTrace();
        }

        // Pushing messy WeddingGrid output logs to one directory. easy to delete
        String weddingGridLogs = "weddinggridlogs/";
        if (!Files.exists(Paths.get(weddingGridLogs))) {
            try {
                Files.createDirectory(Paths.get(weddingGridLogs));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        WeddingGrid.StatisticCollector.outputPushLocation = weddingGridLogs;

        // Output location for my survey files
        String outputLocation = System.getenv("ANONDATA_HOME") + File.separator + "wdll/wd-survey/";
        String mergedFileLocation = System.getenv("ANONDATA_HOME") + File.separator + "wdll/grouped/";
        String reinitFileLocation = System.getenv("ANONDATA_HOME") + File.separator + "wdll/wd-reinit/";
        if (!Files.exists(Paths.get(outputLocation))) {
            try {
                Files.createDirectory(Paths.get(outputLocation));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        RECRunner runner = new RECRunner();
        // Run the sim a few times to check for cleanup and init issues.
        for (int i = seed; i <= seed + seedIncrements; i++) {
            try {
                initSimulationRun(i, runner, params, scenarioFile);
                SimulationState state = new SimulationState(RunState.getInstance().getMasterContext());
                executeSteps(runner, 51, state);
                Log.info("Recoding population data after burn-in");
                recordData(state, outputLocation, i);
                cleanSimulationState(state);
                MaritalRel4yrAgeLoader populationLoader = new MaritalRel4yrAgeLoader(RunState.getInstance().getMasterContext());
                applyMergedPopulation(mergedFileLocation, populationLoader, i, state);
                executeSteps(runner, 61, state);
                Log.info("Recoding population data after end step");
                recordData(state, reinitFileLocation, seed);
                finaliseSimulationRun(runner);
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

        }

    }

    public static void initSimulationRun(int seed, RECRunner runner, Parameters params, File scenarioFile) throws Exception {
        Log.info("Initialising simulation run " + seed);
        params.setValue("randomSeed", seed);
        runner.load(scenarioFile, params); // load the repast scenario
        runner.runInitialize(); // initialize the run
        StatisticCollector.runCount = seed;

    }

    @SuppressWarnings("unchecked")
    public static void executeSteps(RECRunner runner, double endStep, SimulationState state) {

        while (RunEnvironment.getInstance().getCurrentSchedule().getTickCount() <= endStep + 0.5) {
            synchronized (runner) {
                runner.step();
                // System.out.println("Seed"+RunEnvironment.getInstance().getParameters().getValue("randomSeed"));
            }
            state.saveStates(RunState.getInstance().getMasterContext());
        }

    }

    public static void recordData(SimulationState state, String outputLocation, int seed) {
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Map<String, Integer>>> MPRel_4yrAge = state
                .getIndividualData_Sex_MaritalParentRel_4yrGapAge(RunState.getInstance().getMasterContext());
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Map<String, Integer>>> MRel_4yrAge = state
                .getIndividualData_Sex_MaritalRel_4yrGapAge(RunState.getInstance().getMasterContext());
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Map<String, Integer>>> MRel_LatchAge = state
                .getIndividualData_Sex_MaritalRel_LatchAge(RunState.getInstance().getMasterContext());
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Map<String, Integer>>> MPRel_LatchAge = state
                .getIndividualData_Sex_ParentalMaritalRel_LatchAge(RunState.getInstance().getMasterContext());

        write2Csv(outputLocation + "/Sex_MaritalRel_4yrGapAge_" + seed + ".csv", MRel_4yrAge);
        write2Csv(outputLocation + "/Sex_MaritalParentalRel_4yrGapAge_" + seed + ".csv", MPRel_4yrAge);
        write2Csv(outputLocation + "/Sex_MaritalRel_LatchAge_" + seed + ".csv", MRel_LatchAge);
        write2Csv(outputLocation + "/Sex_MaritalParentalRel_LatchAge_" + seed + ".csv", MPRel_LatchAge);
        Log.info("Data saved to: "+ outputLocation);
    }

    @SuppressWarnings("unchecked")
    public static void cleanSimulationState(SimulationState state)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Log.info("Deleting current agent population");
        Context<Object> context = RunState.getInstance().getMasterContext();
        state.deleteAllAgents(context);
        state.resetSimulationCountersTo0(context);
    }

    @SuppressWarnings("unchecked")
    public static void applyMergedPopulation(String mergedFileLocation, MaritalRel4yrAgeLoader populationLoader, int correspondingPopulationSeed,
            SimulationState state) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
        Log.info("Loading merged population");
        Path agentsFile = Paths.get(mergedFileLocation + File.separator + "MaritalRel_4yrGapAge_sFALSE" + File.separator
                + correspondingPopulationSeed + File.separator + "Agents.csv");
        populationLoader.reloadMergedPopulation(agentsFile);
        state.saveStates(RunState.getInstance().getMasterContext());
    }

    public static void finaliseSimulationRun(RECRunner runner) {
        runner.stop(); // execute any actions scheduled at run end
        runner.cleanUpRun();
        Log.info("Run complete");
    }

    public static void write2Csv(String filePath, Map<String, Map<String, Map<String, Integer>>> data) {
        try {
            CSVHandler myCsv = new CSVHandler(filePath/* "survey/RelationshipStatus_long_" + i + ".csv" */);
            myCsv.openFile(false);
            myCsv.append(new Object[] { "Gender", "Rel_Status", "Age", "Agent_Count" });
            int agentCount = 0;
            for (Object relStatus : data.keySet()) {
                for (Object gender : data.get(relStatus).keySet()) {
                    for (Object age : data.get(relStatus).get(gender).keySet()) {
                        agentCount = data.get(relStatus).get(gender).get(age);
                        myCsv.append(new Object[] { relStatus, gender, age, agentCount });
                    }
                }
            }

            myCsv.closeFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}