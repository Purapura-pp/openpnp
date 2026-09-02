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

    /**
     * The bundles that sit next to the translations are not languages.
     * <p>
     * {@code texts_ru.properties} and {@code patterns_zh_CN.properties} live in the same package
     * and are named the same way, so a scan that looked for any suffixed properties file would
     * offer "texts" and "patterns" in the language menu. What keeps them out is the
     * {@code translations_} in the pattern Translations scans with, and nothing else, so it is
     * worth a test: the sibling families are the kind of thing that gets added later, as patterns
     * was.
     */
    @Test
    public void theSiblingBundleFamiliesAreNotOfferedAsLanguages() {
        for (Locale locale : Translations.getAvailableLocales()) {
            assertFalse(locale.getLanguage().equals("texts") || locale.getLanguage().equals("patterns"),
                    "the language menu offers " + locale + ", which came from a sibling bundle "
                            + "family rather than from a translation");
        }
        for (String family : new String[] {"texts", "patterns"}) {
            Pattern pattern = Pattern.compile(".*/" + family + "_(.+)\\.properties");
            for (String suffix : bundleSuffixes(pattern)) {
                Locale locale = Locale.forLanguageTag(suffix.replace('_', '-'));
                assertTrue(Translations.getAvailableLocales().contains(locale),
                        family + "_" + suffix + ".properties is there for a locale the language "
                                + "menu does not offer, so nothing can ever read it");
            }
        }
    }

    /**
     * What the application shows someone who has never picked a language.
     * <p>
     * Read before {@link Locale#setDefault} has been given the configured value, so what it sees
     * is the system locale, and the answer has to be one of the bundles that exist rather than
     * whatever the system happens to say.
     */
    @Test
    public void theSystemLocaleIsMatchedToABundleThatExists() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
            assertEquals(Locale.SIMPLIFIED_CHINESE, Translations.matchSystemLocale(),
                    "an exact match should be taken as it is");

            Locale.setDefault(new Locale("de", "AT"));
            assertEquals(new Locale("de"), Translations.matchSystemLocale(),
                    "an Austrian should be shown the German bundle rather than English");

            Locale.setDefault(new Locale("ja", "JP"));
            assertEquals(Locale.US, Translations.matchSystemLocale(),
                    "a language nobody has translated should fall back to English");

            Locale.setDefault(Locale.US);
            assertEquals(Locale.US, Translations.matchSystemLocale(),
                    "the answer has to be stable once it has been applied, since it is read again "
                            + "after Locale.setDefault");
        }
        finally {
            Locale.setDefault(saved);
        }
    }

    private static List<Locale> translated(List<Locale> locales) {
        List<Locale> result = new ArrayList<>(locales);
        result.remove(Locale.US);
        return result;
    }
}
