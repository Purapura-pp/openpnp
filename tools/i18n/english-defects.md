# English source defects found during the first translation round

Translators are not allowed to touch `translations.properties`, so they report what they find and
the integrator fixes it once every branch is back. Fixing it earlier would change the English
column of batches already generated in the other worktrees, and `import` would reject them whole.

Kept as a record rather than as a task list: a section headed "fixed" is history, and the reasoning
in it is the part worth reading, because most of these defects were invisible to every automated
check and were found by someone reading a panel. What is still outstanding is under **Still open**,
along with two entries left deliberately - the `RotationLabel` rename and the two-line gloss - that
say why.

## Reported by the Russian line

Spelling:

- `minmum` - GcodeDriverSettings lblMinSpeed, and the MotionPlanner MinimumSpeedLabel
- `practicle`
- `limts`
- `Preview PFS` - should be FPS
- `continous` - also in the key name `AllowContinousMotionLabel`
- `Fiduial`
- `dectection`
- `therfore`
- `execuction`
- `transparent tap` - should be tape

Grammar:

- `At least on of the two options need to be enabled`
- `in an GcodeAsyncDriver`
- `the size any feature which is below`
- `lower the the nozzle`
- `this moves take longer`
- `Placement are placed`
- `Check axis letters and make sure use a proper 6-axis configuration`
- `Axis should be treated as a linear for detected firmware`
- `Set to Rendering Quality to High` - one "to" too many
- `Can not take back`

Other:

- `Axis velocity limited by driver Maximum Feed Rate. ` - trailing space
- `SimulationModeMachineConfigurationWizard.lblDiscardPoint.toolTipText` describes an initial
  homing error offset and visual homing, which has nothing to do with a discard point. The tooltip
  looks attached to the wrong label.

Careful with the ones that live in the Solutions prose: those strings are the keys of the
`texts_<lang>` bundles and are hashed into `Solutions.Issue.getFingerprint`. Rewording one
invalidates the dismissals users have stored in machine.xml and kills every language's translation
of it silently. `i18n.py check` reports the dead entries afterwards, and every affected bundle has
to be re-keyed in the same commit.

## Reported by the Spanish line

Spelling: `Prerequsites`, `etcetra`, `practicle`, `limts`, `ommitted`, `atempting`, `meaningfull`,
`mutiple`, `therfore`, `transparent tap` (tape), `Fiduial Locator`, `continous`, `Scale Widt`,
`Allowed Misdectects`, `it's ID` (its), `panels(s)`, `with this the nozzle tip`, and the key name
`...MovableDefaultZTextFiled.toolTipText`.

Malformed HTML, which reflows or shows as text at runtime:

- `</hml>` for `</html>` in `ReferenceNozzleTipToolChangerWizard.ChangerPanel.FirstLocationLabel.toolTipText`
- missing space in `<strong>Locked</strong>to prevent cloning`
- no closing `</html>` in `CameraConfigurationWizard.LightPanel.AllowMachineActuatorsChkBox/Label.toolTipText`,
  `BlindsFeederArrayConfigurationWizard.lblUseVision.toolTipText` and
  `ReferencePushPullFeederConfigurationWizard.CloneFromTemplate.Confirm`
- a sentence cut off by a full stop before `<br/>including delays...` in
  `...PartOffVacuumSensingPanel.ValveOpenCloseLabel.toolTipText`

Also `GcodeDriverGcodes.Action.Import` and `.Import.Description` carry identical text, where the
description is supposed to explain the action.

## Found while reviewing

`PlacementsHolderLocationViewer.ViewingOption.Reticle.ToolTip` ends in a stray `</html>` with no
opening tag. Because `import` requires the markup to match, every language has faithfully copied
the stray tag, so this one has to be fixed in all bundles at once.

## Dead entries to clear out

Keys that no longer exist in the English bundle, so nothing reads them. Harmless, but they are the
only warnings `check` still emits, and clearing them makes a real regression visible next time:

- es: `JogControlsPanel.Label.OverrideBoardProtection`, the same with `.Description`,
  `JogControlsPanel.Tab.Dispense`
- ru: 19 of them, per that line's report

## A sentence assembled from fragments - fixed

`ExistingBoardOrPanelDialog` built its prompt as Leader + the word "board" or "panel" + Trailer.
Both the Spanish and French lines predicted independently that this could not survive a language
with grammatical cases, and they were right. Assembling the six bundles and reading the result
showed:

