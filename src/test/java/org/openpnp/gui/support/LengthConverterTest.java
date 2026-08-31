package org.openpnp.gui.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;

/**
 * Covers the length field of the configuration wizards, from the text a user types through to the
 * Length that reaches the model, and back.
 * <p>
 * The conversion itself is thin, but it sits on {@link Length#parse}, whose behaviour at the edges
 * decides what happens when someone mistypes a unit in a field that positions a real machine.
 */
public class LengthConverterTest {
    private static final double DELTA = 1e-9;

    /** Fixed rather than read from preferences, so the expected strings cannot drift. */
    private static final LengthConverter CONVERTER = new LengthConverter("%.3f");

    private LengthUnit originalUnits;

    @BeforeEach
    public void setUp() {
        Configuration.initialize();
        // System units live in the shared user preferences, so put them back afterwards.
        originalUnits = Configuration.get().getSystemUnits();
        Configuration.get().setSystemUnits(LengthUnit.Millimeters);
    }

    @AfterEach
    public void tearDown() {
        Configuration.get().setSystemUnits(originalUnits);
    }

    private static void assertLength(double value, LengthUnit units, Length actual) {
        assertEquals(value, actual.getValue(), DELTA);
        assertEquals(units, actual.getUnits());
    }

    @Test
    public void unitSuffixIsHonoured() {
        assertLength(15, LengthUnit.Millimeters, CONVERTER.convertReverse("15mm"));
        assertLength(15, LengthUnit.Centimeters, CONVERTER.convertReverse("15cm"));
        assertLength(15, LengthUnit.Meters, CONVERTER.convertReverse("15m"));
        assertLength(15, LengthUnit.Mils, CONVERTER.convertReverse("15mil"));
    }

    /** Feet and inches are spelled with the prime and double prime marks, not "ft" and "in". */
    @Test
    public void imperialUnitsUseQuoteMarks() {
        assertLength(15, LengthUnit.Inches, CONVERTER.convertReverse("15\""));
        assertLength(15, LengthUnit.Feet, CONVERTER.convertReverse("15'"));
    }

    @Test
    public void unitSuffixMayBeSeparatedBySpaces() {
        assertLength(15, LengthUnit.Millimeters, CONVERTER.convertReverse("15 mm"));
        assertLength(15, LengthUnit.Millimeters, CONVERTER.convertReverse("15    mm"));
    }

    @Test
    public void surroundingWhitespaceIsIgnored() {
        assertLength(15, LengthUnit.Millimeters, CONVERTER.convertReverse("  15mm  "));
    }

    @Test
    public void unitSuffixIsCaseInsensitive() {
        assertLength(15, LengthUnit.Millimeters, CONVERTER.convertReverse("15MM"));
        assertLength(15, LengthUnit.Mils, CONVERTER.convertReverse("15MIL"));
    }

    @Test
    public void bareNumberTakesTheSystemUnits() {
        assertLength(15, LengthUnit.Millimeters, CONVERTER.convertReverse("15"));

        Configuration.get().setSystemUnits(LengthUnit.Inches);
        assertLength(15, LengthUnit.Inches, CONVERTER.convertReverse("15"));
    }

    @Test
    public void negativeAndFractionalValuesAreParsed() {
        assertLength(-5.5, LengthUnit.Millimeters, CONVERTER.convertReverse("-5.5mm"));
        assertLength(0.001, LengthUnit.Millimeters, CONVERTER.convertReverse("0.001mm"));
        assertLength(-0.25, LengthUnit.Millimeters, CONVERTER.convertReverse("-.25"));
    }

    @Test
    public void micronsAcceptTheAsciiSpelling() {
        assertLength(15, LengthUnit.Microns, CONVERTER.convertReverse("15um"));
        assertLength(15, LengthUnit.Microns, CONVERTER.convertReverse("15UM"));
        assertLength(15, LengthUnit.Microns, CONVERTER.convertReverse("15\u03bcm"));
    }

    /** The written names are accepted alongside the prime marks used for display. */
    @Test
    public void writtenUnitNamesAreAccepted() {
        assertLength(15, LengthUnit.Inches, CONVERTER.convertReverse("15in"));
        assertLength(15, LengthUnit.Inches, CONVERTER.convertReverse("15inch"));
        assertLength(15, LengthUnit.Inches, CONVERTER.convertReverse("15 inches"));
        assertLength(15, LengthUnit.Feet, CONVERTER.convertReverse("15ft"));
        assertLength(15, LengthUnit.Feet, CONVERTER.convertReverse("15feet"));
        assertLength(15, LengthUnit.Millimeters, CONVERTER.convertReverse("15millimeters"));
        assertLength(15, LengthUnit.Centimeters, CONVERTER.convertReverse("15centimetres"));
        assertLength(15, LengthUnit.Meters, CONVERTER.convertReverse("15metres"));
        assertLength(15, LengthUnit.Mils, CONVERTER.convertReverse("15thou"));
        assertLength(15, LengthUnit.Microns, CONVERTER.convertReverse("15micrometer"));
    }

