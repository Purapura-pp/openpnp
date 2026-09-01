package org.openpnp.gui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.model.DisplayPreferences;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;

/**
 * Covers the length field of the configuration wizards, from the text a user types through to the
 * Length that reaches the model, and back.
 * <p>
 * The conversion itself is thin, but it sits on {@link Length#parse}, whose behaviour at the edges
 * decides what happens when someone mistypes a unit in a field that positions a real machine.
 * <p>
 * The units are supplied here rather than taken from the running configuration. They used to be
 * read from the user's own preferences, which meant the test wrote to them and had to put them
 * back, and a case that needed inches for one assertion was changing a setting for whatever ran
 * next in the same JVM.
 */
public class LengthConverterTest {
    private static final double DELTA = 1e-9;

    private FixedDisplayPreferences preferences;

    /** Fixed format rather than a preference, so the expected strings cannot drift. */
    private LengthConverter converter;

    @BeforeEach
    public void setUp() {
        preferences = new FixedDisplayPreferences(LengthUnit.Millimeters);
        converter = new LengthConverter(preferences, "%.3f");
    }

    private static void assertLength(double value, LengthUnit units, Length actual) {
        assertEquals(value, actual.getValue(), DELTA);
        assertEquals(units, actual.getUnits());
    }

    @Test
    public void unitSuffixIsHonoured() {
        assertLength(15, LengthUnit.Millimeters, converter.convertReverse("15mm"));
        assertLength(15, LengthUnit.Centimeters, converter.convertReverse("15cm"));
        assertLength(15, LengthUnit.Meters, converter.convertReverse("15m"));
        assertLength(15, LengthUnit.Mils, converter.convertReverse("15mil"));
    }

    /** Feet and inches are spelled with the prime and double prime marks, not "ft" and "in". */
    @Test
    public void imperialUnitsUseQuoteMarks() {
        assertLength(15, LengthUnit.Inches, converter.convertReverse("15\""));
        assertLength(15, LengthUnit.Feet, converter.convertReverse("15'"));
    }

    @Test
    public void unitSuffixMayBeSeparatedBySpaces() {
        assertLength(15, LengthUnit.Millimeters, converter.convertReverse("15 mm"));
        assertLength(15, LengthUnit.Millimeters, converter.convertReverse("15    mm"));
    }

    @Test
    public void surroundingWhitespaceIsIgnored() {
        assertLength(15, LengthUnit.Millimeters, converter.convertReverse("  15mm  "));
    }

    @Test
    public void unitSuffixIsCaseInsensitive() {
        assertLength(15, LengthUnit.Millimeters, converter.convertReverse("15MM"));
        assertLength(15, LengthUnit.Mils, converter.convertReverse("15MIL"));
    }

    @Test
    public void bareNumberTakesTheSystemUnits() {
        assertLength(15, LengthUnit.Millimeters, converter.convertReverse("15"));

        preferences.units = LengthUnit.Inches;
        assertLength(15, LengthUnit.Inches, converter.convertReverse("15"));
    }

    @Test
    public void negativeAndFractionalValuesAreParsed() {
        assertLength(-5.5, LengthUnit.Millimeters, converter.convertReverse("-5.5mm"));
        assertLength(0.001, LengthUnit.Millimeters, converter.convertReverse("0.001mm"));
        assertLength(-0.25, LengthUnit.Millimeters, converter.convertReverse("-.25"));
    }

    @Test
    public void micronsAcceptTheAsciiSpelling() {
        assertLength(15, LengthUnit.Microns, converter.convertReverse("15um"));
        assertLength(15, LengthUnit.Microns, converter.convertReverse("15UM"));
        assertLength(15, LengthUnit.Microns, converter.convertReverse("15\u03bcm"));
    }

    /** The written names are accepted alongside the prime marks used for display. */
    @Test
    public void writtenUnitNamesAreAccepted() {
        assertLength(15, LengthUnit.Inches, converter.convertReverse("15in"));
        assertLength(15, LengthUnit.Inches, converter.convertReverse("15inch"));
        assertLength(15, LengthUnit.Inches, converter.convertReverse("15 inches"));
        assertLength(15, LengthUnit.Feet, converter.convertReverse("15ft"));
        assertLength(15, LengthUnit.Feet, converter.convertReverse("15feet"));
        assertLength(15, LengthUnit.Millimeters, converter.convertReverse("15millimeters"));
        assertLength(15, LengthUnit.Centimeters, converter.convertReverse("15centimetres"));
        assertLength(15, LengthUnit.Meters, converter.convertReverse("15metres"));
        assertLength(15, LengthUnit.Mils, converter.convertReverse("15thou"));
        assertLength(15, LengthUnit.Microns, converter.convertReverse("15micrometer"));
    }

    /**
     * A suffix that is present but not a unit is refused, rather than dropped so that the number
     * silently becomes a value in the system units. This is what keeps a mistyped field from
     * moving the machine to a coordinate nobody asked for.
     */
    @Test
    public void unknownUnitSuffixIsRejected() {
        assertThrows(RuntimeException.class, () -> converter.convertReverse("15xyz"));
        assertThrows(RuntimeException.class, () -> converter.convertReverse("15 miles"));
    }

