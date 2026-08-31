package org.openpnp.gui.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    private Path tempDir;

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

        assertEquals(Side.Bottom, placements.get(0).getSide(),
                "the comparison is against an upper case T, so lower case top reads as bottom");
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
     * The blank check runs before the line is trimmed, so a line of spaces gets through and then
     * fails on the column lookup. Pins the present behaviour: such a file aborts the import rather
     * than skipping the line.
     */
    @Test
    public void whitespaceOnlyLineAbortsTheImport() throws Exception {
        File file = export(
                HEADER,
                "C1,C0603,1,1,Top,0,1nF",
                "   ",
                "C2,C0603,2,2,Top,0,1nF");

        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> DipTraceImporter.parseFile(file, true));
    }

    @Test
    public void aRowWithTooFewColumnsAbortsTheImport() throws Exception {
        File file = export(HEADER, "C1,C0603,1,1,Top,0");

        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> DipTraceImporter.parseFile(file, true));
    }
}
