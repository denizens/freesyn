package bnw.abm.intg.algov2.templates;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import bnw.abm.intg.algov2.framework.models.GroupType;
import bnw.abm.intg.algov2.framework.models.IPFPTable;
import bnw.abm.intg.algov2.framework.models.AgentType.ReferenceAgentType;
import bnw.abm.intg.util.GlobalConstants.EXITCODE;
import bnw.abm.intg.util.Log;

public class IPFPTableBuilder {

    final private Path ipfpCsv;

    public IPFPTableBuilder(Path ipfpCsv) {
        this.ipfpCsv = ipfpCsv;
    }

    public IPFPTable build() {
        IPFPTable ipfpTable = new IPFPTable();
        try (CSVParser csvParser = new CSVParser(Files.newBufferedReader(ipfpCsv), CSVFormat.EXCEL.withHeader())) {
            csvParser.getHeaderMap().keySet();
            for (CSVRecord csvRec : csvParser) {
                for (String colKey : csvParser.getHeaderMap().keySet()) {
                    if (colKey.equals("")) {
                        continue;
                    }
                    ReferenceAgentType atype = ReferenceAgentType.getExisting(Integer.parseInt(colKey));
                    if (atype == null) {
                        Log.warn("New Agent Type created: " + colKey);
                        atype = ReferenceAgentType.getInstance(Integer.parseInt(colKey));
                    }

                    int gTypeInt = Integer.parseInt(csvRec.get(0));
                    GroupType gType = GroupType.getExisting(gTypeInt);
                    if (gType == null) {
                        Log.warn("New Group Type created: " + csvRec.get(0));
                        gType = GroupType.getInstance(gTypeInt);
                    }

                    ipfpTable.put(gType, atype, Double.parseDouble(csvRec.get(colKey)));
                }
            }
        } catch (Exception ioe) {
            Log.errorAndExit("Reading IPFP distribution file failed " + ipfpCsv, ioe, EXITCODE.IOERROR);
        }

        return ipfpTable;
    }
}
