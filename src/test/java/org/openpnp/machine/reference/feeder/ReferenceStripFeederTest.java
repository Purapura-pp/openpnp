package org.openpnp.machine.reference.feeder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openpnp.machine.reference.ReferenceFeeder.FeedOptions;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;

/**
 * Covers the tape geometry of the strip feeder: where along the tape a part sits, how far the part
 * is offset from the sprocket holes, and what angle it is picked at.
 * <p>
 * Vision is switched off throughout, which is what keeps this reachable from a test - with vision
 * enabled a feed needs a camera. The geometry itself never needs one: without a vision correction
 * the tape line is simply the configured reference and last hole.
 * <p>
 * Note the local frame is rotated: the part offsets are given as (lateral, linear) and then turned
 * by the tape angle, so a tape running along +X puts the part at -2, +3.5 relative to its
 * reference hole.
 */
public class ReferenceStripFeederTest {
    private static final double DELTA = 1e-9;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        Configuration.initialize(tempDir.resolve(".openpnp").toFile());
        Configuration.get().load();
    }

    /**
     * A feeder whose tape runs from the origin to the given last hole. Defaults elsewhere: 8 mm
     * tape, 4 mm part and hole pitch, 2 mm from the reference hole to the first part.
     */
    private static ReferenceStripFeeder strip(double lastX, double lastY) {
        ReferenceStripFeeder feeder = new ReferenceStripFeeder();
        feeder.setVisionEnabled(false);
        feeder.setReferenceHoleLocation(new Location(LengthUnit.Millimeters, 0, 0, 0, 0));
        feeder.setLastHoleLocation(new Location(LengthUnit.Millimeters, lastX, lastY, 0, 0));
        return feeder;
    }

    private static void assertPick(double x, double y, double rotation,
            ReferenceStripFeeder feeder) throws Exception {
        Location actual = feeder.getPickLocation().convertToUnits(LengthUnit.Millimeters);
        assertEquals(x, actual.getX(), DELTA, "X at feed " + feeder.getFeedCount());
        assertEquals(y, actual.getY(), DELTA, "Y at feed " + feeder.getFeedCount());
        assertEquals(rotation, actual.getRotation(), DELTA,
                "rotation at feed " + feeder.getFeedCount());
    }

    @Test
    public void anUnfedFeederPicksAsIfItHadBeenFedOnce() throws Exception {
        ReferenceStripFeeder feeder = strip(16, 0);

        assertEquals(0, feeder.getFeedCount());
        assertPick(-2, 3.5, 180, feeder);
    }

    @Test
    public void partsAdvanceAlongTheTapeByThePartPitch() throws Exception {
        ReferenceStripFeeder feeder = strip(16, 0);

        feeder.setFeedCount(1);
        assertPick(-2, 3.5, 180, feeder);
        feeder.setFeedCount(2);
        assertPick(2, 3.5, 180, feeder);
        feeder.setFeedCount(3);
        assertPick(6, 3.5, 180, feeder);
        feeder.setFeedCount(5);
        assertPick(14, 3.5, 180, feeder);
    }

    /** The lateral offset is half the tape width less half a millimetre. */
    @Test
    public void theLateralOffsetFollowsTheTapeWidth() throws Exception {
        ReferenceStripFeeder narrow = strip(16, 0);
        narrow.setFeedCount(1);
        assertPick(-2, 3.5, 180, narrow);

        ReferenceStripFeeder wide = strip(16, 0);
        wide.setTapeWidth(new Length(12, LengthUnit.Millimeters));
        wide.setFeedCount(1);
        assertPick(-2, 5.5, 180, wide);
    }

    @Test
    public void aTapeRunningAlongYIsPickedAtMinusNinety() throws Exception {
        ReferenceStripFeeder feeder = strip(0, 16);

        feeder.setFeedCount(1);
        assertPick(-3.5, -2, -90, feeder);
        feeder.setFeedCount(2);
        assertPick(-3.5, 2, -90, feeder);
    }

    @Test
    public void aTapeRunningBackwardsAlongXIsPickedAtZero() throws Exception {
        ReferenceStripFeeder feeder = strip(-16, 0);

        feeder.setFeedCount(1);
        assertPick(2, -3.5, 0, feeder);
        feeder.setFeedCount(2);
        assertPick(-2, -3.5, 0, feeder);
    }

    @Test
    public void aDensePartPitchPacksPartsCloserTogether() throws Exception {
        ReferenceStripFeeder feeder = strip(16, 0);
        feeder.setPartPitch(new Length(2, LengthUnit.Millimeters));

        feeder.setFeedCount(1);
        assertPick(-2, 3.5, 180, feeder);
        feeder.setFeedCount(2);
        assertPick(0, 3.5, 180, feeder);
        feeder.setFeedCount(3);
        assertPick(2, 3.5, 180, feeder);
    }

    @Test
    public void theFeederRotationIsAddedToTheTapeAngle() throws Exception {
        ReferenceStripFeeder feeder = strip(16, 0);
        feeder.setLocation(new Location(LengthUnit.Millimeters, 0, 0, 0, 30));

        feeder.setFeedCount(1);

        assertPick(-2, 3.5, 210, feeder);
    }

    /**
     * The part spacing is calibrated against the measured tape rather than taken from the
     * configuration directly: the span is divided by the number of hole pitches it contains, so a
     * tape measured 1 mm short packs its parts 3.75 mm apart instead of 4.
     */
    @Test
    public void partSpacingIsCalibratedToTheMeasuredTapeLength() throws Exception {
        ReferenceStripFeeder feeder = strip(15, 0);

        feeder.setFeedCount(2);

        assertPick(1.75, 3.5, 180, feeder);
    }

    /** Only the ratio of part pitch to hole pitch matters, not their absolute values. */
    @Test
    public void holePitchEntersOnlyThroughItsRatioToPartPitch() throws Exception {
        ReferenceStripFeeder feeder = strip(16, 0);
        feeder.setHolePitch(new Length(8, LengthUnit.Millimeters));
        feeder.setPartPitch(new Length(8, LengthUnit.Millimeters));

        feeder.setFeedCount(2);

        assertPick(6, 3.5, 180, feeder);
    }

    /**
     * With the reference and last hole set to the same point there is no tape direction to walk
     * along, and the unit vector is computed by dividing by a zero length. The source comment at
     * that branch claims coincident points behave reasonably; they do not, the coordinate comes
     * out as not-a-number. Pinned so the claim and the behaviour can be reconciled.
     */
    @Test
    public void coincidentReferenceAndLastHoleYieldNotANumber() throws Exception {
        ReferenceStripFeeder feeder = strip(0, 0);

        feeder.setFeedCount(1);
        Location pick = feeder.getPickLocation();

        assertTrue(Double.isNaN(pick.getX()), "X should be NaN, was " + pick.getX());
        assertTrue(Double.isNaN(pick.getY()), "Y should be NaN, was " + pick.getY());
    }

    @Test
    public void withoutVisionTheTapeLineIsTheConfiguredHoles() {
        ReferenceStripFeeder feeder = strip(16, 0);

        Location[] line = feeder.getIdealLineLocations();

        assertEquals(2, line.length);
        assertEquals(0, line[0].getX(), DELTA);
        assertEquals(16, line[1].getX(), DELTA);
    }

    @Test
    public void feedAdvancesTheCount() throws Exception {
        ReferenceStripFeeder feeder = strip(16, 0);

        feeder.feed(null);
        assertEquals(1, feeder.getFeedCount());
        feeder.feed(null);
        assertEquals(2, feeder.getFeedCount());
    }

    @Test
    public void aMaxFeedCountOfZeroMeansUnlimited() throws Exception {
        ReferenceStripFeeder feeder = strip(16, 0);
        feeder.setMaxFeedCount(0);
        feeder.setFeedCount(999);

        feeder.feed(null);

        assertEquals(1000, feeder.getFeedCount());
    }

    @Test
    public void feedingPastTheMaxFeedCountReportsTheFeederEmpty() throws Exception {
        ReferenceStripFeeder feeder = strip(16, 0);
        feeder.setPart(new Part("TEST-PART"));
        feeder.setMaxFeedCount(2);
        feeder.setFeedCount(2);

        assertThrows(Feeder.FeederEmptyException.class, () -> feeder.feed(null));
    }

    @Test
    public void theLastPartOfATapeCanStillBeFed() throws Exception {
        ReferenceStripFeeder feeder = strip(16, 0);
        feeder.setPart(new Part("TEST-PART"));
        feeder.setMaxFeedCount(2);
        feeder.setFeedCount(1);

        feeder.feed(null);

        assertEquals(2, feeder.getFeedCount());
    }

    @Test
    public void skipNextSuppressesASingleAdvance() throws Exception {
        ReferenceStripFeeder feeder = strip(16, 0);
        feeder.setFeedCount(1);
        feeder.setFeedOptions(FeedOptions.SkipNext);

        feeder.feed(null);

        assertEquals(1, feeder.getFeedCount());
        assertEquals(FeedOptions.Normal, feeder.getFeedOptions());
    }
}
