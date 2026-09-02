# Translation tooling

Helpers for the workflow in [TRANSLATIONS.md](../../TRANSLATIONS.md). Python 3 only, no
dependencies. Run them from anywhere; paths are resolved relative to the repository.

```
python tools/i18n/i18n.py <command> -h
```

## What each command is for

`gap --lang de` lists what a language still needs, graded easy / medium / hard so that simple
labels can go to a different translator than HTML-heavy confirmation prose.

`batch --lang de --out build/i18n-batches` writes that gap out as small tab separated files.
This is how translation work is handed out: the translator fills in the third column and never
opens a `.properties` file, so escaping, sort order, encoding and line endings stay with the
tool.

`import --lang de --batch build/i18n-batches/translations-de-001.tsv` validates a filled in
batch and merges it. The whole batch is rejected if anything fails, rather than partly applied -
picking the good rows out of a bad batch costs more than translating it again. It checks that
the key column came back unchanged, that the format arguments and the markup still match the
English, and that nothing contains a stray tab. A row whose translation is `SKIP` is left in
English deliberately.

`check` is the gate. It reports as errors only the things a user would see go wrong - a crash
from mismatched format arguments, a translation dropped by a duplicate key, HTML tags printed as
text - and as warnings the dead entries that cost nothing at runtime. It exits non-zero only on
errors, so it can stay green in CI.

`normalize --lang de` rewrites a bundle the way `Properties.store` would. The English bundle has
to be in that form or `LocalisationTest` fails; the others do not have to be, but staying in it
keeps diffs small.

`hardcoded` counts the English still baked into the Java sources, which no bundle can reach.
That is work for the externalisation role, not for a translator.

## The probes, and why they are not tests

`probes/` holds three programs that read wording off an OpenPnP that has actually started, which is
the only way to see the sentences the sources assemble rather than store. They are not part of the
build: they use `sun.misc.Unsafe` to get a `MainFrame` without a display, and they run against the
packaged jar. Compile them against a `package` build:

```
mvn -o package -DskipTests
javac -nowarn -cp "target/openpnp-gui-0.0.1-alpha-SNAPSHOT.jar;target/lib/*" \
      -d build/probes tools/i18n/probes/*.java
```

and run with the same `--add-opens` flags `openpnp.bat` uses, with `build/probes` first on the
class path.

`IssuesProbe <machine.xml> [out]` walks every milestone of Issues and Solutions for that machine
and lists the strings that came back with no CJK in them, i.e. that nothing translated. Its output
is what `patterns --against` wants:

```
java ... -Duser.language=zh -Duser.country=CN IssuesProbe src/main/resources/config/machine.xml build/probes/issues-zh.txt
python tools/i18n/i18n.py patterns --against build/probes/issues-zh.txt
```

Which checks a machine runs depends on what that machine has, so `patterns` on its own can only
speak for the templates against the sources. Feeding it a real run is how you find out what a
configuration actually surfaces.

`ShowIssues <machine.xml> <out>` dumps all the rows as the panel would show them, translated ones
included. `IssuesProbe` answers "what is still English"; this answers "what does it actually say",
which is what a review pass reads. Run it once per locale and read the two files side by side.

`ReleaseCheck` runs against a packaged jar and checks that the dependency closure the manifest
declares really resolves and that the bundles got into the archive. It deliberately asserts no
particular rendering: an expected translation written into a probe is a snapshot that goes stale
the next time someone improves the wording, and then reports a failure that is not one.

## Two things that are easy to get wrong

**A dotted key is not display text.** `MessageBoxes.errorBox(this, Translations.getString("JobPanel.SaveJob.Error"), ...)`
contains a string literal, but it is a lookup key. Counting it as untranslated English inflated
an early survey by about a third. Every scan here masks `getString` arguments first, and
`hardcoded` additionally requires a space in the string.

**Not every literal in the source is reachable as a translation.** The `texts_<lang>` bundle is
keyed by the English prose itself, so only a literal that forms a *whole* argument can ever
match at runtime. `"Axis letter " + letter + " is not valid"` produces something at runtime that
appears nowhere in the source. The extractor therefore parses the argument list rather than
matching lines: a bare literal counts, a ternary between two literals counts as both branches,
and anything joined with `+` counts as nothing.
