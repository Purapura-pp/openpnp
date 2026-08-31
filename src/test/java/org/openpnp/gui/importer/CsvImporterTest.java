package org.openpnp.gui.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.openpnp.model.Part;
import org.openpnp.model.Placement;

/**
 * Covers the shared CSV centroid parser through {@link ReferenceCsvImporter}, whose column name
 * patterns are the broadest of the CSV importers.
 * <p>
 * The parser finds its columns by matching the upper cased header line against those patterns, and
 * it needs all six of reference, value, package, X, Y and rotation before it accepts a line as the
 * header. So most of what is worth pinning down is which spellings are recognised and how the
 * values are converted.
 */
public class CsvImporterTest {
    private static final double DELTA = 0.0001;

    /** A real Altium centroid export checked into the repository. */
    private static final File ALTIUM_SAMPLE = new File("samples", "altium-example.csv");

    /** Reference, value, package, X, Y, rotation and side, in that column order. */
    private static final String HEADER = "Designator,Value,Footprint,Ref X,Ref Y,Rotation,Layer";

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        // parseFile resolves and creates parts through the Configuration singleton.
        Configuration.initialize(tempDir.resolve(".openpnp").toFile());
        Configuration.get().load();
    }

    private CsvImporter importer() {
        CsvImporter importer = new ReferenceCsvImporter();
        importer.initPatterns();
        return importer;
    }

    private File csv(String... lines) throws Exception {
        Path file = Files.createTempFile(tempDir, "centroid", ".csv");
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
    public void everyDataRowOfTheSampleBecomesAPlacement() throws Exception {
        List<Placement> placements = importer().parseFile(ALTIUM_SAMPLE, true, false);

        assertEquals(29, placements.size());
    }

    @Test
    public void placementIsParsedInFull() throws Exception {
        // "C1","NICHICON_A","58.674mm","7.2263mm","58.674mm","7.239mm",...,"T","90.00","10uF"
        // X and Y come from the Ref X / Ref Y columns rather than Mid X / Mid Y, because "REF X"
        // appears earlier in the pattern list than any of the Mid spellings.
        List<Placement> placements = importer().parseFile(ALTIUM_SAMPLE, true, false);

        Placement c1 = byId(placements, "C1");
        assertEquals(58.674, c1.getLocation().getX(), DELTA);
        assertEquals(7.239, c1.getLocation().getY(), DELTA);
        assertEquals(90.0, c1.getLocation().getRotation(), DELTA);
        assertEquals(Side.Top, c1.getSide());
        assertEquals("NICHICON_A-10uF", c1.getPart().getId());
    }

    @Test
    public void unitSuffixIsStrippedFromCoordinates() throws Exception {
        List<Placement> placements = importer().parseFile(ALTIUM_SAMPLE, true, false);

        // "91.186mm" must not come through as 91186, nor fail to parse.
        assertEquals(91.186, byId(placements, "R6").getLocation().getX(), DELTA);
    }

    @Test
    public void createdPartCarriesThePackageColumnAsItsPackage() throws Exception {
        importer().parseFile(ALTIUM_SAMPLE, true, false);

        Part part = Configuration.get().getPart("NICHICON_A-10uF");
        assertNotNull(part);
        assertEquals("NICHICON_A", part.getPackage().getId());
    }

    @Test
    public void placementsAreSkippedWhenTheirPartIsUnknownAndNotCreated() throws Exception {
        List<Placement> placements = importer().parseFile(ALTIUM_SAMPLE, false, false);

        assertTrue(placements.isEmpty(),
                "with an empty configuration and part creation off, every row is skipped");
    }

    @Test
    public void headerIsMatchedCaseInsensitively() throws Exception {
        File file = csv(
                "designator,value,footprint,ref x,ref y,rotation,layer",
                "C1,100nF,C0603,1.5,2.5,0,T");

        List<Placement> placements = importer().parseFile(file, true, false);

        assertEquals(1, placements.size());
        assertEquals(1.5, placements.get(0).getLocation().getX(), DELTA);
    }

    @Test
    public void tabSeparatedFilesAreRecognised() throws Exception {
        File file = csv(
                "Designator\tValue\tFootprint\tRef X\tRef Y\tRotation\tLayer",
                "C1\t100nF\tC0603\t1.5\t2.5\t0\tT");

        List<Placement> placements = importer().parseFile(file, true, false);

        assertEquals(1, placements.size());
        assertEquals(1.5, placements.get(0).getLocation().getX(), DELTA);
        assertEquals(2.5, placements.get(0).getLocation().getY(), DELTA);
    }

    @Test
    public void headerMayBePrefixedWithAHash() throws Exception {
        File file = csv(
                "# " + HEADER,
                "C1,100nF,C0603,1.5,2.5,0,T");

        List<Placement> placements = importer().parseFile(file, true, false);

        assertEquals(1, placements.size());
    }

    @Test
    public void headerIsSearchedForPastLeadingCommentary() throws Exception {
        File file = csv(
                "Pick and Place Locations",
                "Date: 2024-01-01",
                "Units used: mm",
                "",
                HEADER,
                "C1,100nF,C0603,1.5,2.5,0,T");

        List<Placement> placements = importer().parseFile(file, true, false);

        assertEquals(1, placements.size());
    }

    @Test
    public void milColumnsAreConvertedToMillimetres() throws Exception {
        File file = csv(
                "Designator,Value,Footprint,REF-X(MIL),REF-Y(MIL),Rotation,Layer",
                "C1,100nF,C0603,1000,2000,0,T");

        List<Placement> placements = importer().parseFile(file, true, false);

        assertEquals(25.4, placements.get(0).getLocation().getX(), DELTA);
        assertEquals(50.8, placements.get(0).getLocation().getY(), DELTA);
    }

    @Test
    public void quotedDecimalCommaIsAccepted() throws Exception {
        File file = csv(
                HEADER,
                "C1,100nF,C0603,\"1,5\",\"2,5\",0,T");

        List<Placement> placements = importer().parseFile(file, true, false);

        assertEquals(1.5, placements.get(0).getLocation().getX(), DELTA);
        assertEquals(2.5, placements.get(0).getLocation().getY(), DELTA);
    }

    @Test
    public void bottomSideIsRecognisedFromTheLayerColumn() throws Exception {
        File file = csv(
                HEADER,
                "C1,100nF,C0603,1,1,0,T",
                "C2,100nF,C0603,2,2,0,B",
                "C3,100nF,C0603,3,3,0,BottomLayer",
                "C4,100nF,C0603,4,4,0,Yes");

        List<Placement> placements = importer().parseFile(file, true, false);

        assertEquals(Side.Top, byId(placements, "C1").getSide());
        assertEquals(Side.Bottom, byId(placements, "C2").getSide());
        assertEquals(Side.Bottom, byId(placements, "C3").getSide());
        assertEquals(Side.Bottom, byId(placements, "C4").getSide(),
                "only the first character is examined, so a leading Y also means bottom");
    }

    @Test
    public void rotationIsNormalisedToPlusMinus180() throws Exception {
        File file = csv(
                HEADER,
                "C1,100nF,C0603,1,1,270,T",
                "C2,100nF,C0603,2,2,360,T",
                "C3,100nF,C0603,3,3,-450,T");

        List<Placement> placements = importer().parseFile(file, true, false);

        assertEquals(-90.0, byId(placements, "C1").getLocation().getRotation(), DELTA);
        assertEquals(0.0, byId(placements, "C2").getLocation().getRotation(), DELTA);
        assertEquals(-90.0, byId(placements, "C3").getLocation().getRotation(), DELTA);
    }

    @Test
    public void fiducialsAreDetectedByTheirReference() throws Exception {
        File file = csv(
                HEADER,
                "FID1,FIDUCIAL,FIDUCIAL,1,1,0,T",
                "REF2,FIDUCIAL,FIDUCIAL,2,2,0,T",
                "FIDUCIAL,FIDUCIAL,FIDUCIAL,3,3,0,T",
                "C1,100nF,C0603,4,4,0,T");

        List<Placement> placements = importer().parseFile(file, true, false);

        assertEquals(Placement.Type.Fiducial, byId(placements, "FID1").getType());
        assertEquals(Placement.Type.Fiducial, byId(placements, "REF2").getType());
        assertEquals(Placement.Type.Placement, byId(placements, "FIDUCIAL").getType(),
                "the fourth character has to be a digit");
        assertEquals(Placement.Type.Placement, byId(placements, "C1").getType());
    }

    @Test
    public void heightColumnSetsThePartHeightOnCreation() throws Exception {
        File file = csv(
                HEADER + ",Height",
                "C1,100nF,C0603,1,1,0,T,0.8");

        importer().parseFile(file, true, false);

        Part part = Configuration.get().getPart("C0603-100nF");
        assertNotNull(part);
        assertEquals(0.8, part.getHeight().getValue(), DELTA);
    }

    @Test
    public void missingRequiredColumnIsReported() throws Exception {
        // No rotation column, which is one of the six the parser insists on.
        File file = csv(
                "Designator,Value,Footprint,Ref X,Ref Y,Layer",
                "C1,100nF,C0603,1,1,T");

        Exception e = assertThrows(Exception.class, () -> importer().parseFile(file, true, false));
        assertTrue(e.getMessage().contains("Unable to find relevant headers"), e.getMessage());
    }

    @Test
    public void semicolonSeparatedFilesAreNotRecognised() throws Exception {
        // Separator detection only tries comma and then tab.
        File file = csv(
                HEADER.replace(',', ';'),
                "C1;100nF;C0603;1.5;2.5;0;T");

        Exception e = assertThrows(Exception.class, () -> importer().parseFile(file, true, false));
        assertTrue(e.getMessage().contains("Unable to find relevant headers"), e.getMessage());
    }

    @Test
    public void shortRowsAreSkipped() throws Exception {
        File file = csv(
                HEADER,
                "C1,100nF,C0603,1,1,0,T",
                "C2,100nF",
                "C3,100nF,C0603,3,3,0,T");

        List<Placement> placements = importer().parseFile(file, true, false);

        assertEquals(2, placements.size(), "the truncated row is dropped rather than failing");
    }
}
