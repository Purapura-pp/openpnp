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
