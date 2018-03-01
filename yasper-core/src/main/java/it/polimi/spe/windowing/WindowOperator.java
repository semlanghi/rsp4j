package it.polimi.spe.windowing;

import it.polimi.rspql.Stream;
import it.polimi.spe.windowing.assigner.WindowAssigner;
import org.apache.commons.rdf.api.IRI;

public interface WindowOperator {

    String getName();

    boolean isNamed();

    WindowAssigner apply(Stream s);

}