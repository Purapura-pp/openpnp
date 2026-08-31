package org.openpnp.gui.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openpnp.gui.importer.KicadModImporter.KicadPad;

/**
 * Covers the pad definition parsing of the KiCad footprint importer.
 * <p>
 * Only the parsing is covered. The importer itself opens a file dialog from its constructor, so it
 * cannot be driven from a test without restructuring it.
 */
public class KicadModImporterTest {
    private static final double DELTA = 0.0001;

    private static final String SMD_ROUNDRECT =
            "(pad \"1\" smd roundrect (at -0.7625 0.5) (size 0.775 0.875) "
                    + "(layers \"F.Cu\" \"F.Paste\" \"F.Mask\") (roundrect_rratio 0.25))";

    @Test
    public void nameTypeAndShapeAreReadFromTheHeader() throws Exception {
        KicadPad pad = new KicadPad(SMD_ROUNDRECT);

        assertEquals("1", pad.getName());
        assertEquals("smd", pad.getType());
        assertEquals("roundrect", pad.getShape());
    }

    @Test
    public void unquotedPadNameIsAccepted() throws Exception {
        KicadPad pad = new KicadPad("(pad 2 smd rect (at 1.0 2.0) (size 1.0 1.0))");

        assertEquals("2", pad.getName());
        assertEquals("smd", pad.getType());
        assertEquals("rect", pad.getShape());
    }

    @Test
    public void sizeIsReadAsWidthAndHeight() throws Exception {
        KicadPad pad = new KicadPad(SMD_ROUNDRECT);

        assertEquals(0.775, pad.getWidth(), DELTA);
        assertEquals(0.875, pad.getHeight(), DELTA);
    }

    /** KiCad measures Y upwards where OpenPnP measures it downwards, so the sign is flipped. */
    @Test
    public void yCoordinateIsNegated() throws Exception {
        KicadPad pad = new KicadPad(SMD_ROUNDRECT);

        assertEquals(-0.7625, pad.getX(), DELTA);
        assertEquals(-0.5, pad.getY(), DELTA);
    }

    @Test
    public void negativeYBecomesPositive() throws Exception {
        KicadPad pad = new KicadPad("(pad 1 smd rect (at 1.0 -2.5) (size 1.0 1.0))");

        assertEquals(2.5, pad.getY(), DELTA);
    }

    @Test
    public void rotationIsTheOptionalThirdCoordinate() throws Exception {
        KicadPad rotated = new KicadPad("(pad 1 smd rect (at 1.0 2.0 90) (size 1.0 1.0))");
        KicadPad unrotated = new KicadPad("(pad 1 smd rect (at 1.0 2.0) (size 1.0 1.0))");

        assertEquals(90.0, rotated.getRotation(), DELTA);
        assertEquals(0.0, unrotated.getRotation(), DELTA, "absent rotation reads as zero");
    }

    @Test
    public void roundnessIsExpressedAsAPercentage() throws Exception {
        KicadPad pad = new KicadPad(SMD_ROUNDRECT);

        assertEquals(25.0, pad.getRoundness(), DELTA, "0.25 ratio becomes 25 percent");
    }

    @Test
    public void roundnessIsZeroWhenAbsent() throws Exception {
        KicadPad pad = new KicadPad("(pad 1 smd rect (at 1.0 2.0) (size 1.0 1.0))");

        assertEquals(0.0, pad.getRoundness(), DELTA);
    }

    @Test
    public void frontCopperLayerIsDetected() throws Exception {
        KicadPad pad = new KicadPad(SMD_ROUNDRECT);

        assertTrue(pad.isTopCu());
    }

    @Test
    public void throughHolePadOnAllCopperLayersCountsAsFront() throws Exception {
        KicadPad pad = new KicadPad(
                "(pad 1 thru_hole circle (at 0 0) (size 1.7 1.7) (drill 1.0) "
                        + "(layers \"*.Cu\" \"*.Mask\"))");

        assertTrue(pad.isTopCu(), "*.Cu spans every copper layer, front included");
    }

    @Test
    public void backCopperOnlyPadIsNotFront() throws Exception {
        KicadPad pad = new KicadPad(
                "(pad 1 smd rect (at 0 0) (size 1.0 1.0) (layers \"B.Cu\" \"B.Paste\"))");

        assertFalse(pad.isTopCu());
    }

    @Test
    public void missingLayersReadAsNotFront() throws Exception {
        KicadPad pad = new KicadPad("(pad 1 smd rect (at 0 0) (size 1.0 1.0))");

        assertFalse(pad.isTopCu());
    }

    @Test
    public void unparseableDefinitionYieldsNeutralValues() throws Exception {
        KicadPad pad = new KicadPad("(fp_text reference REF** (at 0 0))");

        assertEquals("", pad.getName());
        assertEquals("", pad.getType());
        assertEquals("", pad.getShape());
        assertEquals(0.0, pad.getWidth(), DELTA);
        assertEquals(0.0, pad.getHeight(), DELTA);
    }
}
