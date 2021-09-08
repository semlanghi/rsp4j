package org.streamreasoning.rsp4j.yasper.querying.operators.r2r;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFTerm;

public interface VarOrTerm extends IRI, Var {

    boolean bind(Binding b, RDFTerm t);

}