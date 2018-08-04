package bnw.abm.intg.apps.latch;

import bnw.abm.intg.apps.latch.algov1.LatchPopAlgoV1SimAn;
import bnw.abm.intg.apps.latch.algov1.LatchPopulationAPD;
import bnw.abm.intg.apps.latch.algov1.LatchPopulationAlgoV1;
import bnw.abm.intg.apps.latch.algov1.LatchPopulationNonIPFPAlgoV1;
import bnw.abm.intg.apps.latch.algov2.LatchPopulationAlgoV2;
import bnw.abm.intg.apps.latch.algov2.NonIPFPPopulationConstructor;
import bnw.abm.intg.apps.latch.algov2.PopulationConstructor;
import bnw.abm.intg.apps.latch.algov2.TemplateUtil;
import bnw.abm.intg.apps.latch.ipucensus.IPUDataSynthesiser;
import bnw.abm.intg.apps.latch.ipucensus.NonIPFSynthesiserWithIPUData;
import bnw.abm.intg.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        if (args.length >= 2) {
            switch (args[1]) {
                case "algov1":
                    LatchPopulationAlgoV1.build(args[0], args.length > 2 ? args[2] : "", args.length > 3 ? args[3] : "");
                    break;
                case "algov1SimAn":
                    LatchPopAlgoV1SimAn.build(args[0], args.length > 2 ? args[2] : "", args.length > 3 ? args[3] : "");
                    break;
                case "ipu-data":
                    IPUDataSynthesiser.build(args[0], args.length > 2 ? args[2] : "", args.length > 3 ? args[3] : "");
                    break;
                case "ipu-data-no-ipf":
                    NonIPFSynthesiserWithIPUData.build(args[0], args.length > 2 ? args[2] : "", args.length > 3 ? args[3] : "");
                    break;
                case "noipf":
                    LatchPopulationNonIPFPAlgoV1.build(args[0], args.length > 2 ? args[2] : "", args.length > 3 ? args[3] : "");
                    break;
                case "sa2apd":
                    LatchPopulationAPD.build(args[0], args.length > 2 ? args[2] : "", args.length > 3 ? args[3] : "");
                    break;
                case "algov2":
                    if (args[2].equals("-t")) {
                        if (args.length == 5) {
                            int fromAgentType = Integer.parseInt(args[3]);
                            int toAgentType = Integer.parseInt(args[4]);
                            LatchPopulationAlgoV2.build(args[0], fromAgentType, toAgentType);
                        } else {
                            throw new IllegalArgumentException("Starting and ending agent type not specified");
                        }
                    } else if (args[2].equals("-p")) {
                        PopulationConstructor.construct(args[0]);
                    } else if (args[2].equals("-tp")) {
                        try {
                            TemplateUtil.readGroupTemplatesFromFiles(args[0]);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case "algov2.1":
                    if (args[2].equals("-p")) {
                        NonIPFPPopulationConstructor.construct(args[0]);
                    } else {
                        throw new IllegalArgumentException("Unrecognised argument: " + args[2]);
                    }
                default:
                    System.out.println(
                            "java -jar latch.jar Run.properties algov1 -- generate population using algorithm v1 with hill climbing");
                    System.out.println(
                            "java -jar latch.jar Run.properties algov1SimAn -- generate population using algorithm v1 with simulated " +
                                    "annealing");
                    System.out.println(
                            "java -jar latch.jar Run.properties ipu-data -- generate population with IPU data format using algorithm v1 with hill climbing");
                    System.out.println(
                            "java -jar latch.jar Run.properties noipf -- generate population using algorithm v1.1 (without IPFP)");
                    System.out.println(
                            "java -jar latch.jar Run.properties algov2 -t startAgentType endAgentType -- generates group templates using " +
                                    "agent types from startAgentType to endAgentType as first reference agent of the template");
                    System.out.println(
                            "java -jar latch.jar Run.properties algov2 -p -- generate population using group templates give in Template " +
                                    "files (specified in Run.properties");
                    throw new IllegalArgumentException("Algorithm version unrecognised");
            }
        } else {
            throw new IllegalArgumentException("Properties file and Algorithm version not specified");
        }
        long durationMillis = System.currentTimeMillis() - startTime;
        Log.info("Execution time: " + String.format("%d min, %d sec",
                                                    TimeUnit.MILLISECONDS.toMinutes(durationMillis),
                                                    TimeUnit.MILLISECONDS.toSeconds(durationMillis) -
                                                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationMillis))
        ));
    }
}
