package org.openpnp.gui.importer.rs274x;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.openpnp.model.BoardPad;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Pad;

/**
 * Covers the solder paste Gerber parser. Only flashes of standard apertures become pads; strokes
 * and regions are counted but produce nothing, which is what the importer relies on.
 */
public class Rs274xParserTest {
    private static final double DELTA = 0.000001;

    private static final File DEMO_BOARD = new File("samples", "Demo Board");

    /** A complete, minimal file: format, unit, one aperture, one flash. */
    private static String gerber(String... body) {
        StringBuilder sb = new StringBuilder();
        sb.append("%FSLAX24Y24*%\n");
        sb.append("%MOMM*%\n");
        for (String line : body) {
            sb.append(line).append("\n");
        }
        sb.append("M02*\n");
        return sb.toString();
    }

    private static List<BoardPad> parse(String source) throws Exception {
        return new Rs274xParser().parseSolderPastePads(new StringReader(source));
    }

    @Test
    public void circularApertureFlashBecomesACirclePad() throws Exception {
        List<BoardPad> pads = parse(gerber(
                "%ADD10C,1.0*%",
                "D10*",
                "X10000Y20000D03*"));

        assertEquals(1, pads.size());
        BoardPad boardPad = pads.get(0);
        assertEquals(1.0, boardPad.getLocation().getX(), DELTA);
        assertEquals(2.0, boardPad.getLocation().getY(), DELTA);
        assertEquals(LengthUnit.Millimeters, boardPad.getLocation().getUnits());
        assertEquals(BoardPad.Type.Paste, boardPad.getType());

        assertTrue(boardPad.getPad() instanceof Pad.Circle,
                "expected a circle pad, got " + boardPad.getPad().getClass().getSimpleName());
        Pad.Circle pad = (Pad.Circle) boardPad.getPad();
        assertEquals(0.5, pad.getRadius(), DELTA, "radius is half the aperture diameter");
        assertEquals(LengthUnit.Millimeters, pad.getUnits());
    }

    @Test
    public void rectangularApertureFlashBecomesASquareCorneredPad() throws Exception {
        List<BoardPad> pads = parse(gerber(
                "%ADD11R,2.0X3.0*%",
                "D11*",
                "X0Y0D03*"));

        assertEquals(1, pads.size());
        assertTrue(pads.get(0).getPad() instanceof Pad.RoundRectangle,
                "expected a rectangle pad, got "
                        + pads.get(0).getPad().getClass().getSimpleName());
        Pad.RoundRectangle pad = (Pad.RoundRectangle) pads.get(0).getPad();
        assertEquals(2.0, pad.getWidth(), DELTA);
        assertEquals(3.0, pad.getHeight(), DELTA);
        assertEquals(0.0, pad.getRoundness(), DELTA);
    }

    @Test
    public void padsAreNamedAfterTheirApertureAndUseCount() throws Exception {
        List<BoardPad> pads = parse(gerber(
                "%ADD10C,1.0*%",
                "D10*",
                "X10000Y10000D03*",
                "X20000Y10000D03*",
                "X30000Y10000D03*"));

        assertEquals(3, pads.size());
        assertEquals("D10-000", pads.get(0).getName());
        assertEquals("D10-001", pads.get(1).getName());
        assertEquals("D10-002", pads.get(2).getName());
    }

    @Test
    public void eachApertureCountsSeparately() throws Exception {
        List<BoardPad> pads = parse(gerber(
                "%ADD10C,1.0*%",
                "%ADD11R,2.0X3.0*%",
                "D10*",
                "X10000Y10000D03*",
                "D11*",
                "X20000Y10000D03*",
                "D10*",
                "X30000Y10000D03*"));

        assertEquals("D10-000", pads.get(0).getName());
        assertEquals("D11-000", pads.get(1).getName());
        assertEquals("D10-001", pads.get(2).getName());
    }

    @Test
    public void strokesAndMovesProduceNoPads() throws Exception {
        List<BoardPad> pads = parse(gerber(
                "%ADD10C,1.0*%",
                "D10*",
                "X10000Y10000D02*",
                "X20000Y20000D01*"));

        assertTrue(pads.isEmpty(), "only flashes become pads");
    }

    @Test
    public void inchUnitIsCarriedThrough() throws Exception {
        String source = "%FSLAX24Y24*%\n"
                + "%MOIN*%\n"
                + "%ADD10C,0.05*%\n"
                + "D10*\n"
                + "X10000Y10000D03*\n"
                + "M02*\n";

        List<BoardPad> pads = parse(source);

        assertEquals(LengthUnit.Inches, pads.get(0).getLocation().getUnits());
        assertEquals(LengthUnit.Inches, pads.get(0).getPad().getUnits());
    }

    @Test
    public void coordinatesAreSignAware() throws Exception {
        List<BoardPad> pads = parse(gerber(
                "%ADD10C,1.0*%",
                "D10*",
                "X-15000Y-25000D03*"));

        assertEquals(-1.5, pads.get(0).getLocation().getX(), DELTA);
        assertEquals(-2.5, pads.get(0).getLocation().getY(), DELTA);
    }

    @Test
    public void coordinateIsCarriedOverWhenOnlyOneAxisIsGiven() throws Exception {
        List<BoardPad> pads = parse(gerber(
                "%ADD10C,1.0*%",
                "D10*",
                "X10000Y20000D03*",
                "X30000D03*"));

        assertEquals(3.0, pads.get(1).getLocation().getX(), DELTA);
        assertEquals(2.0, pads.get(1).getLocation().getY(), DELTA,
                "Y should persist from the previous coordinate");
    }

