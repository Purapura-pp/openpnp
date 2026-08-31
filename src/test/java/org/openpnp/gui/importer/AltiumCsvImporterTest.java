package org.openpnp.gui.importer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.openpnp.model.Placement;

/**
 * Covers the Altium flavour of the CSV importer, which narrows the column name patterns of
 * {@link CsvImporter} to the ones Altium actually writes.
 */
public class AltiumCsvImporterTest {
    private static final double DELTA = 0.0001;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        Configuration.initialize(tempDir.resolve(".openpnp").toFile());
        Configuration.get().load();
    }

    private AltiumCsvImporter importer() {
        AltiumCsvImporter importer = new AltiumCsvImporter();
        importer.initPatterns();
        return importer;
    }

    private File csv(String... lines) throws Exception {
        Path file = Files.createTempFile(tempDir, "altium", ".csv");
        Files.write(file, String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
        return file.toFile();
    }

    @Test
    public void columnNamesAreTheAltiumSpellings() {
        AltiumCsvImporter importer = new AltiumCsvImporter();

        assertArrayEquals(new String[] {"DESIGNATOR"}, importer.getReferencePattern());
        assertArrayEquals(new String[] {"COMMENT"}, importer.getValuePattern());
        assertArrayEquals(new String[] {"FOOTPRINT"}, importer.getPackagePattern());
        assertArrayEquals(new String[] {"ROTATION"}, importer.getRotationPattern());
        assertArrayEquals(new String[] {"LAYER"}, importer.getSidePattern());
    }

    @Test
    public void hyphenatedCoordinateColumnsAreRecognised() throws Exception {
        File file = csv(
                "Designator,Comment,Footprint,Ref-X(mm),Ref-Y(mm),Rotation,Layer",
                "C1,100nF,C0603,1.5,2.5,90,T");

        List<Placement> placements = importer().parseFile(file, true, false);

        assertEquals(1, placements.size());
        assertEquals(1.5, placements.get(0).getLocation().getX(), DELTA);
        assertEquals(2.5, placements.get(0).getLocation().getY(), DELTA);
        assertEquals(90.0, placements.get(0).getLocation().getRotation(), DELTA);
        assertEquals("C0603-100nF", placements.get(0).getPart().getId());
    }

    @Test
    public void centerCoordinateColumnsAreAlsoRecognised() throws Exception {
        File file = csv(
                "Designator,Comment,Footprint,Center-X(mm),Center-Y(mm),Rotation,Layer",
                "C1,100nF,C0603,3.5,4.5,0,T");

        List<Placement> placements = importer().parseFile(file, true, false);

        assertEquals(3.5, placements.get(0).getLocation().getX(), DELTA);
        assertEquals(4.5, placements.get(0).getLocation().getY(), DELTA);
    }

    @Test
    public void refColumnsWinOverCenterColumnsWhenBothArePresent() throws Exception {
        File file = csv(
                "Designator,Comment,Footprint,Center-X(mm),Center-Y(mm),Ref-X(mm),Ref-Y(mm),"
                        + "Rotation,Layer",
                "C1,100nF,C0603,3.5,4.5,1.5,2.5,0,T");

        List<Placement> placements = importer().parseFile(file, true, false);

        assertEquals(1.5, placements.get(0).getLocation().getX(), DELTA,
                "Ref-X is listed first in the pattern, so it is matched first");
        assertEquals(2.5, placements.get(0).getLocation().getY(), DELTA);
    }

    @Test
    public void milCoordinateColumnsAreConverted() throws Exception {
        File file = csv(
                "Designator,Comment,Footprint,Ref-X(mil),Ref-Y(mil),Rotation,Layer",
                "C1,100nF,C0603,1000,2000,0,T");

        List<Placement> placements = importer().parseFile(file, true, false);

        assertEquals(25.4, placements.get(0).getLocation().getX(), DELTA);
        assertEquals(50.8, placements.get(0).getLocation().getY(), DELTA);
    }

    @Test
    public void bottomLayerIsRecognised() throws Exception {
        File file = csv(
                "Designator,Comment,Footprint,Ref-X(mm),Ref-Y(mm),Rotation,Layer",
                "C1,100nF,C0603,1,1,0,T",
                "C2,100nF,C0603,2,2,0,B");

        List<Placement> placements = importer().parseFile(file, true, false);

        assertEquals(Side.Top, placements.get(0).getSide());
        assertEquals(Side.Bottom, placements.get(1).getSide());
    }

    /**
     * The bundled samples/altium-example.csv heads its coordinates "Mid X" and "Ref X", with a
     * space and no unit suffix. This importer only knows the hyphenated spellings, so it cannot
     * read that file - {@link ReferenceCsvImporter} is the one that can, because its pattern list
     * includes "REF X".
     * <p>
     * Pinned because the file name suggests otherwise.
     */
    @Test
    public void bundledAltiumSampleIsNotReadableByThisImporter() throws Exception {
        File sample = new File("samples", "altium-example.csv");

        Exception e =
                assertThrows(Exception.class, () -> importer().parseFile(sample, true, false));
        assertTrue(e.getMessage().contains("Unable to find relevant headers"), e.getMessage());
    }

    @Test
    public void bundledAltiumSampleIsReadableByTheReferenceImporter() throws Exception {
        File sample = new File("samples", "altium-example.csv");

        CsvImporter reference = new ReferenceCsvImporter();
        reference.initPatterns();

        assertEquals(29, reference.parseFile(sample, true, false).size());
    }
}
