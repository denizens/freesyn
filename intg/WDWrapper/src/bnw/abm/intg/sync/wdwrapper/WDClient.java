package bnw.abm.intg.sync.wdwrapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import repast.simphony.engine.environment.RunState;
import repast.simphony.parameter.Parameters;
import repast.simphony.parameter.ParametersParser;

import com.google.gson.Gson;

public class WDClient {

    BufferedWriter outToServer;
    BufferedReader inFromServer;
    List<String> commands = null;
    RECRunner wdRunner;
    File scenarioFile; // the scenario dir
    File parametersFile;
    SimulationState state;

    public WDClient() {
        this.commands = new ArrayList<String>();
        this.wdRunner = new RECRunner();
        // this.state = new SimulationState(RunState.getInstance().getMasterContext());

    }

    // static WDClient wd = null;
    public static void main(String[] args) {
        Socket clientSocket = null;
        WDClient wd = new WDClient();
        try {

            wd.scenarioFile = new File("/home/niroshan/repos/anonymous/sources/WeddingGrid/WeddingGrid.rs");
            wd.parametersFile = new File(wd.scenarioFile.getPath() + "/parameters.xml");

            clientSocket = new Socket("localhost", 6799);

            wd.outToServer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            wd.inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            wd.send2IS("I am WD");

            wd.setupSimulation(wd.wdRunner, wd.scenarioFile, wd.parametersFile);

            wd.recieveFromIS();
            wd.executeCommands(wd.commands, wd.wdRunner);

            wd.print(wd.state.getAgentStatuses());
            // clientSocket.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();

        } finally {

            try {
                wd.inFromServer.close();
                wd.outToServer.close();
                clientSocket.close();
            } catch (IOException closeExceptions) {
                // TODO Auto-generated catch block
                closeExceptions.printStackTrace();
            }
        }
    }

    private void send2IS(String str) throws IOException {
        outToServer.write(str);
        outToServer.newLine();
        outToServer.flush();
    }

    private void recieveFromIS() throws IOException {
        String input = null;

        input = inFromServer.readLine();
        Gson gson = new Gson();
        LinkedHashMap<String, String[]> mapStructure = new LinkedHashMap<>();
        LinkedHashMap<String, String[]> jsonMap = gson.fromJson(input, mapStructure.getClass());

        if (jsonMap.containsKey("Commands")) {
            for (String ob : jsonMap.get("Commands")) {
                if (ob != null)
                    commands.add(ob);
            }
        }

    }

    private List<HashMap<Object, Object>> recieveStatesFromIS() throws IOException {
        String input = null;
        List<HashMap<Object, Object>> agentStates = new ArrayList<HashMap<Object, Object>>();
        // JSONParser jp = new JSONParser();
        // JSONObject jo = null;

        input = inFromServer.readLine();
        Gson gson = new Gson();
        agentStates = gson.fromJson(input, agentStates.getClass());
        // System.out.println("From Server: " + input);
        // try {
        // jo = (JSONObject) jp.parse(input);
        // } catch (ParseException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // if (jo.containsKey("State") && ((JSONObject) jo.get("State")).containsKey("Agents")) {
        // for (Object ob : (JSONArray) ((JSONObject) jo.get("State")).get("Agents")) {
        // if (ob != null)
        // agentStates.add((HashMap<Object, Object>) ob);
        // }
        // }

        return agentStates;
    }

    void executeCommands(List<String> commands, RECRunner simRunner) throws IOException {
        Iterator<String> it = commands.iterator();
        while (it.hasNext()) {
            switch (it.next()) {
            case "INIT":
                simRunner.runInitialize();
                state.saveStates(RunState.getInstance().getMasterContext());
                break;
            case "SENDSTATE":
                state.saveStates(RunState.getInstance().getMasterContext());
                ArrayList al = state.getAgentStatuses();
                this.send2IS(convertSimState2JSON(al));
                break;
            case "STEP":
                simRunner.step();
                break;
            case "UPDATE":
                List lst = this.recieveStatesFromIS();
                state.updateAgentStates((ArrayList<Map<String, Object>>) lst);
                break;
            case "SENDIDMAP":
                this.send2IS(convertAgentIDMap2JSON(state.getAgentIDMap()));
                break;
            case "SAVESTATE":
                state.saveStates(RunState.getInstance().getMasterContext());
                break;
            }
        }
    }

    private void convert2GlobalDataFormat(ArrayList al) {

    }

    void setupSimulation(final RECRunner wdRunner, File scenarioFile, File parametersFile) {
        ParametersParser parameterParser;
        Parameters params = null;
        try {
            parameterParser = new ParametersParser(parametersFile);
            params = parameterParser.getParameters();
        } catch (ParserConfigurationException | SAXException | IOException e1) {
            e1.printStackTrace();
        }

        try {
            wdRunner.load(scenarioFile, params); // load the repast scenario
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String convertSimState2JSON(ArrayList<Object> al) {
        Map<String, Map<String, List<Object>>> data = new LinkedHashMap<>();
        Map<String, List<Object>> agents = new LinkedHashMap<>();
        agents.put("Agents", al);
        data.put("State", agents);
        Gson gson = new Gson();
        return gson.toJson(al);
        // JSONObject job = new JSONObject();
        // JSONArray jar = new JSONArray();
        // Iterator<Object> itr = al.iterator();
        //
        // while (itr.hasNext()) {
        // Object ob = itr.next();
        // if (ob instanceof Map) {
        // jar.add(new JSONObject((Map) ob));
        // }
        // }
        // JSONObject o = new JSONObject();
        // o.put("Agents", jar);
        //
        // job.put("State", o);
        // // System.out.println(job.toJSONString());
        // return job;

    }

    String convertAgentIDMap2JSON(Map mp) {
        Map<String, Map> outMap = new LinkedHashMap<>();
        outMap.put("Idmap", mp);
        Map<String, Map<String, Map>> jsonOut = new LinkedHashMap<>();
        jsonOut.put("State", outMap);
        return new Gson().toJson(jsonOut);
        // JSONObject job = new JSONObject(mp);
        // JSONObject o = new JSONObject();
        // o.put("Idmap", job);
        // return (JSONObject) (new JSONObject()).put("State", o);
    }

    public void print(ArrayList<Map<String, Object>> al) {
        for (Map mp : al) {
            Set<Map.Entry> attributes = mp.entrySet();
            for (Map.Entry attribute : attributes) {
                System.out.print(attribute.getKey() + ":" + attribute.getValue() + ", ");
            }
            System.out.println("");
        }
    }
}