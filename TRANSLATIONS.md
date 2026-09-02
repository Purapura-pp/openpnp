There are many ways to do translations.

## Using Eclipse

If you already followed the developer guide and installed Eclipse and WindowBuilder, you can 
translate directly on the dialog editor or use the built-in "Externalized strings" tabular 
editor throughout OpenPnP. 

Be sure to follow the Wiki:
https://github.com/openpnp/openpnp/wiki/Getting-Started-with-Eclipse#translations-in-eclipse

## Standalone i18n Editor
Alternatively, if you don't want to use Eclipse, there is a standalone editor for existing entries:

https://github.com/VSSavin/i18n-editor can be used to edit the translations files.

To import the OpenPnP files:

1. Download and run the tool.
1. Drag the folder openpnp/src/main/resources/org/openpnp into the tool's main window.
1. Set the encoding to UTF-8 in the Settings menu.
1. Click keys to edit their text.
1. Add a new language by selecting Edit -> Add Locale. See
   https://www.oracle.com/technetwork/java/javase/java8locales-2095355.html for a list of
   language codes.

Note: There is an issue where a parent of a nested key is not editable in the tool

OpenPnP picks your system language the first time it runs, if a translation for it exists, and
English otherwise. You can change it under View -> Language; the change takes effect on the next
start.

---

# Working on a translation

The rest of this document is for organised translation work, where several people or sessions
translate different languages at the same time. Everything below is language independent: to
hand out a language, point someone at this document and tell them the language code.

## Adding a language

Create `src/main/resources/org/openpnp/translations_<code>.properties` and start filling it in.
That is the whole procedure. The View -> Language menu is built from the translation files that
are present, so the new language appears as soon as the file does, and a partially translated
file is fine because any key it does not define falls back to English.

Use the language code on its own - `sv`, `pl`, `ja` - and add a country only when the language
genuinely differs by region, as with `zh_CN` and `pt_BR`. Getting the code wrong is the one
mistake that produces no error at all: the file is simply never loaded and everything stays
English. The smoke check below is what catches it.

## Two roles, and why they must not be mixed

**Translating** touches exactly one file, `translations_<code>.properties`, plus
`texts_<code>.properties` if you take that on as well. Nothing else. Any number of languages can
therefore be worked on at the same time without conflict.

**Externalising** is the separate job of moving English that is still hardcoded in the Java
sources into the English bundle so that it becomes translatable at all. It edits Java sources and
`translations.properties`, it is language independent, and two people doing it at once will
collide. It is a single person's job, on its own branch.

Run `python tools/i18n/i18n.py hardcoded` to see what is left of it.

## Isolating parallel work

File ownership is not enough on its own: sessions sharing one checkout will sweep each other's
half-finished edits into their commits. Give each language its own worktree and branch.

```
git worktree add ../openpnp-sv i18n/sv
```

- A translation branch contains commits touching only that language's files.
- Externalisation lives on its own branch, and is the only branch allowed to change Java sources
  or the English bundle.
- Do not edit `CHANGES.md`. Language branches never conflict with each other, and keeping the
  shared file out of them is what guarantees that. Whoever integrates a round writes one entry
  for it.
- Commit messages read `feat: translate <area> into <language>`.

Translation proceeds in rounds. A round takes the English bundle as it stands, everyone
translates against that, and the round is integrated. Externalisation runs alongside and its new
keys go into the next round, so translators are never chasing a moving baseline. There is no need
to wait for it, and there was none in the first round, when German, French, Spanish and Italian
were each missing over 2500 of the existing keys.

## Handing work to a translator

Translators do not edit `.properties` files directly. The reason is not politeness, it is that
the file format has several ways to fail silently - an unescaped separator, a real newline where
`\n` was meant, a duplicate key that quietly overrides the earlier one - and none of them produce
an error. Everything structural stays in the tool.

```
python tools/i18n/i18n.py gap   --lang sv
python tools/i18n/i18n.py batch --lang sv --out build/i18n-batches --size 50
```

That writes tab separated batches with three columns: key, English, and an empty column to fill
in. The translator fills in the third column and nothing else. Then:

