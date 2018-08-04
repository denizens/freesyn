package bnw.abm.intg.algov2.population;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.tuple.Pair;

import bnw.abm.intg.algov2.framework.models.Group;
import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.IPFPTable;
import bnw.abm.intg.algov2.framework.models.Population;
import bnw.abm.intg.util.Log;

abstract public class PopulationBuilder {

    private List<GroupTemplate> groupTemplates;
    private long popSize;
    /**
     * New Population instance
     */
    protected Population population;

    /**
     * Creates a PopulationBuilder instance
     * 
     * @param populationDistribution
     *            IPFPTable instance with target agent distributions (Output of IPFPTableBuilder.build() method)
     * @param groupTemplates
     *            List of possible GroupTemplates (Generally the output of GroupTemplatesBuilder.build() method
     * @param expectedPopulationSize
     *            Size of the expected population (Actual population may be slightly higher than this).
     */
    public PopulationBuilder(IPFPTable populationDistribution, List<GroupTemplate> groupTemplates, long expectedPopulationSize) {
        this.groupTemplates = groupTemplates;
        this.popSize = expectedPopulationSize;
        this.population = new Population(populationDistribution);

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

            int ttlThreads = ((groupTemplates.size() % maxTemplatesInAThread) > 0) ? ((groupTemplates.size() / maxTemplatesInAThread) + 1)
                    : (groupTemplates.size() / maxTemplatesInAThread);
            int templatesPerThread = groupTemplates.size() / ttlThreads;
            int modTemplates = groupTemplates.size() % maxTemplatesInAThread;

            int toIndex = 0, fromIndex = 0;
            for (int i = 0; i < ttlThreads; i++) {
                fromIndex = toIndex;
                toIndex += templatesPerThread;
                toIndex += modTemplates-- > 0 ? 1 : 0;
                toIndex = (groupTemplates.size() < toIndex) ? groupTemplates.size() : toIndex;
                List<GroupTemplate> templatesSubList = groupTemplates.subList(fromIndex, toIndex);

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
}
