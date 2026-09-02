import java.util.List;
import java.util.Locale;

import org.openpnp.Translations;

/**
 * Run against the packaged jar and its lib directory, not the build tree: checks that the
 * dependency closure the manifest points at actually resolves, and that the translations really
 * did get packaged inside the jar.
 * <p>
 * Nothing here asserts a particular rendering. An expected translation written into a probe is a
 * snapshot that goes stale the next time anyone improves the wording, and then it reports a
 * failure that is not one. What is worth checking is the plumbing: that the bundles are in the
 * archive, that a key resolves, that prose keyed by its English is found, and that a string
 * nothing covers still comes back rather than disappearing.
 */
public class ReleaseCheck {

    static int bad = 0;

    static void expect(String what, Object actual, Object wanted) {
        boolean ok = wanted.equals(actual);
        if (!ok) {
            bad++;
        }
        System.out.printf("  %-40s %-30s %s%n", what, actual, ok ? "ok" : "FAIL want " + wanted);
    }

    static void loads(String className) {
        try {
            Class.forName(className);
            System.out.println("  loads  " + className);
        }
        catch (Throwable t) {
            System.out.println("  FAIL   " + className + " : " + t);
            bad++;
        }
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
        System.out.println("locale = " + Locale.getDefault());

        System.out.println();
        System.out.println("=== the dependency closure the manifest declares ===");
        loads("org.openpnp.Main");
        loads("org.opencv.core.Mat");
        loads("org.simpleframework.xml.core.Persister");
        loads("com.formdev.flatlaf.FlatLightLaf");
        loads("org.pmw.tinylog.Logger");
        loads("com.fazecast.jSerialComm.SerialPort");
        loads("org.jdesktop.beansbinding.Binding");
        loads("com.google.common.eventbus.EventBus");
        loads("bsh.engine.BshScriptEngineFactory");
        loads("org.python.util.PythonInterpreter");

        System.out.println();
        System.out.println("=== the bundles are in the archive ===");
        List<Locale> locales = Translations.getAvailableLocales();
        System.out.println("  discovered " + locales);
        expect("English is offered", locales.contains(Locale.US), true);
        expect("more than English was packaged", locales.size() > 1, true);
        expect("the running locale was packaged", locales.contains(Locale.SIMPLIFIED_CHINESE),
                true);

        System.out.println();
        System.out.println("=== the three lookups, each doing its own job ===");
        // A key the English bundle defines, so a missing bundle shows up as the !key! marker
        // rather than as silence.
        String keyed = Translations.getString(
                "AbstractActuatorConfigurationWizard.CoordinateSystemPanel.AxisLabel.text");
        expect("a key resolves", !keyed.startsWith("!"), true);
        System.out.println("    -> " + keyed);

        // Prose keyed by the English itself. Translated or not, it has to come back as prose.
        String prose = Translations.translateText("Convert to GcodeAsyncDriver.");
        expect("prose keyed by English comes back", !prose.isBlank(), true);
        System.out.println("    -> " + prose);

        // Nothing covers this one, and the point is that it survives the trip unchanged instead
        // of being dropped or marked up. It has to be obvious nonsense: prose that merely looks
        // like the application's is liable to match one of the patterns_<lang> templates, which
        // exist precisely to catch sentences assembled around a name or a number.
        String unknown = "Zork the flibbertigibbet at 42 parsecs.";
        expect("an uncovered string passes through", Translations.translateText(unknown), unknown);

        System.out.println();
        System.out.println("=== a format string, as the dialog would build it ===");
        String formatted = String.format(Translations.getString("PartsPanel.PartIdExists"), "R42");
        expect("the argument was substituted", formatted.contains("R42"), true);
        System.out.println("    -> " + formatted);

        System.out.println();
        System.out.println(bad == 0 ? "RELEASE OK" : bad + " PROBLEMS");
        System.exit(bad == 0 ? 0 : 1);
    }
}
