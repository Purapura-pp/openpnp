import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.openpnp.Translations;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;

/**
 * A runtime smoke check for the translation bundles, one language at a time.
 *
 * The failure this exists for is the silent one. A bundle whose file name does not spell a locale
 * the way {@link Locale} does is never loaded, and the interface then looks exactly as it does when
 * nobody has translated it yet: all English, no error, nothing in the log. LocalisationTest does not
 * catch it, because it only ever reads the English file, and i18n.py does not catch it either,
 * because it reads the files by name rather than through the resource machinery the application
 * uses. So these tests go through that machinery.
 */
public class TranslationBundlesTest {
    private static final String BUNDLE = "org.openpnp.translations";

    private static final String TEXT_BUNDLE = "org.openpnp.texts";

    /** Deliberately looser than the one in Translations: it has to see the misnamed files too. */
    private static final Pattern BUNDLE_FILE =
            Pattern.compile(".*/translations_(.+)\\.properties");

    /**
     * The lookup the application does, minus the two fallbacks that would hide a missing bundle.
     * ResourceBundle normally answers a request it cannot satisfy with the default locale's bundle
     * and then with the base one, which is the behaviour that makes an unloadable file invisible.
     */
    private static class ExactControl extends Translations.UTF8Control {
        @Override
        public Locale getFallbackLocale(String baseName, Locale locale) {
            return null;
        }
    }

    @Test
    public void everyDiscoveredLocaleLoadsABundleOfItsOwn() {
        List<Locale> locales = Translations.getAvailableLocales();
        assertTrue(locales.contains(Locale.US), "English is always available");
        assertTrue(locales.size() > 1, "no translated bundle was discovered at all");

        for (Locale locale : translated(locales)) {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE, locale, new ExactControl());
            assertEquals(locale, bundle.getLocale(),
                    "translations_" + locale + ".properties was not loaded for " + locale
                            + "; the file name does not spell the locale the way Locale does");
        }
    }

    /**
     * The other half of the same failure. The test above walks the locales the application found,
     * so it can only speak for files it managed to recognise: a bundle named translations_zh-CN
     * rather than translations_zh_CN is not merely unloadable, it is invisible, and the loop above
     * would never ask about it. This walks the files instead and requires each to be accounted for.
     */
    @Test
    public void everyBundleFileOnTheClasspathIsAccountedFor() {
        List<Locale> available = Translations.getAvailableLocales();
        for (String suffix : bundleSuffixes(BUNDLE_FILE)) {
            Locale locale = Locale.forLanguageTag(suffix.replace('_', '-'));
            assertFalse(locale.getLanguage().isEmpty(),
                    "translations_" + suffix + ".properties does not name a locale, so it is never "
                            + "loaded and the language it holds can never be displayed");
            assertTrue(available.contains(locale),
                    "translations_" + suffix + ".properties holds " + locale + ", which the "
                            + "language menu does not offer");
        }
    }

    private static List<String> bundleSuffixes(java.util.regex.Pattern pattern) {
        List<String> suffixes = new ArrayList<>();
        try (ScanResult scan = new ClassGraph().acceptPaths("org/openpnp").scan()) {
            for (Resource resource : scan.getAllResources()) {
                Matcher matcher = pattern.matcher("/" + resource.getPath());
                if (matcher.matches()) {
                    suffixes.add(matcher.group(1));
                }
            }
        }
        assertFalse(suffixes.isEmpty(), "no translated bundle file was found on the classpath");
        return suffixes;
    }

    @Test
    public void everyKeyEveryLocaleDefinesResolvesToSomething() {
        for (Locale locale : translated(Translations.getAvailableLocales())) {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE, locale, new ExactControl());
            int checked = 0;
            for (Enumeration<String> keys = bundle.getKeys(); keys.hasMoreElements(); ) {
                String key = keys.nextElement();
                String value = bundle.getString(key);
                assertFalse(value.trim().isEmpty(), locale + " translates " + key + " to nothing");
                assertFalse(value.equals('!' + key + '!'),
                        locale + " carries the marker Translations.getString writes for a key it "
                                + "cannot find, under " + key);
                checked++;
            }
            assertTrue(checked > 0, locale + " has a bundle but it defines no keys");
        }
    }

    @Test
    public void theIssuesAndSolutionsTextsResolveWhereverTheyExist() {
        for (Locale locale : translated(Translations.getAvailableLocales())) {
            ResourceBundle bundle;
            try {
                bundle = ResourceBundle.getBundle(TEXT_BUNDLE, locale, new ExactControl());
            }
            catch (MissingResourceException e) {
                // A language that has not taken on the descriptions yet. They fall back to English.
                continue;
            }
            for (Enumeration<String> keys = bundle.getKeys(); keys.hasMoreElements(); ) {
                String english = keys.nextElement();
                assertFalse(english.trim().isEmpty(), locale + " has an empty English key");
                assertFalse(bundle.getString(english).trim().isEmpty(),
                        locale + " translates an Issues and Solutions description to nothing: "
                                + english);
            }
        }
    }

    @Test
    public void translateTextLeavesEnglishItDoesNotKnowAlone() {
        String unknown = "This prose is not in any bundle.";
        assertEquals(unknown, Translations.translateText(unknown));
        assertEquals("", Translations.translateText(""));
    }

    private static List<Locale> translated(List<Locale> locales) {
        List<Locale> result = new ArrayList<>(locales);
        result.remove(Locale.US);
        return result;
    }
}
