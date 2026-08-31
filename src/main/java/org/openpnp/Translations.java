package org.openpnp;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Translations {
    private static final String BUNDLE_NAME = "org.openpnp.translations"; //$NON-NLS-1$

    private static final String TEXT_BUNDLE_NAME = "org.openpnp.texts"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, new UTF8Control());

    private static final ResourceBundle TEXT_BUNDLE = loadTextBundle();

    private Translations() {
    }

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    private static ResourceBundle loadTextBundle() {
        try {
            return ResourceBundle.getBundle(TEXT_BUNDLE_NAME, new UTF8Control());
        }
        catch (MissingResourceException e) {
            return null;
        }
    }

    /**
     * Translates a string the source code carries as English prose instead of as a key, keyed by
     * that English. The Issues and Solutions descriptions are written that way, and their English
     * wording doubles as their persisted identity - see Solutions.Issue.getFingerprint - so they
     * cannot be turned into keys without invalidating every issue a user has already dismissed or
     * marked as solved, and without the identity shifting whenever the display language changes.
     * <p>
     * Returns the English unchanged when nothing matches. That is the normal outcome for the
     * descriptions that are assembled from variables at runtime, since only whole strings are
     * looked up.
     */
    public static String translateText(String english) {
        if (TEXT_BUNDLE == null || english == null || english.isEmpty()) {
            return english;
        }
        try {
            return TEXT_BUNDLE.getString(english);
        }
        catch (MissingResourceException e) {
            return english;
        }
    }

    public static class UTF8Control extends ResourceBundle.Control {
        public ResourceBundle newBundle
                (String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try {
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                } finally {
                    stream.close();
                }
            }
            return bundle;
        }
    }
}
