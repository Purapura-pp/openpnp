package org.openpnp.scripting;

import java.util.Map;

/**
 * The part of {@link Scripting} that the machine and its elements use: firing a named event at
 * whatever user scripts are listening for it.
 * <p>
 * A nozzle that wants to announce a pick does not need the engine pool, the scripts directory or
 * the ability to execute an arbitrary file, and it has no business reaching Configuration to get
 * at them. It needs to say "this happened, here are the details", which is these two methods.
 */
public interface ScriptEvents {
    /**
     * Run whatever scripts are registered for the named event.
     *
     * @param event
     * @param globals objects the scripts are given by name, or null
     * @throws Exception
     */
    void on(String event, Map<String, Object> globals) throws Exception;

    /**
     * Whether the event is known to have no scripts listening, so that a caller can skip the work
     * of assembling the globals.
     * <p>
     * False when there are scripts and also when it is not yet known either way.
     *
     * @param event
     * @return
     */
    Boolean hasNoScript(String event);
}
