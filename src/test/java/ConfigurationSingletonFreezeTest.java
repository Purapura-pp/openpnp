import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Holds the line on the Configuration singleton: the calls that exist may go away, new ones may not
 * appear.
 * <p>
 * {@code Configuration.get()} reaches process-wide mutable state from anywhere, which is how several
 * hundred calls accumulated across a quarter of the source files without anyone deciding to put them
 * there. Two consequences follow from that reach, and neither one shows up as a failing test. A
 * class that needs the machine, the system units or the part list does not have to say so in its
 * constructor, so its dependencies are invisible to the next reader and beyond the reach of a test
 * that wants to supply its own. And because the instance is static and never cleared, tests sharing
 * a JVM see whatever the class that ran before them left behind.
 * <p>
 * Removing the calls in one change is not possible, so this freezes the count instead. The baseline
 * file records how many calls each file is allowed to make. Refactoring lowers a number; nothing
 * raises one.
 * <p>
 * Comments and string literals do not count, because the numbers are meant to be the calls the
 * program really makes: commenting a call out is progress, and naming one in a comment - as the
 * explanation of a call that was removed usually has to - is free.
 */
public class ConfigurationSingletonFreezeTest {
    private static final Path SOURCES = Paths.get("src/main/java");

    private static final Path BASELINE =
            Paths.get("src/test/resources/configuration-get-baseline.txt");

    /** Spaced out the way the compiler sees it, so that reformatting cannot hide a call. */
    private static final Pattern CALL =
            Pattern.compile("Configuration\\s*\\.\\s*get\\s*\\(\\s*\\)");

    @Test
    public void noFileGainsACallOnTheSingleton() throws Exception {
        Map<String, Integer> counted = countCalls();
        Map<String, Integer> allowed = readBaseline(counted);

        List<String> exceeded = new ArrayList<>();
        for (Map.Entry<String, Integer> file : counted.entrySet()) {
            int cap = allowed.getOrDefault(file.getKey(), 0);
            if (file.getValue() > cap) {
                exceeded.add(String.format("  %s calls it %d times, %d allowed", file.getKey(),
                        file.getValue(), cap));
            }
        }
        if (exceeded.isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append("New calls on the Configuration singleton:\n");
        for (String line : exceeded) {
            message.append(line).append("\n");
        }
        message.append("\nA call reaches process-wide mutable state, so it is a dependency the "
                + "constructor does not declare and a test cannot replace. Three ways out, in the "
                + "order they usually apply:\n"
                + "  - a machine element - nozzle, camera, axis, feeder, driver, signaler - reaches "
                + "its machine through its own references, either getHead().getMachine() or the "
                + "getMachine() that AbstractMachine fills in while loading;\n"
                + "  - a ConfigurationListener callback is handed the configuration as its "
                + "argument, so take it from there rather than from the singleton;\n"
                + "  - a GUI class takes what it needs through its constructor. MainFrame is built "
                + "with the Configuration and hands it to the panels it creates.\n");
        message.append(regenerated(counted,
                "If a call really is unavoidable, raise the number by copying"));
        fail(message.toString());
    }

    /**
     * Keeps the baseline honest about the files that are actually there. A file that was renamed
     * fails the test above anyway, under its new name; requiring the old name to go too is what
     * keeps the total from counting work that has already been done.
     */
    @Test
    public void theBaselineNamesOnlyFilesThatStillCallIt() throws Exception {
        Map<String, Integer> counted = countCalls();
        Map<String, Integer> allowed = readBaseline(counted);

        List<String> stale = new ArrayList<>();
        for (Map.Entry<String, Integer> file : allowed.entrySet()) {
            if (!counted.containsKey(file.getKey())) {
                stale.add(String.format("  %s is allowed %d and makes none", file.getKey(),
                        file.getValue()));
            }
        }
        if (stale.isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append("The baseline still allows calls in files that no longer make any:\n");
        for (String line : stale) {
            message.append(line).append("\n");
        }
        message.append("\nThat is either work already finished or a file that was renamed or "
                + "deleted. Either way the entry has to go, so that the remaining total is the "
                + "work that is actually left.\n");
        message.append(regenerated(counted, "Take the corrected list from"));
        fail(message.toString());
    }

    /**
     * The counts as they stand, keyed by the path below src/main/java. Files that make no call are
     * left out, so that adding a source file is not by itself a reason to touch the baseline.
     */
    private static Map<String, Integer> countCalls() throws IOException {
        assertTrue(Files.isDirectory(SOURCES),
                SOURCES.toAbsolutePath() + " was not found. This test reads the sources, so it has "
                        + "to run with the project root as the working directory.");
        Map<String, Integer> counts = new TreeMap<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            files.filter(path -> path.getFileName().toString().endsWith(".java")).forEach(path -> {
                int calls = countCalls(code(read(path)));
                if (calls > 0) {
                    counts.put(SOURCES.relativize(path).toString().replace('\\', '/'), calls);
                }
            });
        }
        assertTrue(!counts.isEmpty(), "not one call was found in " + SOURCES.toAbsolutePath()
                + ", which means this test is no longer reading the sources it thinks it is");
        return counts;
    }

    private static int countCalls(String code) {
        Matcher matcher = CALL.matcher(code);
        int calls = 0;
        while (matcher.find()) {
            calls++;
        }
        return calls;
    }

    private static Map<String, Integer> readBaseline(Map<String, Integer> counted)
            throws IOException {
        if (!Files.isRegularFile(BASELINE)) {
            // Also the way to tighten the numbers after a round of refactoring: delete the file,
            // run this once, and commit what it writes.
            write(BASELINE, format(counted));
            fail("There was no baseline at " + BASELINE + ", so one was written from the calls "
                    + "that are there now. Read it, and commit it if it looks right.");
        }
        Map<String, Integer> allowed = new TreeMap<>();
        for (String line : Files.readAllLines(BASELINE, StandardCharsets.UTF_8)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] columns = line.split("\t");
            assertTrue(columns.length == 2, "this line of " + BASELINE
                    + " is not a path and a count separated by a tab: " + line);
            allowed.put(columns[0], Integer.parseInt(columns[1].trim()));
        }
        return allowed;
    }

