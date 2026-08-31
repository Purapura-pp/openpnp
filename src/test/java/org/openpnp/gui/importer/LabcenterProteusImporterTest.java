package org.openpnp.gui.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.openpnp.model.Part;
import org.openpnp.model.Placement;

/**
 * Covers the Labcenter Proteus .pkp parser. The column order is fixed, but an optional stock code
 * column shifts everything after the package, which the caller signals with a flag rather than the
 * parser detecting it.
 * <p>
 * Layout is {@code "Part ID","Value","Package",[Stock Code,]Layer,Rotation,X,Y}.
 */
public class LabcenterProteusImporterTest {
    private static final double DELTA = 0.0001;

    @TempDir
    private Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        Configuration.initialize(tempDir.resolve(".openpnp").toFile());
        Configuration.get().load();
    }

    private File pkp(String... lines) throws Exception {
        Path file = Files.createTempFile(tempDir, "proteus", ".pkp");
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
        File file = pkp("\"R1\",\"10k\",\"0402\",TOP,270,15.1678,15.24");

        List<Placement> placements = LabcenterProteusImporter.parseFile(file, true, false);

        assertEquals(1, placements.size());
        Placement r1 = placements.get(0);
        assertEquals("R1", r1.getId(), "surrounding quotes are stripped");
        assertEquals(15.1678, r1.getLocation().getX(), DELTA);
        assertEquals(15.24, r1.getLocation().getY(), DELTA);
        assertEquals(270.0, r1.getLocation().getRotation(), DELTA);
        assertEquals(Side.Top, r1.getSide());
        assertEquals("0402-10k", r1.getPart().getId());
    }

    @Test
    public void stockCodeColumnShiftsTheRemainingFields() throws Exception {
        File file = pkp("\"R1\",\"10k\",\"0402\",\"SC-1234\",TOP,270,15.1678,15.24");

        List<Placement> placements = LabcenterProteusImporter.parseFile(file, true, true);

        Placement r1 = placements.get(0);
        assertEquals(15.1678, r1.getLocation().getX(), DELTA);
        assertEquals(15.24, r1.getLocation().getY(), DELTA);
        assertEquals(270.0, r1.getLocation().getRotation(), DELTA);
        assertEquals(Side.Top, r1.getSide());
        assertEquals("0402-10k", r1.getPart().getId());
    }

    @Test
    public void thouUnitsAreConvertedToMillimetres() throws Exception {
        File file = pkp(
                "Units used: thou",
                "\"R1\",\"10k\",\"0402\",TOP,0,1000,2000");

        List<Placement> placements = LabcenterProteusImporter.parseFile(file, true, false);

        assertEquals(25.4, placements.get(0).getLocation().getX(), DELTA);
        assertEquals(50.8, placements.get(0).getLocation().getY(), DELTA);
    }

    @Test
    public void millimetresAreTheDefault() throws Exception {
        File file = pkp("\"R1\",\"10k\",\"0402\",TOP,0,1000,2000");

        List<Placement> placements = LabcenterProteusImporter.parseFile(file, true, false);

        assertEquals(1000.0, placements.get(0).getLocation().getX(), DELTA);
    }

    @Test
    public void theUnitDirectiveOnlyAppliesToRowsAfterIt() throws Exception {
        File file = pkp(
                "\"R1\",\"10k\",\"0402\",TOP,0,1000,1000",
                "Units used: thou",
                "\"R2\",\"10k\",\"0402\",TOP,0,1000,1000");

        List<Placement> placements = LabcenterProteusImporter.parseFile(file, true, false);

        assertEquals(1000.0, byId(placements, "R1").getLocation().getX(), DELTA);
        assertEquals(25.4, byId(placements, "R2").getLocation().getX(), DELTA);
    }

    @Test
    public void linesNotStartingWithAQuoteAreSkipped() throws Exception {
        File file = pkp(
                "Proteus Pick and Place File",
                "Part ID, Value, Package, Layer, Rotation, X, Y",
                "",
                "\"R1\",\"10k\",\"0402\",TOP,0,1,2");

        List<Placement> placements = LabcenterProteusImporter.parseFile(file, true, false);

        assertEquals(1, placements.size(), "headers and commentary are ignored");
    }

    @Test
    public void bottomLayerIsRecognised() throws Exception {
        File file = pkp(
                "\"R1\",\"10k\",\"0402\",TOP,0,1,1",
                "\"R2\",\"10k\",\"0402\",BOTTOM,0,2,2");

        List<Placement> placements = LabcenterProteusImporter.parseFile(file, true, false);

        assertEquals(Side.Top, byId(placements, "R1").getSide());
        assertEquals(Side.Bottom, byId(placements, "R2").getSide());
    }

    @Test
    public void negativeRotationIsPassedThrough() throws Exception {
        File file = pkp("\"R11\",\"22\",\"0402\",TOP,-180,2.6416,11.5062");

        List<Placement> placements = LabcenterProteusImporter.parseFile(file, true, false);

        assertEquals(-180.0, placements.get(0).getLocation().getRotation(), DELTA);
    }

    @Test
    public void createdPartCarriesThePackageColumn() throws Exception {
        File file = pkp("\"R1\",\"10k\",\"0402\",TOP,0,1,2");

        LabcenterProteusImporter.parseFile(file, true, false);

        Part part = Configuration.get().getPart("0402-10k");
        assertNotNull(part);
        assertEquals("0402", part.getPackage().getId());
    }

    @Test
    public void noPartIsAssignedWhenPartCreationIsOff() throws Exception {
        File file = pkp("\"R1\",\"10k\",\"0402\",TOP,0,1,2");

        List<Placement> placements = LabcenterProteusImporter.parseFile(file, false, false);

        assertEquals(1, placements.size(), "the placement is still imported");
        assertNull(placements.get(0).getPart());
    }

    @Test
    public void severalRowsAreImportedInOrder() throws Exception {
        File file = pkp(
                "\"R1\",\"10k\",\"0402\",TOP,270,15.1678,15.24",
                "\"R18\",\"10k\",\"0402\",TOP,270,16.7172,15.24",
                "\"R11\",\"22\",\"0402\",TOP,-180,2.6416,11.5062");

        List<Placement> placements = LabcenterProteusImporter.parseFile(file, true, false);

        assertEquals(3, placements.size());
        assertEquals("R1", placements.get(0).getId());
        assertEquals("R18", placements.get(1).getId());
        assertEquals("R11", placements.get(2).getId());
        assertTrue(placements.get(0).getPart() == placements.get(1).getPart(),
                "R1 and R18 share a package and value");
    }
}
