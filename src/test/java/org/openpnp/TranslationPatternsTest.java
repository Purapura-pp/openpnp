package org.openpnp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.openpnp.Translations.ProsePattern;

/**
 * The templates that translate Issues and Solutions wording the sources assemble at runtime.
 *
 * These go at the template rather than through {@link Translations#translateText}, because that
 * binds its bundles to the locale the JVM started in: asserting Chinese through it would pass on a
 * Chinese machine and fail everywhere else. The templates themselves are read from the bundles and
 * exercised directly, which says the same thing and says it the same way anywhere.
 */
public class TranslationPatternsTest {
    private static final String VOCABULARY_PREFIX = "Word.";

    private static Properties bundle(String name) throws IOException {
        Properties properties = new Properties();
        try (InputStream stream = Translations.class.getClassLoader()
                .getResourceAsStream("org/openpnp/" + name)) {
            assertNotNull(stream, name + " is not on the classpath");
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        return properties;
    }

    private static List<String> templateKeys(Properties english) {
        List<String> keys = new ArrayList<>();
        for (String key : english.stringPropertyNames()) {
            if (!key.startsWith(VOCABULARY_PREFIX)) {
                keys.add(key);
            }
        }
        Collections.sort(keys);
        return keys;
    }

    @Test
    public void aTemplateRecognisesTheSentenceItsSourceAssembles() {
        ProsePattern pattern = ProsePattern.of("test",
                "Calibrate static white balance for camera %s.",
                "\u4e3a\u76f8\u673a %s \u6821\u51c6\u9759\u6001\u767d\u5e73\u8861\u3002");
        assertNotNull(pattern, "the template should have compiled");
        assertEquals("\u4e3a\u76f8\u673a Top \u6821\u51c6\u9759\u6001\u767d\u5e73\u8861\u3002",
                pattern.apply("Calibrate static white balance for camera Top.", Map.of()),
                "the camera name should have been carried through");
    }

    @Test
    public void aTranslationMayPutTheValuesInItsOwnOrder() {
        ProsePattern pattern = ProsePattern.of("test",
                "Automatically calibrates the camera %s using the nozzle %s.",
                "\u7528\u5439\u5634 %2$s \u81ea\u52a8\u6807\u5b9a\u76f8\u673a %1$s\u3002");
        assertNotNull(pattern);
        assertEquals("\u7528\u5439\u5634 N1 \u81ea\u52a8\u6807\u5b9a\u76f8\u673a Bottom\u3002",
                pattern.apply("Automatically calibrates the camera Bottom using the nozzle N1.",
                        Map.of()),
                "the indices should have swapped the camera and the nozzle round");
    }

    @Test
    public void aRoleTheSourceNamesInEnglishIsTranslatedButAUserSNameIsNot() {
        ProsePattern pattern = ProsePattern.of("test",
                "%s is missing a %s actuator.",
                "%1$s \u7f3a\u5c11\u300c%2$s\u300d\u6267\u884c\u5668\u3002");
        assertNotNull(pattern);
        assertEquals("ReferenceHead H1 \u7f3a\u5c11\u300c\u6c14\u6cf5\u63a7\u5236\u300d\u6267\u884c\u5668\u3002",
                pattern.apply("ReferenceHead H1 is missing a pump control actuator.",
                        Map.of("pump control", "\u6c14\u6cf5\u63a7\u5236")),
                "the role should be translated and the head left as the user named it");
    }

    @Test
    public void aTemplateWithNothingDistinctiveInItIsRefused() {
        assertNull(ProsePattern.of("test", "%s.", "%s\u3002"),
                "a template that is only a placeholder would match every string it saw");
        assertNull(ProsePattern.of("test", "A %s B", "%s"),
                "a template whose literal runs are a character or two is no safer");
    }

    @Test
    public void proseNoTemplateDescribesIsLeftAlone() throws IOException {
        Properties english = bundle("patterns.properties");
        Properties chinese = bundle("patterns_zh_CN.properties");
        String unrelated = "This prose is not in any bundle.";
        for (String key : templateKeys(english)) {
            ProsePattern pattern = ProsePattern.of(key, english.getProperty(key),
                    chinese.getProperty(key));
            if (pattern != null) {
                assertNull(pattern.apply(unrelated, Map.of()),
                        key + " matched a sentence it has nothing to do with");
            }
        }
    }

    /**
     * A template that will not compile is dropped with a warning at startup, which is exactly the
     * silence this whole mechanism is meant to avoid, so the bundles are held to compiling.
     */
    @Test
    public void everyTemplateInTheBundleCompilesAndFormats() throws IOException {
        Properties english = bundle("patterns.properties");
        Properties chinese = bundle("patterns_zh_CN.properties");
        List<String> keys = templateKeys(english);
        assertFalse(keys.isEmpty(), "patterns.properties defines no templates");

        for (String key : keys) {
            String translated = chinese.getProperty(key);
            assertNotNull(translated, "patterns_zh_CN.properties has no " + key);
            assertNotNull(ProsePattern.of(key, english.getProperty(key), translated),
                    key + " cannot be turned into a matcher, so it would be ignored at runtime");
        }
    }

    /**
     * Some of these sentences are built around a clause rather than a name, and the clause is
     * assembled too. A template hands what it captured back through the whole chain so that a
     * clause with a template of its own is translated as well, rather than sitting in English in
     * the middle of a translated paragraph.
     */
    @Test
    public void aCapturedClauseIsTranslatedInItsOwnRight() {
        ProsePattern frame = ProsePattern.of("frame",
                "Once the fiducial is captured you can use it.%sJog nozzle %s over it.",
                "\u91c7\u96c6\u5b8c\u57fa\u51c6\u70b9\u540e\u5c31\u80fd\u7528\u5b83\u3002%1$s\u628a\u5439\u5634 %2$s \u70b9\u52a8\u5230\u5b83\u4e0a\u65b9\u3002");
        assertNotNull(frame);
        String translated = frame.apply(
                "Once the fiducial is captured you can use it.This also equalises Z.Jog nozzle N2 over it.",
                Map.of("This also equalises Z.", "\u8fd9\u4e5f\u4f1a\u5bf9\u9f50 Z\u3002"));
        assertEquals("\u91c7\u96c6\u5b8c\u57fa\u51c6\u70b9\u540e\u5c31\u80fd\u7528\u5b83\u3002"
                + "\u8fd9\u4e5f\u4f1a\u5bf9\u9f50 Z\u3002\u628a\u5439\u5634 N2 \u70b9\u52a8\u5230\u5b83\u4e0a\u65b9\u3002",
                translated, "the captured clause should have been translated too");
    }

    /** Every word a template may lift out has to be spelled the same in both files. */
    @Test
    public void everyVocabularyWordIsTranslated() throws IOException {
        Properties english = bundle("patterns.properties");
        Properties chinese = bundle("patterns_zh_CN.properties");
        int checked = 0;
        for (String key : english.stringPropertyNames()) {
            if (!key.startsWith(VOCABULARY_PREFIX)) {
                continue;
            }
            String translated = chinese.getProperty(key);
            assertNotNull(translated, "patterns_zh_CN.properties has no " + key);
            assertFalse(translated.isBlank(), key + " is translated to nothing");
            checked++;
        }
        assertTrue(checked > 0, "patterns.properties defines no vocabulary");
    }
}
