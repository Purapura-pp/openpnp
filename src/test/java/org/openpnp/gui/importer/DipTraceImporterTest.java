package org.openpnp.gui.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openpnp.model.Abstract2DLocatable.Side;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;

/**
 * Covers the DipTrace pick and place parser. The format is fixed column order rather than named
 * columns, so the parser trusts the export layout completely:
 * {@code RefDes,Name,X (mm),Y (mm),Side,Rotate,Value}.
 */
public class DipTraceImporterTest {
    private static final double DELTA = 0.0001;

    private static final String HEADER = "RefDes,Name,X (mm),Y (mm),Side,Rotate,Value";

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        Configuration.initialize(tempDir.resolve(".openpnp").toFile());
        Configuration.get().load();
    }

    private File export(String... lines) throws Exception {
        Path file = Files.createTempFile(tempDir, "diptrace", ".csv");
        Files.write(file, String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
        return file.toFile();
    }

    private static Placement byId(List<Placement> placements, String id) {
        return placements.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No placement with id " + id));
    }

    @Test
    public void placementIsParsedInFull() throws Exception {
        File file = export(HEADER, "C1,C0603,8.6,7.2,Top,0,1nF");

        List<Placement> placements = DipTraceImporter.parseFile(file, true);

        assertEquals(1, placements.size());
        Placement c1 = placements.get(0);
        assertEquals("C1", c1.getId());
        assertEquals(8.6, c1.getLocation().getX(), DELTA);
        assertEquals(7.2, c1.getLocation().getY(), DELTA);
        assertEquals(0.0, c1.getLocation().getRotation(), DELTA);
        assertEquals(LengthUnit.Millimeters, c1.getLocation().getUnits());
        assertEquals(Side.Top, c1.getSide());
        assertEquals("C0603-1nF", c1.getPart().getId());
    }

    @Test
    public void firstLineIsAlwaysTreatedAsAHeader() throws Exception {
        File file = export(
                HEADER,
                "C1,C0603,8.6,7.2,Top,0,1nF",
                "C2,C0402,10.81,22.99,Top,180,0.1uF");

        List<Placement> placements = DipTraceImporter.parseFile(file, true);

        assertEquals(2, placements.size(), "the header must not become a placement");
    }

    @Test
    public void sideIsDecidedByTheFirstCharacterOfTheSideColumn() throws Exception {
        File file = export(
                HEADER,
                "C1,C0603,1,1,Top,0,1nF",
                "C2,C0603,2,2,Bottom,0,1nF");

        List<Placement> placements = DipTraceImporter.parseFile(file, true);

        assertEquals(Side.Top, byId(placements, "C1").getSide());
        assertEquals(Side.Bottom, byId(placements, "C2").getSide());
    }

    @Test
    public void anythingNotStartingWithTIsTakenAsBottom() throws Exception {
        File file = export(HEADER, "C1,C0603,1,1,bottom,0,1nF");

        List<Placement> placements = DipTraceImporter.parseFile(file, true);

        assertEquals(Side.Bottom, placements.get(0).getSide());
    }

    /**
     * The side used to be decided by comparing against an upper case T, so a hand edited file
     * spelling the side in lower case put every top placement on the bottom of the board.
     */
    @Test
    public void sideIsRecognisedRegardlessOfCase() throws Exception {
        File file = export(
                HEADER,
                "C1,C0603,1,1,top,0,1nF",
                "C2,C0603,2,2,TOP,0,1nF",
                "C3,C0603,3,3,tOp,0,1nF",
                "C4,C0603,4,4,BOTTOM,0,1nF");

        List<Placement> placements = DipTraceImporter.parseFile(file, true);

        assertEquals(Side.Top, byId(placements, "C1").getSide());
        assertEquals(Side.Top, byId(placements, "C2").getSide());
        assertEquals(Side.Top, byId(placements, "C3").getSide());
        assertEquals(Side.Bottom, byId(placements, "C4").getSide());
    }

    @Test
    public void surroundingSpaceInTheSideColumnIsIgnored() throws Exception {
        File file = export(HEADER, "C1,C0603,1,1, Top ,0,1nF");

        List<Placement> placements = DipTraceImporter.parseFile(file, true);

        assertEquals(Side.Top, placements.get(0).getSide());
    }

    @Test
    public void anEmptySideColumnFallsBackToBottom() throws Exception {
        File file = export(HEADER, "C1,C0603,1,1,,0,1nF");

        List<Placement> placements = DipTraceImporter.parseFile(file, true);

        assertEquals(Side.Bottom, placements.get(0).getSide());
    }

    @Test
    public void negativeAndFractionalValuesAreParsed() throws Exception {
        File file = export(HEADER, "C1,C0603,-12.345,-6.789,Top,-90.5,1nF");

        List<Placement> placements = DipTraceImporter.parseFile(file, true);

        assertEquals(-12.345, placements.get(0).getLocation().getX(), DELTA);
        assertEquals(-6.789, placements.get(0).getLocation().getY(), DELTA);
        assertEquals(-90.5, placements.get(0).getLocation().getRotation(), DELTA);
    }

    @Test
    public void rotationIsNotNormalised() throws Exception {
        File file = export(HEADER, "C1,C0603,1,1,Top,270,1nF");

        List<Placement> placements = DipTraceImporter.parseFile(file, true);

        assertEquals(270.0, placements.get(0).getLocation().getRotation(), DELTA,
                "unlike the CSV importer, this one passes the angle through unchanged");
    }

    @Test
    public void createdPartCarriesThePackageColumn() throws Exception {
        File file = export(HEADER, "C1,C0603,1,1,Top,0,1nF");

        DipTraceImporter.parseFile(file, true);

        Part part = Configuration.get().getPart("C0603-1nF");
        assertNotNull(part);
        assertEquals("C0603", part.getPackage().getId());
    }

    @Test
    public void partsAreReusedAcrossPlacements() throws Exception {
        File file = export(
                HEADER,
                "C1,C0603,1,1,Top,0,1nF",
                "C2,C0603,2,2,Top,0,1nF");

        List<Placement> placements = DipTraceImporter.parseFile(file, true);

        assertSame(byId(placements, "C1").getPart(), byId(placements, "C2").getPart());
    }

    @Test
    public void noPartIsAssignedWhenPartCreationIsOff() throws Exception {
        File file = export(HEADER, "C1,C0603,1,1,Top,0,1nF");

        List<Placement> placements = DipTraceImporter.parseFile(file, false);

        assertEquals(1, placements.size(), "the placement is still imported");
        assertNull(placements.get(0).getPart());
        assertNull(Configuration.get().getPart("C0603-1nF"));
    }

    @Test
    public void emptyLinesAreSkipped() throws Exception {
        File file = export(
                HEADER,
                "C1,C0603,1,1,Top,0,1nF",
                "",
                "C2,C0603,2,2,Top,0,1nF");

        List<Placement> placements = DipTraceImporter.parseFile(file, true);

        assertEquals(2, placements.size());
    }

    /**
     * The blank check used to run before the line was trimmed, so a line of spaces got through and
     * then failed on the column lookup, aborting the whole import over one stray space.
     */
    @Test
    public void whitespaceOnlyLineIsSkipped() throws Exception {
        File file = export(
                HEADER,
                "C1,C0603,1,1,Top,0,1nF",
                "   ",
                "\t",
                "C2,C0603,2,2,Top,0,1nF");

        List<Placement> placements = DipTraceImporter.parseFile(file, true);

        assertEquals(2, placements.size());
        assertEquals(Side.Top, byId(placements, "C2").getSide());
    }

    /**
     * A row that really is short is still fatal - guessing at missing coordinates would place
     * parts in the wrong spot - but the failure now names the line instead of surfacing as an
     * array index out of bounds.
     */
    @Test
    public void aRowWithTooFewColumnsReportsTheLine() throws Exception {
        File file = export(
                HEADER,
                "C1,C0603,1,1,Top,0,1nF",
                "C2,C0603,1,1,Top,0");

        Exception e = assertThrows(Exception.class,
                () -> DipTraceImporter.parseFile(file, true));

        assertTrue(e.getMessage().contains("3"),
                "the message should name the offending line: " + e.getMessage());
        assertTrue(e.getMessage().contains("C2,C0603,1,1,Top,0"),
                "the message should quote the offending line: " + e.getMessage());
    }
}
