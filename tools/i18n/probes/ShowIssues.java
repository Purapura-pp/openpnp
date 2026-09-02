import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.Solutions;

/**
 * Prints the Issues and Solutions rows as the panel would show them, so a translation pass can be
 * read rather than merely counted.
 * <p>
 * {@link IssuesProbe} answers "what is still English"; this answers "what does it actually say",
 * which is the only way to review wording that the sources assemble. Run it once per locale and
 * read the two files side by side.
 */
public class ShowIssues {
    private static final int WIDTH = 150;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("usage: ShowIssues <machine.xml> <outputFile>");
        }

        ReferenceMachine machine = Probes.load(Path.of(args[0]), "show-issues");
        Solutions solutions = machine.getSolutions();
        solutions.setTargetMilestone(Solutions.Milestone.Advanced);
        solutions.findIssues();
        solutions.publishIssues();

        List<String> out = new ArrayList<>();
        for (int i = 0; i < solutions.getIssues().size(); i++) {
            Solutions.Issue issue = solutions.getIssue(i);
            out.add("[" + issue.getSubject().getSubjectText() + "]");
            out.add("   issue    " + oneLine(issue.getIssue()));
            out.add("   solution " + oneLine(issue.getSolution()));
        }
        Files.writeString(Path.of(args[1]), String.join("\n", out));
        System.out.println(solutions.getIssues().size() + " rows -> " + args[1]);
    }

    private static String oneLine(String value) {
        String text = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return text.length() > WIDTH ? text.substring(0, WIDTH) + " ..." : text;
    }
}