- ru: `Выберите плата из списка ниже...` - the verb needs the accusative, so the noun cannot stay
  in its dictionary form, and the trailing `найти его` is masculine where both nouns are feminine
- it: `Seleziona scheda dall'elenco...` - the article had to be dropped, because no single one
  agrees with both `scheda` and `pannello`, and `individuarne uno` does not agree with `scheda`
- zh_CN: stray spaces left around the noun, next to fullwidth punctuation

Spanish and French each got away with it only by finding a fixed noun to carry the agreement -
"Seleccione un archivo de placa", "Sélectionner un fichier carte" - which is a workaround, not a
solution, and would not have been available in every language.

Fixed by giving each case its own whole sentence, `Label.SelectOneBoardFile` and
`Label.SelectOnePanelFile`, and deleting the three fragments. English, Spanish, French and Chinese
are written; Russian and Italian were left to their own lines rather than guessed at.

The related defect - the file extension being built out of the translated word, which broke the
Browse filter in Chinese and Russian - was fixed earlier.

## Pre-existing defects in the Russian bundle, reported by the same line

- `Solutions.Milestone.Production.description` carries the Calibration description
- `Отлючен` should be `Отключен`

## The camera view popup menu - fixed

All four reported by the Italian line, all real.

- The crosshair reticle's Options menu listed Red, Green, Yellow, Orange, Blue, White, **Red**. Two
  radio buttons in one button group bound to the same colour, so the second could never be
  selected. The three reticle menus each carried their own copy of the colour list, which is how
  one extra line survived unnoticed; they share a single `addColorMenuItems` helper now, so the
  duplicate is gone by construction rather than by deletion.
- The colour names were passed as a `String name` argument to `createColorMenuItem` rather than
  written into a `JMenuItem` constructor, so neither `hardcoded` nor `externalize` could see them.
  The Color submenu stayed English in all six languages under a translated parent. Now keyed as
  `CameraViewPopupMenu.Color.Red` through `.White`.
- `CameraViewPopupMenu.chkMenuItem.text` and `.inputMenuItem.text` were named after their
  WindowBuilder variables rather than their text, which was "Filled" and "Size". Renamed. The
  `GENERATED_NAME` filter in `externalize` catches `menuItem` and `subMenu` but not `chkMenuItem`
  or `inputMenuItem`, which is worth widening before the next externalisation round.
- Found while fixing the above: `CameraViewPopupMenu.Reticle.EnterSize` interpolated
  `reticle.getUnits().toString().toLowerCase()`, the raw enum name, so every language asked for the
  size in "millimeters". It reuses the units labels the same menu already translates.

## Two labels that were wrong in English too - fixed

Both reported by the Russian line, and neither is a translator's problem: an English user sees them.

- `CameraConfigurationWizard.Action.ConfirmMeasure1` was `Measure`. `btnMeasure1` swaps its action
  when clicked, from `measure1Action` to `confirmMeasure1Action`, so the second press confirms what
  was measured - but both actions were labelled "Measure", so the button read the same before and
  after and gave no sign it was waiting. Its twin `ConfirmMeasure2` already said `Confirm`. Now
  `Confirm`, and each language takes its own existing `ConfirmMeasure2` rendering, the two English
  strings now being identical.
- `CameraVisionConfigurationWizard.VisionPanel.EdgeSensitiveLabel.text` held a verbatim copy of its
  own `.toolTipText`, so the sentence "Use the gradients of the images rather than brightness." sat
  where the sibling checkbox has the short question "Color Sensitive?". Now `Edge Sensitive?`.

Worth noting how these surfaced. The Russian bundle had already rendered both correctly -
«Подтвердить» and «Чувствительность к краям» - so the translator had silently repaired the English
years ago and the defect survived only in the source. Neither the consistency nor the collisions
report can see that shape: the translation is right and it is the English that is wrong, which no
check comparing the two can adjudicate. It took a translator reading the panel.

## Interpolating a label that is also a menu item

