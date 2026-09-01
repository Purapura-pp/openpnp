package org.openpnp;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openpnp.model.Configuration;

/**
 * Clears the Configuration singleton when a test class finishes, so that the next one cannot
 * inherit it.
 * <p>
 * The tests share one JVM - surefire is not configured to fork - and nothing used to clear the
 * instance, so whatever the previously run class set up was still there. A class that forgot to
 * initialize therefore passed, on somebody else's configuration, and would only fail on the day it
 * happened to run first or alone. Worse, a test that changed a preference changed it for everything
 * that ran afterwards, which is why the length converter test used to save the user's units and put
 * them back.
 * <p>
 * Registered for every test through {@code META-INF/services} and the autodetection flag in
 * {@code junit-platform.properties}, because the point is to cover the classes that did not think
 * to ask for it.
 * <p>
 * After the class rather than after each test: a class is entitled to initialize once in
 * {@code @BeforeAll} and several do.
 */
public class ConfigurationIsolation implements AfterAllCallback {
    @Override
    public void afterAll(ExtensionContext context) {
        Configuration.deinitialize();
    }
}
