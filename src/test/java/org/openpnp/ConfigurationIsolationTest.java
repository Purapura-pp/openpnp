package org.openpnp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.openpnp.model.Configuration;

/**
 * Checks that {@link ConfigurationIsolation} is actually being applied.
 * <p>
 * An extension registered through {@code META-INF/services} is silent when it is not picked up:
 * nothing logs that it was skipped, and every test goes on passing, so the isolation it provides
 * would quietly stop existing the next time the JUnit configuration is touched. This class does not
 * initialize a configuration and asserts that it cannot see one, which is only true if the classes
 * that ran before it had theirs cleared.
 * <p>
 * The check depends on something having run first, and surefire runs the default package before
 * {@code org.openpnp}, so several classes that do initialize are always ahead of this one. Run on
 * its own it passes for the trivial reason, which is the right way round for a probe: it cannot fail
 * for a reason other than the one it is looking for.
 */
public class ConfigurationIsolationTest {
    @Test
    public void noConfigurationSurvivesTheClassThatMadeIt() {
        assertFalse(Configuration.isInstanceInitialized(),
                "a configuration set up by an earlier test class is still here, so "
                        + "ConfigurationIsolation is not being applied; check that "
                        + "junit-platform.properties still enables extension autodetection and "
                        + "that META-INF/services names the class");
    }

    /**
     * The other half of the same thing: reaching for it has to fail rather than answer, so that a
     * test which forgets to initialize says so instead of running on whatever it finds.
     */
    @Test
    public void askingForOneThatWasNeverInitializedFails() {
        assertThrows(Error.class, Configuration::get);
    }
}