```
python tools/i18n/i18n.py import --lang sv --batch build/i18n-batches/translations-sv-001.tsv
```

The import validates the batch and **rejects all of it** if anything is wrong, rather than
applying the good rows. Picking usable rows out of a bad batch costs more than redoing it. It
requires that:

- the key column comes back exactly as it was sent, which is what catches a translator who
  "translated" a key;
- the format arguments still match. `"%s and %s"` may become `"%2$s och %1$s"`, because that
  consumes the same two arguments, but losing one of them would throw
  `MissingFormatArgumentException` in front of a user;
- the markup still matches, so a dropped `<br/>` or `\n` cannot silently reflow a dialog;
- no cell contains a tab.

Before handing out the first batch, run

```
python tools/i18n/i18n.py reuse --lang sv --apply
```

The bundles repeat themselves heavily - the same explanation of "Rotation in Tape" sits under four
keys, "Feed Count" under six - and `reuse` fills any entry whose English is already translated
under another key. It only fills English that is unambiguous: where two keys share the English but
were translated differently, it leaves the entry alone rather than picking one. Running it first
keeps the translator from paying for the same sentence twice, and keeps the copies from drifting
apart.

A row whose translation is `SKIP` is left in English on purpose. That exit matters: an
untranslated string is harmless because it falls back to English, whereas a guessed translation
of something the translator did not understand is not. Prefer `SKIP` to guessing.

`gap` grades every entry easy, medium or hard from its length, its argument count and whether it
contains HTML. Short labels and column headings are a different job from multi-paragraph
confirmation prose, and can be given to a different translator. Everything in `texts_<lang>` is
at the hard end: those are Issues and Solutions descriptions, and a mistranslation there misleads
someone operating a real machine.

Give each batch the terminology block from the top of the language's file, and a handful of
already translated entries from the same part of the interface. Consistency comes much more
easily from examples than from instructions.

## The three mechanisms, and why one has a strange rule

Almost everything goes through `Translations.getString("Some.Key")` and lives in
`translations_<lang>.properties`, keyed by an identifier. That is the ordinary case.

The Issues and Solutions descriptions work differently. They go through
`Translations.translateText(englishProse)` and live in `texts_<lang>.properties`, **keyed by the
English text itself**. The reason is in the comment on `Translations.translateText`: that English
wording is also hashed into `Solutions.Issue.getFingerprint`, which is what `machine.xml` stores
to remember the issues a user has dismissed or marked as solved. Turning the descriptions into
keys would invalidate every one of those records, and translating the fingerprint input would
make them shift every time the display language changed.

So in that file, **the key is English and must stay exactly as it is**. Only the value is
translated. If you change a key there, that entry stops matching and the translation silently
disappears.

The same mechanism has a maintenance cost worth knowing about: if a developer rewords the English
by so much as a word, the entry no longer matches and the translation goes dead with no error
anywhere. `i18n.py check` reports those as dead entries, and it is the only way they are found.

Note that a description the source splits across several lines is still one constant, and it is
offered for translation like any other. Only a description built around a *value* is not.

## The wording the source assembles, and how it is translated anyway

A great many of those descriptions are not constants at all:

```java
holder.getSubjectText()+" is missing a "+qualifier+" actuator.",
```

That is a different string on every machine, so no key made of the English can ever reach it. Those
go through the third mechanism: `patterns_<lang>.properties`, which holds templates keyed by an
identifier the way `translations` is.

```
ActuatorSolutions.Missing=%s is missing a %s actuator.
```

`translateText` tries the whole-string lookup first. On a miss it matches the string against these
templates, lifts out whatever the placeholders covered, and formats them into the translation. The
fingerprint is untouched by any of this: `getFingerprint` reads the raw fields, and only
`getIssue`, `getSolution` and `Choice.getDescription` translate.

Four things are worth knowing before adding a template:

- **The English side must read exactly as the source assembles it.** Reword the source and the
  template stops matching, silently. `i18n.py patterns` checks each template's literal runs are
  still somewhere in the Java, which catches most of it.
