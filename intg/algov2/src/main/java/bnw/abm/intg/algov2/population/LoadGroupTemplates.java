package bnw.abm.intg.algov2.population;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;

import bnw.abm.intg.algov2.framework.models.GroupTemplate;
import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.util.Log;

abstract public class LoadGroupTemplates {

    private int totalTemplatesRead = 0;
    private int uniqueTemplates = 0;

    /**
     * 
     * Reads in templates from multiples files sequentially
     * 
     * @param templates
     *            Map for holding loaded templates
     * @param filePaths
     *            Paths of the templates files
     * @param templateProcessingThreadPoolSize
     *            number of threads
     * @param maxTemplatesInAThread
     *            Max number of GroupTemplates to be processed in one thread
     * @throws IOException
     *             When reading from templates file
     * @throws ClassNotFoundException
     *             Class of GroupTemplates object cannot be found
     * @throws InterruptedException
     *             if the current thread was interrupted while waiting
     * @throws ExecutionException
     *             if the computation threw an exception
     */
    public void load(Map<GroupType, List<GroupTemplate>> templates, List<Path> filePaths, int templateProcessingThreadPoolSize,
            int maxTemplatesInAThread) throws IOException, ClassNotFoundException, InterruptedException, ExecutionException {
        for (Path filePath : filePaths) {
            this.load(templates, filePath, templateProcessingThreadPoolSize, maxTemplatesInAThread);
        }
    }

    /**
     * Reads in Templates from one file
     * 
     * @param templates
     *            Map for holding loaded templates
     * @param filePath
     *            Path of the templates file
     * @param threadPoolSize
     *            number of threads
     * @param maxTemplatesInAThread
     *            Max number of GroupTemplates to be processed in one thread
     * @throws IOException
     *             When reading from templates file
     * @throws ClassNotFoundException
     *             Class of GroupTemplates object cannot be found
     * @throws InterruptedException
     *             if the current thread was interrupted while waiting
     * @throws ExecutionException
     *             if the computation threw an exception
     */
    public void load(Map<GroupType, List<GroupTemplate>> templates, Path filePath, int threadPoolSize, int maxTemplatesInAThread)
            throws IOException, ClassNotFoundException, InterruptedException, ExecutionException {

        final ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize);
        final ExecutorCompletionService<Boolean> completionService = new ExecutorCompletionService<>(pool);

        try (ObjectInputStream objInStream = new ObjectInputStream(new LZMACompressorInputStream(new BufferedInputStream(
                Files.newInputStream(filePath))))) {
            List<GroupTemplate> templateObjetcsBatch = null;
            while (true) {
                Object inObject = objInStream.readObject();
                if (inObject instanceof String && inObject.equals("END")) {
                    break;
                } else if (inObject instanceof ArrayList) {
                    templateObjetcsBatch = (ArrayList<GroupTemplate>) inObject;
                    int uniqTemplatesInThisBatch = 0;
                    for (GroupTemplate newTemplate : templateObjetcsBatch) {
                        // GroupType instance in file is different from ones we currently have on jvm
                        newTemplate.setGroupType(GroupType.getInstance(newTemplate.getGroupType().getID()));
                        totalTemplatesRead++;
                        if (filter(newTemplate.getGroupType())) {
                            if (templates.containsKey(newTemplate.getGroupType())) {
                                if (!isDuplicateTemplate(newTemplate, templates.get(newTemplate.getGroupType()), completionService,
                                        maxTemplatesInAThread)) {
                                    templates.get(newTemplate.getGroupType()).add(newTemplate);
                                    ++uniqueTemplates;
                                    ++uniqTemplatesInThisBatch;
                                }
                            } else {
                                templates.put(newTemplate.getGroupType(), new ArrayList<>(Arrays.asList(newTemplate)));
                            }
                        }
                    }
                    Log.info("Newly added templates: " + uniqTemplatesInThisBatch + "/" + templateObjetcsBatch.size() + " total: "
                            + uniqueTemplates);
                }
            }
        }
        pool.shutdown();
        Log.info("Total templates: " + uniqueTemplates + "/" + totalTemplatesRead);
    }

    private boolean isDuplicateTemplate(GroupTemplate newTemplate, List<GroupTemplate> groupTemplates,
            CompletionService<Boolean> completionService, int maxTemplatesInAThread) throws InterruptedException, ExecutionException {

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
            completionService.submit(new DuplicateChecker().init(newTemplate, templatesSubList));
        }

        Future<Boolean> isDuplicate = null;
        for (int i = 0; i < ttlThreads; i++) {
            isDuplicate = completionService.take();
        }
        DuplicateChecker.duplicateFound = false;
        return isDuplicate.get();
    }

    public int totalTemplatesInInput() {
        return totalTemplatesRead;
    }

    public int totalUniqueTemplates() {
        return uniqueTemplates;
    }

    abstract public boolean filter(GroupType groupType);

}

class DuplicateChecker implements Callable<Boolean> {
    List<GroupTemplate> groupTemplatesSubList;
    GroupTemplate referenceTemplate;

    static Boolean duplicateFound = false;

    @Override
    public Boolean call() throws Exception {
        for (GroupTemplate groupTemplate : groupTemplatesSubList) {
            if (duplicateFound) {
                return duplicateFound;
            }

            if (this.referenceTemplate.equals(groupTemplate)) {
                synchronized (duplicateFound) {
                    duplicateFound = true;
                }
                return duplicateFound;
            }
        }
        return duplicateFound;
    }

    /**
     * Initialise the Caller for checking duplicates
     * 
     * @param referenceTemplate
     *            new template that will be added to templates list
     * @param groupTemplatesSubList
     *            sublist of templates that will be processed by this thread
     * @return True if new Template is a duplicate, False if no duplicates found in the specified list
     */
    Callable<Boolean> init(GroupTemplate referenceTemplate, List<GroupTemplate> groupTemplatesSubList) {
        this.groupTemplatesSubList = groupTemplatesSubList;
        this.referenceTemplate = referenceTemplate;
        return this;
    }
}