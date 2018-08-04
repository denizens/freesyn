package bnw.abm.intg.sync.wdwrapper.reload;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

import WeddingGrid.Builder;
import WeddingGrid.TheWorld;
import repast.simphony.context.Context;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

abstract public class PopulationLoader {

    final TheWorld theWorld;
    final ContinuousSpace<Object> space;
    final Grid<Object> grid;
    final Context<Object> context;

    public PopulationLoader(Context<Object> context) {
        this.context = context;
        Builder thisBuilder = null;
        // Find the right Context Builder
        for (Object obj : context) {
            if (obj instanceof Builder) {
                thisBuilder = ((Builder) obj);
            }
        }
        this.theWorld = thisBuilder.theWorld;
        this.space = thisBuilder.space;
        this.grid = thisBuilder.grid;
    }

    abstract public void reloadMergedPopulation(Path agentsFile)
            throws IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;
}
