package bnw.abm.intg.sync.wdwrapper;
import java.io.File;
import WeddingGrid.*;
import repast.simphony.batch.BatchScenarioLoader;
import repast.simphony.context.Context;
import repast.simphony.engine.controller.Controller;
import repast.simphony.engine.controller.DefaultController;
import repast.simphony.engine.environment.AbstractRunner;
import repast.simphony.engine.environment.ControllerRegistry;
import repast.simphony.engine.environment.DefaultRunEnvironmentBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunEnvironmentBuilder;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.Schedule;
import repast.simphony.parameter.Parameters;
import repast.simphony.parameter.SweeperProducer;
import repast.simphony.scenario.ScenarioLoader;
import simphony.util.messages.MessageCenter;

public class RECRunner extends AbstractRunner {

	private static MessageCenter msgCenter = MessageCenter.getMessageCenter(RECRunner.class);

	private RunEnvironmentBuilder runEnvironmentBuilder;
	protected Controller controller;
	protected boolean pause = false;
	protected Object monitor = new Object();
	protected SweeperProducer producer;
	private ISchedule schedule;
	private Parameters params;

	public RECRunner() {
		runEnvironmentBuilder = new DefaultRunEnvironmentBuilder(this, false);
		controller = new DefaultController(runEnvironmentBuilder);
		controller.setScheduleRunner(this);
	}

	public void load(File scenarioDir, Parameters params) throws Exception{
		if (scenarioDir.exists()) {
			BatchScenarioLoader loader = new BatchScenarioLoader(scenarioDir);
			ControllerRegistry registry = loader.load(runEnvironmentBuilder);
			controller.setControllerRegistry(registry);
		} else {
			msgCenter.error("Scenario not found", new IllegalArgumentException(
					"Invalid scenario " + scenarioDir.getAbsolutePath()));
			return;
		}
		
		controller.batchInitialize();
		this.params = controller.runParameterSetters(params);
	}

	public void runInitialize(){
		
		/*DefaultParameters defaultParameters = new DefaultParameters();
		defaultParameters.addParameter("randomSeed", "randomSeed", Number.class, 1,true);
		controller.runInitialize(defaultParameters);*/
		controller.runInitialize(params);
		schedule = RunState.getInstance().getScheduleRegistry().getModelSchedule();

	}

	public void cleanUpRun(){
		controller.runCleanup();
	}
	public void cleanUpBatch(){
		controller.batchCleanup();
	}

	// returns the tick count of the next scheduled item
	public double getNextScheduledTime(){
		return ((Schedule)RunEnvironment.getInstance().getCurrentSchedule()).peekNextAction().getNextTime();
	}

	// returns the number of model actions on the schedule
	public int getModelActionCount(){
		return schedule.getModelActionCount();
	}

	// returns the number of non-model actions on the schedule
	public int getActionCount(){
		return schedule.getActionCount();
	}

	// Step the schedule
	public void step(){
		synchronized (schedule) {
			schedule.execute();
		}
		
		    
	}
	
	public Female findaMom()
	{
		Female mom = null;
		Context<Object> context = RunState.getInstance().getMasterContext();
		
		for (Object woman: context){
			if (woman instanceof Female && ((Female)woman).isMarried()){		
				mom = (Female)woman;
				return mom;
			}
		}
		return null;
	}

	// stop the schedule
	public void stop(){
		if ( schedule != null )
			schedule.executeEndActions();
	}

	public void setFinishing(boolean fin){
		schedule.setFinishing(fin);
	}

	public void execute(RunState toExecuteOn) {
		// required AbstractRunner stub.  We will control the
		//  schedule directly.
	}
}