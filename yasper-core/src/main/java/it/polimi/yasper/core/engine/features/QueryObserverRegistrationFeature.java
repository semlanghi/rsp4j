package it.polimi.yasper.core.engine.features;

import it.polimi.yasper.core.quering.ContinuousQuery;
import it.polimi.yasper.core.quering.execution.ContinuousQueryExecution;
import it.polimi.yasper.core.quering.formatter.QueryResponseFormatter;

public interface QueryObserverRegistrationFeature {

    void register(ContinuousQuery q, QueryResponseFormatter o);

}
