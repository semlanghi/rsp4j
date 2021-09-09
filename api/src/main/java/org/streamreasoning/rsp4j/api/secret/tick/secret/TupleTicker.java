package org.streamreasoning.rsp4j.api.secret.tick.secret;

import org.streamreasoning.rsp4j.api.operators.s2r.execution.assigner.StreamToRelationOp;
import org.streamreasoning.rsp4j.api.operators.s2r.execution.instance.Window;
import org.streamreasoning.rsp4j.api.secret.tick.Ticker;

/**
 * The Tick dimension in our model defines the condition which drives an SPE
 * to take action on its input (also referred to as “window state change” or “window re-evaluation” [13]).
 * Like Report, Tick is also part of a system’s inter- nal execution model.
 * While some systems react to individual tuples as they arrive, others collectively
 * react to all or subsets of tuples with the same tapp value.
 * During our analysis, we have identified three main ways that di↵erent systems “tick”:
 * (a) tuple-driven, where each tuple arrival causes a system to react;
 * (b) time-driven, where the progress of tapp causes a system to react;
 * (c) batch-driven, where either a new batch arrival or the progress of tapp causes a system to react.
 **/
public class TupleTicker implements Ticker {

    private final StreamToRelationOp<?, ?> wa;

    public TupleTicker(StreamToRelationOp<?, ?> wa) {
        this.wa = wa;
    }

    @Override
    public void tick(long t_e, Window w) {
        wa.compute(t_e, w);
    }
}


