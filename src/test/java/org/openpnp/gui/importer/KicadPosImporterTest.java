package org.openpnp.gui.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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
 * Covers {@link KicadPosImporter#parseFile} against the sample export checked into the repository,
 * and against the coordinate conventions it applies to bottom side parts.
 */
public class KicadPosImporterTest {
    private static final double DELTA = 0.0001;

    /** The sample export shipped with the project, 95 placements on the top side. */
    private static final File SAMPLE = new File("samples", "kicad-example-F.Cu.pos");

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        // parseFile resolves parts through the Configuration singleton.
        Configuration.initialize(tempDir.resolve(".openpnp").toFile());
        Configuration.get().load();
    }

    /** Builds a .pos file from the given lines, so a single convention can be exercised alone. */
    private File posFile(String... lines) throws Exception {
        Path file = Files.createTempFile(tempDir, "positions", ".pos");
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
    public void everyDataLineOfTheSampleBecomesAPlacement() throws Exception {
        List<Placement> placements = KicadPosImporter.parseFile(SAMPLE, Side.Top, true, true, false);

        assertEquals(95, placements.size());
    }

    @Test
    public void placementIsParsedInFull() throws Exception {
        // C1       100u             Capacitors_SMD:c  128.9050   -52.0700       0.0    F.Cu
        List<Placement> placements = KicadPosImporter.parseFile(SAMPLE, Side.Top, true, true, false);

        Placement c1 = byId(placements, "C1");
        assertEquals(128.9050, c1.getLocation().getX(), DELTA);
        assertEquals(-52.0700, c1.getLocation().getY(), DELTA);
        assertEquals(0.0, c1.getLocation().getRotation(), DELTA);
        assertEquals(LengthUnit.Millimeters, c1.getLocation().getUnits());
        assertEquals(Side.Top, c1.getSide());
        assertEquals("Capacitors_SMD:c-100u", c1.getPart().getId());
    }

    @Test
    public void referenceDesignatorMayContainPunctuation() throws Exception {
        // The logo is exported as a placement whose reference is G***.
        List<Placement> placements = KicadPosImporter.parseFile(SAMPLE, Side.Top, true, true, false);

        Placement logo = byId(placements, "G***");
        assertEquals("bumps:OSH-LOGO-LOGO", logo.getPart().getId());
    }

    @Test
    public void rotationIsTakenFromTheFile() throws Exception {
        List<Placement> placements = KicadPosImporter.parseFile(SAMPLE, Side.Top, true, true, false);

        assertEquals(180.0, byId(placements, "C2").getLocation().getRotation(), DELTA);
        assertEquals(270.0, byId(placements, "C12").getLocation().getRotation(), DELTA);
        assertEquals(90.0, byId(placements, "C18").getLocation().getRotation(), DELTA);
    }

    @Test
    public void sideIsTakenFromTheArgument() throws Exception {
        List<Placement> placements =
                KicadPosImporter.parseFile(SAMPLE, Side.Bottom, true, true, false);

        assertEquals(Side.Bottom, byId(placements, "C1").getSide());
    }

    @Test
    public void partIdCombinesPackageAndValue() throws Exception {
        List<Placement> placements = KicadPosImporter.parseFile(SAMPLE, Side.Top, true, true, false);

        assertEquals("SMD_Packages:SM0-100k", byId(placements, "R1").getPart().getId());
    }

    @Test
    public void partIdIsTheBareValueWhenRequested() throws Exception {
        List<Placement> placements = KicadPosImporter.parseFile(SAMPLE, Side.Top, true, true, true);

        assertEquals("100k", byId(placements, "R1").getPart().getId());
    }

    @Test
    public void createdPartCarriesThePackageFromTheFile() throws Exception {
        KicadPosImporter.parseFile(SAMPLE, Side.Top, true, true, false);

        Part part = Configuration.get().getPart("Capacitors_SMD:c-100u");
        assertNotNull(part, "the missing part should have been created");
        assertNotNull(part.getPackage());
        assertEquals("Capacitors_SMD:c", part.getPackage().getId());
    }

    @Test
    public void partsAreReusedRatherThanDuplicated() throws Exception {
        // C1 through C5 all share the same package and value.
        List<Placement> placements = KicadPosImporter.parseFile(SAMPLE, Side.Top, true, true, false);

        assertSame(byId(placements, "C1").getPart(), byId(placements, "C2").getPart());
    }

    @Test
    public void noPartIsAssignedWhenAssignPartsIsOff() throws Exception {
        List<Placement> placements =
                KicadPosImporter.parseFile(SAMPLE, Side.Top, false, true, false);

        assertNull(byId(placements, "C1").getPart());
        assertNull(Configuration.get().getPart("Capacitors_SMD:c-100u"),
                "no part should have been created either");
    }

    @Test
    public void unknownPartIsLeftUnassignedWhenNotCreatingParts() throws Exception {
        List<Placement> placements =
                KicadPosImporter.parseFile(SAMPLE, Side.Top, true, false, false);

        assertNull(byId(placements, "C1").getPart());
        assertNull(Configuration.get().getPart("Capacitors_SMD:c-100u"));
    }

    @Test
    public void commentsAndBlankLinesAreSkipped() throws Exception {
        File file = posFile(
                "### Module positions - created on Tue 25 Mar 2014 03:42:43 PM PDT ###",
                "## Unit = mm, Angle = deg.",
                "# Ref    Val    Package    PosX    PosY    Rot    Side",
                "",
                "   ",
                "C1       100u   Cap:c      1.0000  2.0000  0.0    F.Cu",
                "## End");

        List<Placement> placements = KicadPosImporter.parseFile(file, Side.Top, true, true, false);

        assertEquals(1, placements.size());
        assertEquals("C1", placements.get(0).getId());
    }

    /**
     * KiCad exports bottom side positions measured from the opposite origin, so the importer
     * mirrors X and reflects the rotation.
     */
    @Test
    public void bottomLayerMirrorsXAndReflectsRotation() throws Exception {
        File file = posFile("C1  100u  Cap:c  10.0000  20.0000  45.0  bottom");

        List<Placement> placements =
                KicadPosImporter.parseFile(file, Side.Bottom, true, true, false);

        Placement c1 = placements.get(0);
        assertEquals(-10.0, c1.getLocation().getX(), DELTA);
        assertEquals(20.0, c1.getLocation().getY(), DELTA, "Y is not mirrored");
        assertEquals(135.0, c1.getLocation().getRotation(), DELTA);
    }

    /**
     * The mirroring above keys off the layer name in the file rather than the side argument, and
     * it matches the lower case "bottom" that older KiCad versions wrote. Current versions write
     * "B.Cu" instead, which does not match, so those files are imported unmirrored. This test
     * pins the present behaviour rather than endorsing it - see the layer handling in
     * KicadPosImporter#parseFile.
     */
    @Test
    public void modernBottomLayerNameIsNotRecognisedAsBottom() throws Exception {
        File file = posFile("C1  100u  Cap:c  10.0000  20.0000  45.0  B.Cu");

        List<Placement> placements =
                KicadPosImporter.parseFile(file, Side.Bottom, true, true, false);

        Placement c1 = placements.get(0);
        assertEquals(10.0, c1.getLocation().getX(), DELTA);
        assertEquals(45.0, c1.getLocation().getRotation(), DELTA);
    }

    /** KiCad can emit -0.0, which should reach the model as a plain zero. */
    @Test
    public void negativeZeroRotationIsNormalised() throws Exception {
        File file = posFile("C1  100u  Cap:c  1.0000  2.0000  -0.0  F.Cu");

        List<Placement> placements = KicadPosImporter.parseFile(file, Side.Top, true, true, false);

        double rotation = placements.get(0).getLocation().getRotation();
        assertEquals(0, Double.compare(0.0, rotation), "rotation should be +0.0, not -0.0");
    }

    @Test
    public void negativeCoordinatesAreParsed() throws Exception {
        File file = posFile("C1  100u  Cap:c  -12.3456  -78.9000  180.0  F.Cu");

        List<Placement> placements = KicadPosImporter.parseFile(file, Side.Top, true, true, false);

        assertEquals(-12.3456, placements.get(0).getLocation().getX(), DELTA);
        assertEquals(-78.9000, placements.get(0).getLocation().getY(), DELTA);
    }
}