    /**
     * A suffix that is present but not a unit is refused, rather than dropped so that the number
     * silently becomes a value in the system units. This is what keeps a mistyped field from
     * moving the machine to a coordinate nobody asked for.
     */
    @Test
    public void unknownUnitSuffixIsRejected() {
        assertThrows(RuntimeException.class, () -> CONVERTER.convertReverse("15xyz"));
        assertThrows(RuntimeException.class, () -> CONVERTER.convertReverse("15 miles"));
    }

    /**
     * Scientific notation is not supported: the exponent reads as the start of a unit suffix, and
     * an unknown suffix is now refused. Refusing is the point - it used to yield 1 instead of
     * 100000.
     */
    @Test
    public void scientificNotationIsRejected() {
        assertThrows(RuntimeException.class, () -> CONVERTER.convertReverse("1e5"));
    }

    @Test
    public void unparseableInputIsRejected() {
        assertThrows(RuntimeException.class, () -> CONVERTER.convertReverse("abc"));
        assertThrows(RuntimeException.class, () -> CONVERTER.convertReverse(""));
        assertThrows(RuntimeException.class, () -> CONVERTER.convertReverse("   "));
        assertThrows(RuntimeException.class, () -> CONVERTER.convertReverse("mm"));
        assertThrows(RuntimeException.class, () -> CONVERTER.convertReverse("1.2.3"));
        assertThrows(RuntimeException.class, () -> CONVERTER.convertReverse("--5"));
    }

    @Test
    public void rejectionNamesTheOffendingInput() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> CONVERTER.convertReverse("not a length"));

        assertEquals("Unable to parse not a length", e.getMessage());
    }

    @Test
    public void displayUsesTheConfiguredFormat() {
        assertEquals("15.000", CONVERTER.convertForward(new Length(15, LengthUnit.Millimeters)));
        assertEquals("15.500", CONVERTER.convertForward(new Length(15.5, LengthUnit.Millimeters)));
    }

    @Test
    public void displayRoundsToTheFormatPrecision() {
        assertEquals("0.001", CONVERTER.convertForward(new Length(0.0012, LengthUnit.Millimeters)));
        assertEquals("0.002", CONVERTER.convertForward(new Length(0.0016, LengthUnit.Millimeters)));
    }

    @Test
    public void displayConvertsIntoTheSystemUnits() {
        assertEquals("25.400", CONVERTER.convertForward(new Length(1, LengthUnit.Inches)));

        Configuration.get().setSystemUnits(LengthUnit.Inches);
        assertEquals("1.000", CONVERTER.convertForward(new Length(25.4, LengthUnit.Millimeters)));
    }

    @Test
    public void displayOmitsTheUnitSuffix() {
        String shown = CONVERTER.convertForward(new Length(15, LengthUnit.Millimeters));

        assertEquals("15.000", shown, "the field shows the number alone; the unit comes from the UI");
    }

    @Test
    public void aValueSurvivesADisplayAndReentryCycle() {
        Length original = new Length(12.345, LengthUnit.Millimeters);

        Length roundTripped = CONVERTER.convertReverse(CONVERTER.convertForward(original));

        assertLength(12.345, LengthUnit.Millimeters, roundTripped);
    }

    @Test
    public void aValueInForeignUnitsIsRebasedByTheRoundTrip() {
        // Displaying converts into the system units and re-entering keeps them, so the unit of the
        // returned Length follows the system rather than the original.
        Length roundTripped =
                CONVERTER.convertReverse(CONVERTER.convertForward(new Length(1, LengthUnit.Inches)));

        assertLength(25.4, LengthUnit.Millimeters, roundTripped);
    }

    @Test
    public void precisionBeyondTheFormatIsLostOnRoundTrip() {
        // Three decimals of millimetre is a micron, so this is a deliberate limit rather than a
        // defect, but a caller re-reading a displayed value does not get back what it put in.
        Length roundTripped = CONVERTER
                .convertReverse(CONVERTER.convertForward(new Length(1.23456, LengthUnit.Millimeters)));

        assertLength(1.235, LengthUnit.Millimeters, roundTripped);
    }

    @Test
    public void formatFromTheConstructorIsUsedRatherThanThePreference() {
        LengthConverter twoDecimals = new LengthConverter("%.2f");

        assertEquals("15.35",
                twoDecimals.convertForward(new Length(15.348, LengthUnit.Millimeters)));
    }
}
