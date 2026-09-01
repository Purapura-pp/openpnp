package org.openpnp;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pmw.tinylog.Logger;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;

public class Translations {
    private static final String BUNDLE_NAME = "org.openpnp.translations"; //$NON-NLS-1$

    private static final String TEXT_BUNDLE_NAME = "org.openpnp.texts"; //$NON-NLS-1$

    /** Where the bundles live, as a resource path rather than a package name. */
    private static final String BUNDLE_PATH = "org/openpnp"; //$NON-NLS-1$

    private static final Pattern BUNDLE_RESOURCE =
            Pattern.compile(".*/translations_(\\w+)\\.properties"); //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, new UTF8Control());

    private static final ResourceBundle TEXT_BUNDLE = loadTextBundle();

    private static List<Locale> availableLocales;

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

    /**
     * The locales the application can display, discovered from the translation bundles that are
     * actually on the classpath rather than from a list in the code, so that adding a language is
     * only a matter of adding its properties file.
     * <p>
     * Scans the classpath rather than a directory: once packaged, the bundles are inside the jar,
     * where File cannot list them.
     */
    public static synchronized List<Locale> getAvailableLocales() {
        if (availableLocales == null) {
            availableLocales = discoverLocales();
        }
        return availableLocales;
    }

    private static List<Locale> discoverLocales() {
        Set<Locale> locales = new HashSet<>();
        // The base bundle holds English and has no suffix to discover it by.
        locales.add(Locale.US);
        try (ScanResult scan = new ClassGraph().acceptPaths(BUNDLE_PATH).scan()) {
            for (Resource resource : scan.getAllResources()) {
                Matcher matcher = BUNDLE_RESOURCE.matcher("/" + resource.getPath());
                if (matcher.matches()) {
                    locales.add(localeOfSuffix(matcher.group(1)));
                }
            }
        }
        catch (Throwable t) {
            // Better to offer only English than to fail starting up over a menu.
            Logger.warn(t, "Could not scan for translation bundles."); //$NON-NLS-1$
        }
        // By language tag, not by display name: this list is cached, and it is first built before
        // Locale.setDefault has been given the configured locale, so display names here would be
        // ordered by whatever the system language happened to be. Callers that show the list sort
        // it for the user themselves.
        List<Locale> sorted = new ArrayList<>(locales);
        sorted.sort(Comparator.comparing(Locale::toLanguageTag));
        return Collections.unmodifiableList(sorted);
    }

    private static Locale localeOfSuffix(String suffix) {
        String[] parts = suffix.split("_", 3); //$NON-NLS-1$
        if (parts.length == 1) {
            return new Locale(parts[0]);
        }
        if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        }
        return new Locale(parts[0], parts[1], parts[2]);
    }

    /**
     * The locale to display when the user has never picked one. Prefers an exact match for the
     * system locale, then a bundle whose language matches, and falls back to English.
     * <p>
     * Called before {@link Locale#setDefault} has been given the configured value, so the default
     * locale it reads is still the system one. Re-reading it afterwards yields the same answer,
     * because whatever this returns is itself one of the available locales.
     */
    public static Locale matchSystemLocale() {
        Locale system = Locale.getDefault();
        List<Locale> available = getAvailableLocales();
        for (Locale locale : available) {
            if (locale.equals(system)) {
                return locale;
            }
        }
        for (Locale locale : available) {
            if (locale.getLanguage().equals(system.getLanguage())) {
                return locale;
            }
        }
        return Locale.US;
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