    /**
     * Writes the list as it would have to read to pass, next to the baseline, and returns the
     * sentence that points a reader at it.
     */
    private static String regenerated(Map<String, Integer> counted, String lead) throws IOException {
        Path updated = Paths.get(BASELINE + "-updated");
        write(updated, format(counted));
        return lead + ":\n  " + updated + "\n";
    }

    private static String format(Map<String, Integer> counts) {
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        StringBuilder text = new StringBuilder();
        text.append("# Every file under src/main/java that calls Configuration.get(), and how many\n");
        text.append("# times it is allowed to. ConfigurationSingletonFreezeTest reads these as upper\n");
        text.append("# bounds: a file may call it fewer times than it says here, never more, and a\n");
        text.append("# file that is not listed may not call it at all.\n");
        text.append("#\n");
        text.append("# ").append(total).append(" calls in ").append(counts.size()).append(" files.\n");
        for (Map.Entry<String, Integer> file : counts.entrySet()) {
            text.append(file.getKey()).append("\t").append(file.getValue()).append("\n");
        }
        return text.toString();
    }

    private static void write(Path path, String text) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Files.write(path, text.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The source with its comments and literals taken out, so that a call is counted only where the
     * program really makes one. Java's own lexical rules decide what a comment is: the "//" inside
     * a string, which every URL in the source contains, does not start one.
     */
    private static String code(String source) {
        StringBuilder code = new StringBuilder(source.length());
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '/' && i + 1 < source.length()) {
                char next = source.charAt(i + 1);
                if (next == '/') {
                    while (i < source.length() && source.charAt(i) != '\n') {
                        i++;
                    }
                    continue;
                }
                if (next == '*') {
                    int end = source.indexOf("*/", i + 2);
                    i = (end < 0 ? source.length() : end + 2);
                    continue;
                }
            }
            if (c == '"' || c == '\'') {
                i = endOfLiteral(source, i);
                continue;
            }
            code.append(c);
            i++;
        }
        return code.toString();
    }

    private static int endOfLiteral(String source, int start) {
        char quote = source.charAt(start);
        if (quote == '"' && source.startsWith("\"\"\"", start)) {
            int end = source.indexOf("\"\"\"", start + 3);
            return (end < 0 ? source.length() : end + 3);
        }
        int i = start + 1;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            // A newline inside a literal means the source does not compile. Stopping here keeps a
            // stray quote from swallowing the rest of the file and reporting zero calls.
            if (c == quote || c == '\n') {
                return i + 1;
            }
            i++;
        }
        return i;
    }
}
