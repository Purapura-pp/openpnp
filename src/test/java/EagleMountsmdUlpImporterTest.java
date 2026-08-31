import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openpnp.gui.importer.EagleMountsmdUlpImporter;
import org.openpnp.model.Abstract2DLocatable.Side;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;

/**
 * Covers the Eagle mountsmd ULP importer, whose input is whitespace separated:
 * {@code Name X Y Angle [Value] [Package]}. Eagle omits the value for parts that have none, which
 * leaves five fields, and the importer then treats the last field as the package.
 */
public class EagleMountsmdUlpImporterTest {
    private static final double DELTA = 0.0001;

    private static final File DEMO_BOARD = new File("samples", "Demo Board");
    private static final File EAT001 = new File("samples", "EAT001");

    @TempDir
    private Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        Configuration.initialize(tempDir.resolve(".openpnp").toFile());
        Configuration.get().load();
    }

    private File mnt(String... lines) throws Exception {
        Path file = Files.createTempFile(tempDir, "mountsmd", ".mnt");
        Files.write(file, String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
        return file.toFile();
    }

    @Test
    public void demoBoardTopAndBottomAreParsed() throws Exception {
        List<Placement> top = EagleMountsmdUlpImporter
                .parseFile(new File(DEMO_BOARD, "Demo Board v2.mnt"), Side.Top, true);
        List<Placement> bottom = EagleMountsmdUlpImporter
                .parseFile(new File(DEMO_BOARD, "Demo Board v2.mnb"), Side.Bottom, true);

        assertEquals(90, top.size());
        assertEquals(92, bottom.size());
    }

    @Test
    public void placementIsParsedInFull() throws Exception {
        // C1 48.11 33.02  90  ELECTRO-SMD-E-7.8MM
        List<Placement> top = EagleMountsmdUlpImporter
                .parseFile(new File(DEMO_BOARD, "Demo Board v2.mnt"), Side.Top, true);

        Placement c1 = top.stream()
                .filter(p -> p.getId().equals("C1"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No placement C1"));
        assertEquals(48.11, c1.getLocation().getX(), DELTA);
        assertEquals(33.02, c1.getLocation().getY(), DELTA);
        assertEquals(90.0, c1.getLocation().getRotation(), DELTA);
        assertEquals(LengthUnit.Millimeters, c1.getLocation().getUnits());
        assertEquals(Side.Top, c1.getSide());
        assertEquals("ELECTRO-SMD-E-7.8MM", c1.getPart().getId());
    }

    @Test
    public void sideComesFromTheArgument() throws Exception {
        List<Placement> asBottom = EagleMountsmdUlpImporter
                .parseFile(new File(DEMO_BOARD, "Demo Board v2.mnt"), Side.Bottom, true);

        assertFalse(asBottom.isEmpty());
        for (Placement placement : asBottom) {
            assertEquals(Side.Bottom, placement.getSide());
        }
    }

    /** With no value column the last field is the package, and the part is named after it. */
    @Test
    public void fiveFieldLineNamesThePartAfterItsPackage() throws Exception {
        File file = mnt("C1 48.11 33.02  90  ELECTRO-SMD-E-7.8MM");

        List<Placement> placements = EagleMountsmdUlpImporter.parseFile(file, Side.Top, true);

        Part part = placements.get(0).getPart();
        assertEquals("ELECTRO-SMD-E-7.8MM", part.getId());
        assertEquals("ELECTRO-SMD-E-7.8MM", part.getPackage().getId());
    }

    /** With a value present the part id combines package and value. */
    @Test
    public void sixFieldLineCombinesPackageAndValue() throws Exception {
        File file = mnt("C1 41.91 34.93 180 0.1uF C0805");

        List<Placement> placements = EagleMountsmdUlpImporter.parseFile(file, Side.Top, true);

        Part part = placements.get(0).getPart();
        assertEquals("C0805-0.1uF", part.getId());
        assertEquals("C0805", part.getPackage().getId());
    }

    @Test
    public void blankLinesAreSkipped() throws Exception {
        File file = mnt(
                "C1 1.0 2.0 0 C0805",
                "",
                "   ",
                "C2 3.0 4.0 0 C0805");

        List<Placement> placements = EagleMountsmdUlpImporter.parseFile(file, Side.Top, true);

        assertEquals(2, placements.size());
    }

    @Test
    public void noPartIsAssignedWhenPartCreationIsOff() throws Exception {
        File file = mnt("C1 1.0 2.0 0 C0805");

        List<Placement> placements = EagleMountsmdUlpImporter.parseFile(file, Side.Top, false);

        assertEquals(1, placements.size(), "the placement is still imported");
        assertNull(placements.get(0).getPart());
        assertNull(Configuration.get().getPart("C0805"));
    }

    @Test
    public void rulerOriginMarkerIsImportedLikeAnyOtherLine() throws Exception {
        // TOP_RULER_ORGIN 87.00 49.00 0 RULER
        List<Placement> top = EagleMountsmdUlpImporter
                .parseFile(new File(DEMO_BOARD, "Demo Board v2.mnt"), Side.Top, true);

        Placement ruler = top.stream()
                .filter(p -> p.getId().equals("TOP_RULER_ORGIN"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No ruler origin placement"));
        assertEquals("RULER", ruler.getPart().getId(),
                "the marker is not filtered out, so it arrives as a placement to be removed by hand");
    }

    @Test
    public void eat001SampleIsParsed() throws Exception {
        List<Placement> top =
                EagleMountsmdUlpImporter.parseFile(new File(EAT001, "EAT001.mnt"), Side.Top, true);
        List<Placement> bottom = EagleMountsmdUlpImporter
                .parseFile(new File(EAT001, "EAT001.mnb"), Side.Bottom, true);

        assertFalse(top.isEmpty());
        assertNotNull(bottom, "the bottom file may legitimately be empty, but must parse");
    }

    /**
     * Regression for whole number coordinates, which Eagle writes without a decimal point.
     * See https://github.com/openpnp/openpnp/issues/390
     */
    @Test
    public void wholeNumberCoordinatesAreParsed() throws Exception {
        List<Placement> placements = EagleMountsmdUlpImporter
                .parseFile(new File("samples", "test/mountsmd_whole_numbers.mnt"), Side.Top, true);

        assertFalse(placements.isEmpty());
    }
}