`CameraViewPopupMenu.Reticle.EnterSize` now reuses the translated units labels rather than the raw
enum name, which fixed "Enter the size in millimeters" appearing untranslated everywhere. The labels
are capitalised, because they are menu items, so the prompt now reads "…in Millimeters" with a
capital mid-sentence. German does not care, since it capitalises nouns anyway. French does, and the
French line restructured its own sentence so the interpolation lands after a colon - "Saisissez la
taille. Unité : %s" - which is the right answer: lowercasing in `unitsLabel()` would break German,
and a parallel set of lowercase unit keys is a lot of machinery for one prompt. Any language that
finds the capital conspicuous can restructure the same way with no code change.

## A label named after the wrong noun

`BottomVisionSettingsConfigurationWizard.RotationLabel.text` reads "Rotation", but the widget is
`lblMaxRotation` and it labels `comboBoxMaxRotation`, bound to `BottomVisionSettings.MaxRotation`
values. It selects a rotation *strategy*, not an angle. The consistency report flagged Chinese for
rendering it 旋转方式 where sixteen other Rotation labels say 旋转, and 旋转方式 turns out to be the
accurate one - the English is what needs renaming, to "Max. Rotation". Left alone for now because
renaming it retranslates the key in six bundles for a wording change, but every language currently
inherits the ambiguity.

## A gloss that no longer matches what it glosses

`ImageCameraConfigurationWizard.GeneralPanel.UnitsPerPixelLabel.text` and
`SimulatedUpCameraConfigurationWizard.GeneralPanel.UnitsPerPixelLabel.text` are both "Simulated
Units per Pixel". The Chinese two-line labels quote the English on their first line, and the second
one quoted it as "Units per Pixel" - dropping "Simulated", so the gloss contradicted the string it
was glossing. This is a hazard specific to the two-line style: the English is duplicated into the
translation, where nothing checks it against the source. Worth a `check` rule if the style spreads.

## Prose assembled around a value, and the 31 that were never even offered

Most of the Issues and Solutions wording is not a constant. `ActuatorSolutions` alone builds every
sentence it produces around a role and a name, and there are twenty-odd more like it. None of that
could ever be reached by a bundle keyed on the English, so it stayed English in every language.
That is now handled by templates in `patterns_<lang>.properties`, matched against the assembled
string at display time. The fingerprint is untouched, because only the display path translates.

Under that sat a smaller and more annoying problem. `whole_literals()` in `i18n.py` dropped any
argument containing a `+`, on the grounds that a concatenation is not knowable from the source. A
concatenation of *literals* is perfectly knowable, and these sources split long prose across lines
constantly, so 31 descriptions had been invisible to `gap` since the tooling was written. Nobody
had ever been offered them, in any of the six languages, and nothing reported them missing. The
manifest goes from 112 to 143 with that rule corrected.

The lesson worth keeping: a translation manifest recovered from sources is only as honest as its
parser, and the failure mode is silence in the direction that looks like success.

## Still open

`MainFrame.RightComponent.tabs.IssuesAndSolutionsHtml` holds half a tag:

    <html>Issues &amp; Solutions <span style="color:#

It only becomes valid HTML after `IssuesAndSolutionsPanel` concatenates a hex colour and a closing
`;">&#…;</span></html>` onto it at runtime. A translator cannot know that from a batch, and
`import`'s markup check cannot help, because the English is already unbalanced. It should be one
complete value with a `%s` for the colour. Left for a later round because it needs a change to how
that panel builds the label, and six bundles were in flight.

`ExistingBoardOrPanelDialog` is fixed, but the same shape survives elsewhere: four
`JobPlacementsPanel.Set*.MenuTip` keys all hold the English "Set selected placement(s) to", so four
different menus share one tooltip and none of them says what it sets. A translation cannot repair
that without inventing information the source does not carry, so the Chinese leaves all four
identical too.

## Dead entries, revisited

One is not merely dead but corrupt. `MachineSetup.JobProcessors.ReferencePnpJobProcessor.Label.DelayInfo`
in the Italian bundle ends with the whole of `JogControlsPanel.homeButton.toolTipText` spliced onto
its value, from a version that still had English words in it - a newline lost somewhere in the
file's history. The live `homeButton.toolTipText` is defined properly further down and is fine, and
`DelayInfo` is unreachable, so nothing shows it today. Checked across all six bundles: French has
the same key with a clean value, nobody else has it, so the splice is Italian-only and it is the
only corrupt value of the six. Being deleted rather than repaired, since a repaired value would be
equally unreachable.
