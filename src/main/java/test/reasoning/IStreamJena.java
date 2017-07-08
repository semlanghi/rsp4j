package test.reasoning;

import it.polimi.rsp.core.enums.Entailment;
import it.polimi.rsp.core.enums.Maintenance;
import it.polimi.rsp.core.rsp.RSPQLEngine;
import it.polimi.rsp.core.rsp.query.execution.ContinuousQueryExecution;
import it.polimi.rsp.core.rsp.query.formatter.ConstructResponseSysOutFormatter;
import it.polimi.rsp.core.rsp.query.formatter.SelectResponseSysOutFormatter;
import it.polimi.rsp.core.rsp.stream.RSPEsperEngine;
import it.polimi.sr.rsp.RSPQLParser;
import it.polimi.sr.rsp.RSPQuery;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.system.IRIResolver;
import org.parboiled.Parboiled;
import org.parboiled.errors.ParseError;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;
import test.GraphStream;

import java.io.File;
import java.io.IOException;

/**
 * Created by Riccardo on 03/08/16.
 */
public class IStreamJena {

    public static void main(String[] args) throws InterruptedException, IOException {

        RSPEsperEngine e = new RSPQLEngine(0);
        RSPQuery q = getRspQuery();

        RSPQLEngine sr = (RSPQLEngine) e;
        sr.startProcessing();

        Model tbox = ModelFactory.createDefaultModel();//.read("/Users/riccardo/_Projects/RSP/RSP-Baselines/src/main/resources/arist.tbox.owl");
        ContinuousQueryExecution cqe = sr.registerQuery(q, tbox, Maintenance.NAIVE, Entailment.RHODF);

        if (q.isSelectType())
            sr.registerObserver(cqe, new SelectResponseSysOutFormatter(true)); // attaches a new *RSP-QL query to the SDS
        if (q.isConstructType())
            sr.registerObserver(cqe, new ConstructResponseSysOutFormatter(true)); // attaches a new *RSP-QL query to the SDS

        (new Thread(new GraphStream(sr, "Painter", "http://streamreasoning.org/iminds/massif/stream1", 1))).start();

    }

    private static RSPQuery getRspQuery() throws IOException {
        String input = getInput();

        RSPQLParser parser = Parboiled.createParser(RSPQLParser.class);

        parser.setResolver(IRIResolver.create());

        ParsingResult<RSPQuery> result = new ReportingParseRunner(parser.Query()).run(input);

        if (result.hasErrors()) {
            for (ParseError arg : result.parseErrors) {
                System.out.println(input.substring(0, arg.getStartIndex()) + "|->" + input.substring(arg.getStartIndex(), arg.getEndIndex()) + "<-|" + input.substring(arg.getEndIndex() + 1, input.length() - 1));
            }
        }
        return result.resultValue;
    }

    public static String getInput() throws IOException {
        File file = new File("/Users/riccardo/_Projects/RSP/RSP-Baselines/src/test/resources/q52.rspql");
        return FileUtils.readFileToString(file);
    }
}
