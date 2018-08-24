package simple.windowing;

import it.polimi.yasper.core.rspql.execution.ContinuousQueryExecution;
import it.polimi.yasper.core.rspql.tvg.TimeVaryingGraph;
import it.polimi.yasper.core.spe.content.Content;
import it.polimi.yasper.core.spe.content.ContentGraph;
import it.polimi.yasper.core.spe.content.EmptyGraphContent;
import it.polimi.yasper.core.spe.exceptions.OutOfOrderElementException;
import it.polimi.yasper.core.spe.time.Time;
import it.polimi.yasper.core.spe.windowing.assigner.ObservableWindowAssigner;
import it.polimi.yasper.core.spe.windowing.definition.Window;
import it.polimi.yasper.core.spe.windowing.definition.WindowImpl;
import it.polimi.yasper.core.rspql.RDFUtils;
import lombok.extern.log4j.Log4j;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;

import java.util.*;
import java.util.stream.Collectors;

@Log4j
public class CSPARQLWindowAssigner extends ObservableWindowAssigner<Graph> implements Observer {

    private final long a, b;
    private final long t0;

    private Map<Window, Content<Graph>> active_windows;
    private Set<Window> to_evict;
    private long tc0;
    private long toi;

    public CSPARQLWindowAssigner(IRI iri, long a, long b, long t0, long tc0, Time instance) {
        super(iri, instance);
        this.a = a;
        this.b = b;
        this.t0 = t0;
        this.tc0 = tc0;
        this.toi = 0;
        this.active_windows = new HashMap<>();
        this.to_evict = new HashSet<>();
    }

    @Override
    public Content<Graph> getContent(long t_e) {
        Optional<Window> max = active_windows.keySet().stream()
                .filter(w -> w.getO() < t_e && w.getC() <= t_e)
                .max(Comparator.comparingLong(Window::getC));

        if (max.isPresent())
            return active_windows.get(max.get());

        return new EmptyGraphContent();
    }

    @Override
    public List<Content> getContents(long t_e) {
        return active_windows.keySet().stream()
                .filter(w -> w.getO() <= t_e && t_e < w.getC())
                .map(active_windows::get).collect(Collectors.toList());
    }

    @Override
    public void update(Observable o, Object arg) {
    }

    protected void windowing(Graph e, long timestamp) {

        log.debug("Received element (" + e + "," + timestamp + ")");
        long t_e = timestamp;

        if (time.getAppTime() > t_e) {
            log.error("OUT OF ORDER NOT HANDLED");
            throw new OutOfOrderElementException("(" + e + "," + timestamp + ")");
        }

        scope(t_e);

        active_windows.keySet().forEach(
                w -> {
                    log.debug("Processing Window [" + w.getO() + "," + w.getC() + ") for element (" + e + "," + timestamp + ")");
                    if (w.getO() <= t_e && t_e < w.getC()) {
                        log.debug("Adding element [" + e + "] to Window [" + w.getO() + "," + w.getC() + ")");
                        active_windows.get(w).add(e);
                    } else if (t_e > w.getC()) {
                        log.debug("Scheduling for Eviction [" + w.getO() + "," + w.getC() + ")");
                        schedule_for_eviction(w);
                    }
                });


        active_windows.keySet().stream()
                .filter(w -> report.report(w, null, t_e, System.currentTimeMillis()))
                .max(Comparator.comparingLong(Window::getC))
                .ifPresent(window -> tick(t_e, window));

//        switch (aw) {
//            case MULTIPLE:
//                active_windows.keySet().stream()
//                        .filter(w -> report.report(w, null, t_e, System.currentTimeMillis()))
//                        .forEach(w -> tick(t_e, w));
//                break;
//            case SINGLE:
//            default:
//                active_windows.keySet().stream()
//                        .filter(w -> report.report(w, null, t_e, System.currentTimeMillis()))
//                        .max(Comparator.comparingLong(Window::getC))
//                        .ifPresent(window -> tick(t_e, window));
//        }

        to_evict.forEach(w -> {
            log.debug("Evicting [" + w.getO() + "," + w.getC() + ")");
            active_windows.remove(w);
            if (toi < w.getC())
                toi = w.getC() + b;
        });
        to_evict.clear();
    }

    private void scope(long t_e) {
        long c_sup = (long) Math.ceil(((double) Math.abs(t_e - tc0) / (double) b)) * b;
        long o_i = c_sup - a;
        log.debug("Calculating the Windows to Open. First one opens at [" + o_i + "] and closes at [" + c_sup + "]");

        do {
            log.debug("Computing Window [" + o_i + "," + (o_i + a) + ") if absent");

            active_windows
                    .computeIfAbsent(new WindowImpl(o_i, o_i + a), x -> new ContentGraph());
            o_i += b;

        } while (o_i <= t_e);

    }


    private void schedule_for_eviction(Window w) {
        to_evict.add(w);
    }


    protected Content compute(long t_e, Window w) {
        Content content = active_windows.containsKey(w) ? active_windows.get(w) : new EmptyGraphContent();
        time.setAppTime(t_e);
        return content;
    }


    @Override
    public TimeVaryingGraph set(ContinuousQueryExecution content) {
        this.addObserver(content);
        //TODO Generalize the type of content using an ENUM
        return new TimeVaryingGraph(this, iri, RDFUtils.createGraph());
    }


}
