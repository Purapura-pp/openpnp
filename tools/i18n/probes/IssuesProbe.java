import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.Solutions;

/**
 * Enumerates the Issues and Solutions wording a given machine.xml produces, so the strings that
 * still come out English can be collected from a run rather than guessed at from the sources.
 * <p>
 * Run under a Chinese locale: anything printed without a CJK character in it is a string no bundle
 * covers, which is exactly the work list. Milestones are walked one by one because each opens a
 * different set of checks, and the configuration only ever sits at one of them.
 * <p>
 * Its output is what {@code i18n.py patterns --against} expects: which checks a machine runs
 * depends on what that machine has, so the only way to know what a configuration surfaces is to
 * read it off a running one and feed the list back in.
 */
public class IssuesProbe {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("usage: IssuesProbe <machine.xml> [outputFile]");
        }

        ReferenceMachine machine = Probes.load(Path.of(args[0]), "issues-probe");
        Solutions solutions = machine.getSolutions();

        Set<String> untranslated = new LinkedHashSet<>();
        List<String> report = new ArrayList<>();
        for (Solutions.Milestone milestone : Solutions.Milestone.values()) {
            solutions.setTargetMilestone(milestone);
            try {
                solutions.findIssues();
                solutions.publishIssues();
            }
            catch (Throwable t) {
                report.add("# " + milestone + " scan failed: " + t);
                continue;
            }
            int englishHere = 0;
            for (int i = 0; i < solutions.getIssues().size(); i++) {
                Solutions.Issue issue = solutions.getIssue(i);
                englishHere += collect(untranslated, issue.getIssue());
                englishHere += collect(untranslated, issue.getSolution());
                for (Solutions.Issue.Choice choice : issue.getChoices()) {
                    if (choice != null) {
                        englishHere += collect(untranslated, choice.getDescription());
                    }
                }
            }
            report.add(String.format("# %-12s rows=%-3d untranslated strings=%d",
                    milestone, solutions.getIssues().size(), englishHere));
        }

        report.add("");
        report.add("# " + untranslated.size() + " distinct strings no bundle covers");
        report.add("");
        report.addAll(untranslated);

        String text = String.join("\n", report);
        if (args.length > 1) {
            Files.writeString(Path.of(args[1]), text);
            System.out.println(untranslated.size() + " distinct untranslated strings -> " + args[1]);
        }
        else {
            System.out.println(text);
        }
    }

    /** Adds the string if it carries no Chinese, i.e. if nothing translated it. */
    private static int collect(Set<String> into, String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.UnicodeScript.of(value.codePointAt(i)) == Character.UnicodeScript.HAN) {
                return 0;
            }
        }
        into.add(value.replace("\n", "\\n"));
        return 1;
    }
}
