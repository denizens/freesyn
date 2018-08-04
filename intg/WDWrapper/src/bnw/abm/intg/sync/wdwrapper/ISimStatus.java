package bnw.abm.intg.sync.wdwrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import repast.simphony.context.Context;

public interface ISimStatus {

    void saveStates(Context<Object> context) throws IOException;

    public ArrayList<Map<String,Object>> getAgentStatuses();

    void setAgentStatuses(ArrayList<Map<String,Object>> agentStatuses);
}