- **Use `%1$s` indices in the translation whenever the sentence wants the values in another order.**
  `Automatically calibrates the camera %s using the nozzle %s.` becomes
  `用吸嘴 %2$s 自动标定相机 %1$s。`
- **A template needs one run of six literal characters.** Without something distinctive it would
  match far more than it was written for, so the runtime refuses to load it and says so in the log.
- **Keys under `Word.` are not templates.** They are the English words a template may lift out -
  `pump control`, `primary` - as opposed to the names a user gave their own nozzles and cameras,
  which have no entry and so pass through untouched. `i18n.py patterns` reports an actuator role
  the sources use that has no `Word.` entry.

When two templates describe the same string, the one resting on more literal text wins, so
`The %s actuator %s has no driver assigned.` beats `The %s actuator %s has no %s assigned.` and the
reader is told what is missing. `i18n.py patterns --against <file>` reports any string that two
templates both claim, along with any that nothing covers:

```
python tools/i18n/i18n.py patterns --against untranslated.txt
```

Feed it the strings a real machine produced. Reading them off a running machine is the only way to
know what a given configuration actually surfaces; the sources alone will not tell you, because
which checks run depends on what the machine has.

## What not to translate

- Class and implementation names: `GcodeDriver`, `ReferenceNozzle`, `ContactProbeNozzle`,
  `BlindsFeeder`.
- G-code, command bytes, regular expressions, property names.
- Log messages. They stay English so that whoever ends up reading a log to help someone can read
  it. `Solutions` deliberately logs its untranslated wording for this reason.
- The keys in `texts_<lang>.properties`, as above.
- Terms your language decides to keep in English. Record the decision in the terminology block
  rather than leaving it to the next person to rediscover.

## Terminology

Keep a comment block at the top of your language's file listing the terms you have settled on and
the ones you are deliberately leaving in English. The file belongs to your language alone, so this
is conflict free, and it is the first thing the next translator sees.

`translations_zh_CN.properties` has one to copy the shape of. Chinese keeps the English term and
adds a gloss - `像素尺寸(Units Per Pixel)` - so that a user can still match what they see against
the English wiki and screenshots. For field labels the same idea is done with a two-line HTML
label. Another language may reasonably choose differently; what matters is choosing once and
writing it down.

## Run this before you trust a consistency report

```
python tools/i18n/i18n.py unused
```

A class can be deleted upstream while its keys stay behind in every bundle. Nothing breaks, because
nothing looks them up, so they sit there indefinitely - `BoardLocationsTableModel`,
`PlacementsTableModel` and `PanelFiducialsTableModel` were each replaced by a `PlacementsHolder*`
equivalent and left forty-six keys between them.

They are not harmless, and the damage is specifically to the report below. Consistency groups keys
by their English text, so a stale key carrying an older rendering is indistinguishable from a live
disagreement. Four of the sixty-nine Simplified Chinese findings were nothing but that, and one of
them was convincing enough to send a reviewer looking for a Length/Width mix-up that did not exist:
the live keys all said 纵向宽度, and the lone 长度 belonged to a table that no longer had a class.

Keys are also assembled at run time, so a key with no literal occurrence is only a candidate; the
command treats every dotted prefix that appears as a literal as a possible builder and leaves
matching candidates alone. It still pays to confirm a surprising one by hand - search for the key in
quotes, since `"PlacementsTableModel.ColumnName.Part"` is a bare substring of
`"PlacementsHolderPlacementsTableModel.ColumnName.Part"` and an unquoted grep will tell you it is
alive.

When you delete, delete from all seven bundles. A key removed from English alone becomes an orphan
that `check` then complains about.

## When one word comes out two ways

`check` finds the things that break at runtime. It cannot find a label that simply reads
differently on two panels, because each reading is correct on its own; only the comparison across
keys shows it. That is what this reports:

```
python tools/i18n/i18n.py consistency --lang sv
```

It lists short English strings that your language renders more than one way, and it is worth a look
before you call a language finished.

Note what it cannot do. The Italian bundle had the word for "delete" on all thirty of its Cancel
buttons; the report said nothing, because the thirty agreed with each other. A consistency check
finds disagreement, and a mistake applied everywhere is perfectly consistent. That one was caught by
reading a sample of entries against their English. Run both.