    /**
     * The full declared precision has to survive: in a 2.4 format the implied decimal point sits
     * four digits from the right, so X12345 is 1.2345 and Y00001 is one unit in the last place.
     * The reader used to build the fraction one digit short and drop that last place.
     */
    @Test
    public void coordinateKeepsEveryDeclaredDecimalDigit() throws Exception {
        List<BoardPad> pads = parse(gerber(
                "%ADD10C,1.0*%",
                "D10*",
                "X12345Y00001D03*"));

        assertEquals(1.2345, pads.get(0).getLocation().getX(), DELTA);
        assertEquals(0.0001, pads.get(0).getLocation().getY(), DELTA);
    }

    /**
     * A coordinate needing more integer digits than the format announces is still read with the
     * decimal point four digits from the right, rather than being shifted by the surplus.
     */
    @Test
    public void coordinateWiderThanTheFormatKeepsItsScale() throws Exception {
        List<BoardPad> pads = parse(gerber(
                "%ADD10C,1.0*%",
                "D10*",
                "X1234567Y0000001D03*"));

        assertEquals(123.4567, pads.get(0).getLocation().getX(), DELTA);
        assertEquals(0.0001, pads.get(0).getLocation().getY(), DELTA);
    }

    @Test
    public void negativeCoordinateKeepsEveryDecimalDigit() throws Exception {
        List<BoardPad> pads = parse(gerber(
                "%ADD10C,1.0*%",
                "D10*",
                "X-12345Y-00001D03*"));

        assertEquals(-1.2345, pads.get(0).getLocation().getX(), DELTA);
        assertEquals(-0.0001, pads.get(0).getLocation().getY(), DELTA);
    }

    /**
     * Aperture macros are not reconstructed. Importing the layer anyway would hand back a paste
     * layer with every macro pad missing, so the parser has to refuse and say why. It must also
     * not mistake the leading letter of the macro name for a standard aperture template.
     */
    @Test
    public void macroApertureIsRefusedWithAnExplanation() throws Exception {
        String source = "%FSLAX24Y24*%\n"
                + "%MOMM*%\n"
                + "%AMROUNDRECT*21,1,$1,$2,0,0,0*%\n"
                + "%ADD10ROUNDRECT,0.5X0.3*%\n"
                + "D10*\n"
                + "X10000Y10000D03*\n"
                + "M02*\n";

        Exception e = assertThrows(Exception.class, () -> parse(source));

        assertTrue(e.getMessage().contains("ROUNDRECT"),
                "the message should name the macro: " + e.getMessage());
        assertTrue(e.getMessage().contains("macro"),
                "the message should explain the cause: " + e.getMessage());
    }

    @Test
    public void unnamedMacroApertureIsAlsoRefused() throws Exception {
        String source = "%FSLAX24Y24*%\n"
                + "%MOMM*%\n"
                + "%AMTHERMAL*7,0,0,0.8,0.55,0.125,45*%\n"
                + "%ADD10THERMAL*%\n"
                + "D10*\n"
                + "X10000Y10000D03*\n"
                + "M02*\n";

        Exception e = assertThrows(Exception.class, () -> parse(source));

        assertTrue(e.getMessage().contains("THERMAL"),
                "the message should name the macro: " + e.getMessage());
    }

    @Test
    public void topSolderPasteLayerOfTheDemoBoardIsParsed() throws Exception {
        List<BoardPad> pads =
                new Rs274xParser().parseSolderPastePads(new File(DEMO_BOARD, "Demo Board v2.GTP"));

        // The layer contains 269 flashes, all of standard circle and rectangle apertures.
        assertEquals(269, pads.size());
    }

    @Test
    public void bottomSolderPasteLayerOfTheDemoBoardIsParsed() throws Exception {
        List<BoardPad> pads =
                new Rs274xParser().parseSolderPastePads(new File(DEMO_BOARD, "Demo Board v2.GBP"));

        assertEquals(137, pads.size());
    }

    @Test
    public void everyPadOfARealLayerIsUsable() throws Exception {
        List<BoardPad> pads =
                new Rs274xParser().parseSolderPastePads(new File(DEMO_BOARD, "Demo Board v2.GTP"));

        Set<String> names = new HashSet<>();
        for (BoardPad pad : pads) {
            assertTrue(names.add(pad.getName()), "duplicate pad name " + pad.getName());
            assertEquals(LengthUnit.Inches, pad.getLocation().getUnits());
            assertEquals(BoardPad.Type.Paste, pad.getType());
            assertTrue(pad.getPad() instanceof Pad.Circle
                    || pad.getPad() instanceof Pad.RoundRectangle,
                    "unexpected pad shape " + pad.getPad().getClass().getSimpleName());
        }
    }

    @Test
    public void parserInstanceCanBeReused() throws Exception {
        Rs274xParser parser = new Rs274xParser();

        int first = parser.parseSolderPastePads(new File(DEMO_BOARD, "Demo Board v2.GTP")).size();
        int second = parser.parseSolderPastePads(new File(DEMO_BOARD, "Demo Board v2.GTP")).size();

        assertEquals(first, second, "state from the previous parse must not leak into the next");
    }
}
