package bnw.abm.intg.algov2.nonipfp.population;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.tuple.Pair;

import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.algov2.framework.models.Group;
import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.framework.models.Population;
import bnw.abm.intg.util.Log;

abstract public class NonIPFPPopulationBuilder {

    private Map<GroupType, List<GroupTemplate>> groupTemplates;
    private long popSize;

    /**
     * New Population instance
     */
    protected Population population;

    protected final Map<GroupType, Integer> targetGroupTypeCounts;
    protected final Map<ReferenceAgentType, Integer> targetAgentTypeCounts;

    /**
     * Creates a PopulationBuilder instance
     * 
     * @param targetGroupTypeCounts
     *            Target GroupType distribution
     * @param targetAgentTypeCounts
     *            Target AgentType distribution
     * @param groupTemplates
     *            List of possible GroupTemplates (Generally the output of GroupTemplatesBuilder.build() method
     * @param expectedPopulationSize
     *            Size of the expected population (Actual population may be slightly higher than this).
     */
    public NonIPFPPopulationBuilder(Map<GroupType, Integer> targetGroupTypeCounts, Map<ReferenceAgentType, Integer> targetAgentTypeCounts,
            Map<GroupType, List<GroupTemplate>> groupTemplates, long expectedPopulationSize) {
        this.groupTemplates = groupTemplates;
        this.popSize = expectedPopulationSize;
        this.population = new Population(targetGroupTypeCounts, targetAgentTypeCounts);
        this.targetGroupTypeCounts = targetGroupTypeCounts;
        this.targetAgentTypeCounts = targetAgentTypeCounts;

        // countAgentTypes(groupTemplates);
    }

    protected long getExptectedPopulationSize() {
        return popSize;
    }

    /**
     * Implement this method to provide error calculation function for selecting GroupTemplates. In an iterative process, this function is called
     * for each available GroupTemplate to find the error that would occur in the population if the GroupTemplate is added to the population.
     * GroupTemplate that gives least error is selected for creating a new Group in each iteration.
     * 
     * @param groupTemplate
     *            GroupTemplate of which error is calculated
     * @return Error if this GroupTemplate is selected for the next new Group
     */
    abstract protected double calculateError(GroupTemplate groupTemplate);

    abstract protected double calculateError(GroupType groupType);

    /**
     * Call this function to construct the population. Basic logic is: Calculate error for each available GroupTemplate and select the
     * GroupTemplate with least error. Follow this process until expected population size is reached.
     * 
     * @param threadPoolSize
     *            Number of threads to be used
     * @param maxTemplatesInAThread
     *            Max number of templates to be processed in one thread
     * @return instance of Population.
     * @throws InterruptedException
     *             if thread interrupted while waiting to least error group template calculation to finish
     * @throws ExecutionException
     *             if GroupTemplate with least error calculation throws an exception
     */
    public Population build(int threadPoolSize, int maxTemplatesInAThread) throws InterruptedException, ExecutionException {
        final ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize);
        final ExecutorCompletionService<Pair<GroupTemplate, Double>> completionService = new ExecutorCompletionService<>(pool);

        while (population.size() < popSize) {

            GroupType minErrorGroupType = minimumErrorGroupType();

            List<GroupTemplate> possibleGroupTemplates = groupTemplates.get(minErrorGroupType);

            int ttlThreads = ((possibleGroupTemplates.size() % maxTemplatesInAThread) > 0) ? ((possibleGroupTemplates.size() / maxTemplatesInAThread) + 1)
                    : (possibleGroupTemplates.size() / maxTemplatesInAThread);
            int templatesPerThread = possibleGroupTemplates.size() / ttlThreads;
            int modTemplates = possibleGroupTemplates.size() % maxTemplatesInAThread;

            int toIndex = 0, fromIndex = 0;
            for (int i = 0; i < ttlThreads; i++) {
                fromIndex = toIndex;
                toIndex += templatesPerThread;
                toIndex += modTemplates-- > 0 ? 1 : 0;
                toIndex = (possibleGroupTemplates.size() < toIndex) ? possibleGroupTemplates.size() : toIndex;
                List<GroupTemplate> templatesSubList = possibleGroupTemplates.subList(fromIndex, toIndex);

                completionService.submit(new Callable<Pair<GroupTemplate, Double>>() {

                    List<GroupTemplate> groupTemplatesSubList;

                    @Override
                    public Pair<GroupTemplate, Double> call() throws Exception {
                        double newError = 0, minimumError = Double.POSITIVE_INFINITY;
                        GroupTemplate minimumErrorTemplate = null;
                        for (GroupTemplate groupTemplate : groupTemplatesSubList) {
                            newError = calculateError(groupTemplate);
                            if (minimumError > newError) {
                                minimumErrorTemplate = groupTemplate;
                                minimumError = newError;
                            }
                        }
                        return Pair.of(minimumErrorTemplate, minimumError);
                    }

                    private Callable<Pair<GroupTemplate, Double>> init(List<GroupTemplate> groupTemplatesSubList) {
                        this.groupTemplatesSubList = groupTemplatesSubList;
                        return this;
                    }
                }.init(templatesSubList));
            }

            double minimumErrorOfAll = Double.POSITIVE_INFINITY;
            GroupTemplate minimumErrorTemplateOfAll = null;
            for (int i = 0; i < ttlThreads; i++) {
                Future<Pair<GroupTemplate, Double>> entry = completionService.take();
                if (minimumErrorOfAll > entry.get().getValue()) {
                    minimumErrorOfAll = entry.get().getValue();
                    minimumErrorTemplateOfAll = entry.get().getKey();
                }
            }

            population.addGroup(new Group(minimumErrorTemplateOfAll));
        }
        pool.shutdown();
        Log.info("Population construction complete!");
        return population;
    }

    private GroupType minimumErrorGroupType() {
        double minimumError = Double.POSITIVE_INFINITY;
        GroupType minimumErrorGroupType = null;
        for (GroupType groupType : targetGroupTypeCounts.keySet()) {
            double newError = calculateError(groupType);
            if (minimumError > newError) {
                minimumError = newError;
                minimumErrorGroupType = groupType;
            }
        }
        return minimumErrorGroupType;
    }
}