Expect findings you should keep, and say so in the commit rather than flattening them. The Chinese
bundle uses two different words for Nozzle and Nozzle Tip, which is a distinction the English makes
only by wording; several languages render `Cameras` differently for the bottom cameras and the head
cameras. Where the English is one word for two different objects, two renderings is the right
answer. Where it is one word for one object, they should match, and the majority form usually wins.

Read the Java before you decide, because the report cannot tell you which reading is right and the
majority is not always it. Six of the sixty-nine Chinese findings looked like defects and were not.
`Width` and `Length` render as 横向长度 and 纵向宽度 because those columns read X and Y respectively,
which the English names get backwards. The label that said 旋转方式 where sixteen others said 旋转 sits
on `comboBoxMaxRotation` and selects a strategy rather than an angle, so the odd one out was the
accurate one and the English is what wants renaming. All eight `Solutions.Milestone.*.name` values
are phases, so a milestone reading "进行校准阶段" against a panel's "校准" is the pattern, not an
outlier. Twenty minutes with `git grep` saved four wrong "fixes" in that round.

Of the sixty-nine, twenty-six survive on purpose. That is a normal ratio once a language is mature,
and the ones you keep are worth writing down by key, with the reason - otherwise the next pass
rediscovers them and someone flattens them from the report alone.

Long values are left out on purpose: a sentence varies with its surroundings, and reporting those
would bury the labels that matter. Raise `--max-len` if you want to see them anyway.

## When two things come out one way

The inverse, and the worse of the two:

```
python tools/i18n/i18n.py collisions --lang sv
```

This reports one rendering standing for several distinct English strings. An inconsistency leaves a
user unsure whether two labels mean the same thing. A collision leaves them looking at two controls
that do different things under one label, with nothing on screen to tell them apart.

The Italian bundle had the five jog increment buttons rendered as amounts rather than ordinals, and
`Second` and `Fourth` both came out "Incrementa di Quattro": two adjacent buttons in one panel,
identical label. The consistency report cannot see that, because those two keys hold different
English and are entitled to differ.

Read the key names, not just the values. A collision between two unrelated wizards is usually
harmless, because the two labels are never on screen together. A collision inside one panel is
nearly always a bug. The other kind worth hunting is a rendering that is right for one of its keys
and simply wrong for the other, which is how a nozzle Settings panel was found carrying the nozzle
tip's "Pick & Place" title in Chinese.

Most collisions are legitimate and are left alone. English is compared in a reduced form that drops
plurals, case and trailing punctuation, because Chinese marks no plural and has to land `Offset` and
`Offsets` on one word; reporting those would bury the rest. Beyond that, English often distinguishes
where your language should not, and forcing a difference to quiet the report makes the translation
worse. Write the ones you keep into the terminology block.

## When Java has to build the sentence

Some strings are glued to a value at runtime: `Center Camera on ` gets a location name appended,
`CameraViewPopupMenu.Reticle.EnterSize` interpolates a unit. Both hand you a word you do not
control, in a form you did not choose. The unit label is capitalised because it doubles as a menu
item, and the location name arrives in the nominative because it is one too.

English survives this because it inflects almost nothing and does not mind a capital mid-sentence.
Russian and French both do mind. The fix that works without touching Java is to restructure your
sentence so the interpolation lands after a colon, where any reader takes a capitalised word as a
quoted value:

    Введите размер. Единица: %s
    Saisissez la taille. Unité : %s

Lowercasing the label in Java would be wrong, because German capitalises nouns and needs the
capital. A parallel set of lowercase keys is a lot of machinery for two prompts. Restructuring
costs nothing and is a translator's decision. Write it into your terminology block, because it
looks like an oversight and the next person will otherwise "fix" it back.

The same reasoning is why `ExistingBoardOrPanelDialog` no longer assembles its prompt from a
leader, a noun and a trailer. Where Java concatenates, prefer one key holding a whole sentence.

## A leading space is not what you typed

`Properties.load` throws away unescaped whitespace after the separator, so

    SomeLabel.text= Placed:

reaches the program as `Placed:` and the padding you wanted is gone. To keep it, escape it:

    SomeLabel.text=\ Placed:

`check` reports the unescaped form as an error. It has to, because the space is invisible in the
file and does nothing at runtime, so it looks deliberate and correct to everyone who reads it
afterwards - two status bar counters had been losing their padding for years.

## Before you commit

```
python tools/i18n/i18n.py check --lang sv
python tools/i18n/i18n.py consistency --lang sv
python tools/i18n/i18n.py collisions --lang sv
mvn -o test
```

`check` reports as errors only the things a user would actually see go wrong, so it should be
green. It separately lists dead entries, which are harmless leftovers rather than something you
introduced.

Then confirm the language really loads. This is the step that catches a misnamed file, and it
cannot be skipped, because a file that is never loaded looks exactly like a file that has not been
translated yet:

- start OpenPnP, choose the language under View -> Language, restart, and look at the interface.

Two things to know about the test suite. `LocalisationTest` only checks the English bundle, and it
requires the exact escaped form that `Properties.store` writes - `!`, `=` and `:` all escaped,
including inside HTML attributes such as `<span color\="red">`. If you have edited the English
bundle, run `python tools/i18n/i18n.py normalize --lang en`. Do not copy the `-normalised` file
that the test writes out: it is produced with the platform default encoding, so on a non-English
Windows it will corrupt every non-ASCII character in the file.

And if your machine's default language is not English, a test that asserts on English interface
text will fail for you. The known ones use `Solutions.Issue.getUntranslatedIssue()`; anything new
of that kind should do the same.

## When the English is what is wrong

A translator reading a panel finds things no comparison of the two languages can: a spelling
mistake, a tooltip attached to the wrong label, a button that reads the same before and after it
is pressed. Those are defects an English user sees too, and they are not the translator's to fix -
editing `translations.properties` while batches are out in other branches changes the English
column those batches were generated from, and `import` rejects a batch whose English moved.

So they get reported and the integrator fixes them once the branches are back.
[tools/i18n/english-defects.md](tools/i18n/english-defects.md) is the record from the first round,
including what is still open. Read the reasoning before adding to it: rewording a string that
belongs to the Issues and Solutions prose also changes `Solutions.Issue.getFingerprint`, which
invalidates the dismissals users have stored in their machine.xml and silently kills every
language's translation of it.

## Where the work stands

`python tools/i18n/i18n.py gap --lang <code>` is the current answer for any language. All six -
Simplified Chinese, Russian, German, Spanish, French and Italian - cover all 2650 English keys.

The Issues and Solutions descriptions are further along in Simplified Chinese than elsewhere. The
manifest is 143, not the 112 it read for a long time: `whole_literals()` used to drop any argument
containing a `+`, which threw away 31 descriptions the sources merely split across source lines.
They were never offered to anybody, in any language. Chinese has taken them on and has the 21
templates for the assembled wording as well; the other five languages will report those as missing
until someone picks them up, which is the truth rather than a regression.

Consistency has been through a second pass. The counts now read 1 for French, 2 for German, 3 for
Russian, 4 for Spanish, 5 for Italian and 26 for Simplified Chinese, every one of them recorded as
deliberate with its reason. Chinese started at sixty-nine, an order of magnitude above the rest, for
a reason that has nothing to do with the language: it was translated first and by the most hands, so
it accumulated the most disagreement about wording. Expect the same of whichever language ends up
carrying the most contributors.

A language being complete now means what it says. The English that used to be hardcoded in the
configuration wizards, the dialogs and the camera view popup menu has been externalised, so there
is no longer a body of English sitting outside the bundles waiting to appear in a translated
interface. `i18n.py hardcoded` reports what remains, which is mostly log and exception wording that
stays English on purpose.

`gap` also counts entries whose translation is identical to the English, and that number varies far
more than it looks like it should: 131 in French against 19 in Russian. It is not a measure of work
left undone. French shares a great deal of vocabulary with English - Rotation, Configuration,
Vision, Distance, Machine, Saturation, Options and Description are all correct French - so a high
count in a language with Latin roots is expected. Read the list before concluding anything from the
number.