    /**
     * Scientific notation is not supported: the exponent reads as the start of a unit suffix, and
     * an unknown suffix is now refused. Refusing is the point - it used to yield 1 instead of
     * 100000.
     */
    @Test
    public void scientificNotationIsRejected() {
        assertThrows(RuntimeException.class, () -> converter.convertReverse("1e5"));
    }

    @Test
    public void unparseableInputIsRejected() {
        assertThrows(RuntimeException.class, () -> converter.convertReverse("abc"));
        assertThrows(RuntimeException.class, () -> converter.convertReverse(""));
        assertThrows(RuntimeException.class, () -> converter.convertReverse("   "));
        assertThrows(RuntimeException.class, () -> converter.convertReverse("mm"));
        assertThrows(RuntimeException.class, () -> converter.convertReverse("1.2.3"));
        assertThrows(RuntimeException.class, () -> converter.convertReverse("--5"));
    }

    @Test
    public void rejectionNamesTheOffendingInput() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> converter.convertReverse("not a length"));

        assertEquals("Unable to parse not a length", e.getMessage());
    }

    @Test
    public void displayUsesTheConfiguredFormat() {
        assertEquals("15.000", converter.convertForward(new Length(15, LengthUnit.Millimeters)));
        assertEquals("15.500", converter.convertForward(new Length(15.5, LengthUnit.Millimeters)));
    }

    @Test
    public void displayRoundsToTheFormatPrecision() {
        assertEquals("0.001", converter.convertForward(new Length(0.0012, LengthUnit.Millimeters)));
        assertEquals("0.002", converter.convertForward(new Length(0.0016, LengthUnit.Millimeters)));
    }

    @Test
    public void displayConvertsIntoTheSystemUnits() {
        assertEquals("25.400", converter.convertForward(new Length(1, LengthUnit.Inches)));

        preferences.units = LengthUnit.Inches;
        assertEquals("1.000", converter.convertForward(new Length(25.4, LengthUnit.Millimeters)));
    }

    @Test
    public void displayOmitsTheUnitSuffix() {
        String shown = converter.convertForward(new Length(15, LengthUnit.Millimeters));

        assertEquals("15.000", shown, "the field shows the number alone; the unit comes from the UI");
    }

    @Test
    public void aValueSurvivesADisplayAndReentryCycle() {
        Length original = new Length(12.345, LengthUnit.Millimeters);

        Length roundTripped = converter.convertReverse(converter.convertForward(original));

        assertLength(12.345, LengthUnit.Millimeters, roundTripped);
    }

    @Test
    public void aValueInForeignUnitsIsRebasedByTheRoundTrip() {
        // Displaying converts into the system units and re-entering keeps them, so the unit of the
        // returned Length follows the system rather than the original.
        Length roundTripped =
                converter.convertReverse(converter.convertForward(new Length(1, LengthUnit.Inches)));

        assertLength(25.4, LengthUnit.Millimeters, roundTripped);
    }

    @Test
    public void precisionBeyondTheFormatIsLostOnRoundTrip() {
        // Three decimals of millimetre is a micron, so this is a deliberate limit rather than a
        // defect, but a caller re-reading a displayed value does not get back what it put in.
        Length roundTripped = converter
                .convertReverse(converter.convertForward(new Length(1.23456, LengthUnit.Millimeters)));

        assertLength(1.235, LengthUnit.Millimeters, roundTripped);
    }

    @Test
    public void formatFromTheConstructorIsUsedRatherThanThePreference() {
        preferences.format = "%.4f";
        LengthConverter twoDecimals = new LengthConverter(preferences, "%.2f");

        assertEquals("15.35",
                twoDecimals.convertForward(new Length(15.348, LengthUnit.Millimeters)));
    }

    @Test
    public void withoutAFormatThePreferenceIsUsed() {
        preferences.format = "%.1f";

        assertEquals("15.3",
                new LengthConverter(preferences).convertForward(new Length(15.348, LengthUnit.Millimeters)));
    }

    /**
     * Stands in for the Configuration the wizards use. Mutable so that a test can change the units
     * the way the settings panel does, without the change outliving the test.
     */
    private static class FixedDisplayPreferences implements DisplayPreferences {
        LengthUnit units;

        String format = "%.3f";

        FixedDisplayPreferences(LengthUnit units) {
            this.units = units;
        }

        @Override
        public LengthUnit getSystemUnits() {
            return units;
        }

        @Override
        public String getLengthDisplayFormat() {
            return format;
        }

        @Override
        public String getLengthDisplayAlignedFormat() {
            return format;
        }

        @Override
        public String getLengthDisplayFormatWithUnits() {
            return format + "%s";
        }

        @Override
        public String getLengthDisplayAlignedFormatWithUnits() {
            return format + "%s";
        }

        @Override
        public String formatLength(Length length) {
            return String.format(java.util.Locale.US, format,
                    length.convertToUnits(units).getValue());
        }

        @Override
        public int getVerticalScrollUnitIncrement() {
            return 16;
        }
    }
}
