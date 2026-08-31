package org.openpnp.machine.reference.feeder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Feeder;

/**
 * Covers the pocket geometry of the rotated tray feeder.
 * <p>
 * Three things combine here and each can be got wrong independently: the tray is walked row by row,
 * row numbers grow towards negative Y, and the whole pocket grid is rotated by the tray angle while
 * the part angle is a separate offset added on top.
 */
public class ReferenceRotatedTrayFeederTest {
    private static final double DELTA = 1e-9;

    @TempDir
    private Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        Configuration.initialize(tempDir.resolve(".openpnp").toFile());
        Configuration.get().load();
    }

    /** A tray at (10, 20) with 2 mm column pitch and 3 mm row pitch, unrotated. */
    private static ReferenceRotatedTrayFeeder tray(int cols, int rows) {
        ReferenceRotatedTrayFeeder feeder = new ReferenceRotatedTrayFeeder();
        feeder.setLocation(new Location(LengthUnit.Millimeters, 10, 20, 0, 0));
        feeder.setOffsets(new Location(LengthUnit.Millimeters, 2, 3, 0, 0));
        feeder.setTrayCountCols(cols);
        feeder.setTrayCountRows(rows);
        return feeder;
    }

    private static void assertPickLocation(double x, double y,
            ReferenceRotatedTrayFeeder feeder) {
        Location actual = feeder.getPickLocation().convertToUnits(LengthUnit.Millimeters);
        assertEquals(x, actual.getX(), DELTA, "X of pocket " + feeder.getFeedCount());
        assertEquals(y, actual.getY(), DELTA, "Y of pocket " + feeder.getFeedCount());
    }

    @Test
    public void anUnfedTrayPicksFromItsOwnLocation() {
        ReferenceRotatedTrayFeeder feeder = tray(3, 2);

        assertEquals(0, feeder.getFeedCount());
        assertPickLocation(10, 20, feeder);
    }

    /** The default order is along a row until it runs out, then on to the next row. */
    @Test
    public void theTrayIsWalkedRowByRow() {
        ReferenceRotatedTrayFeeder feeder = tray(3, 2);

        feeder.setFeedCount(1);
        assertPickLocation(10, 20, feeder);
        feeder.setFeedCount(2);
        assertPickLocation(12, 20, feeder);
        feeder.setFeedCount(3);
        assertPickLocation(14, 20, feeder);
        feeder.setFeedCount(4);
        assertPickLocation(10, 17, feeder);
        feeder.setFeedCount(5);
        assertPickLocation(12, 17, feeder);
        feeder.setFeedCount(6);
        assertPickLocation(14, 17, feeder);
    }

    /**
     * Row one is the first row and rows count downwards in Y, which is how the tray is described
     * on the configuration screen.
     */
    @Test
    public void rowsAdvanceTowardsNegativeY() {
        ReferenceRotatedTrayFeeder feeder = tray(1, 3);

        feeder.setFeedCount(1);
        assertPickLocation(10, 20, feeder);
        feeder.setFeedCount(2);
        assertPickLocation(10, 17, feeder);
        feeder.setFeedCount(3);
        assertPickLocation(10, 14, feeder);
    }

    /** Unlike the plain tray feeder, the traversal order does not depend on the tray shape. */
    @Test
    public void aTallTrayIsStillWalkedRowByRow() {
        ReferenceRotatedTrayFeeder feeder = tray(2, 3);

        feeder.setFeedCount(2);
        assertPickLocation(12, 20, feeder);
        feeder.setFeedCount(3);
        assertPickLocation(10, 17, feeder);
    }

    /** A quarter turn of the tray sends the column pitch along +Y and the row pitch along +X. */
    @Test
    public void theTrayAngleRotatesThePocketGrid() {
        ReferenceRotatedTrayFeeder feeder = tray(2, 2);
        feeder.setLocation(new Location(LengthUnit.Millimeters, 10, 20, 0, 90));

        feeder.setFeedCount(2);
        assertPickLocation(10, 22, feeder);

        feeder.setFeedCount(3);
        assertPickLocation(13, 20, feeder);
    }

    @Test
    public void aHalfTurnMirrorsBothPitches() {
        ReferenceRotatedTrayFeeder feeder = tray(2, 2);
        feeder.setLocation(new Location(LengthUnit.Millimeters, 10, 20, 0, 180));

        feeder.setFeedCount(2);
        assertPickLocation(8, 20, feeder);

        feeder.setFeedCount(3);
        assertPickLocation(10, 23, feeder);
    }

    @Test
    public void theTrayAngleAloneDoesNotRotateThePart() {
        ReferenceRotatedTrayFeeder feeder = tray(2, 2);
        feeder.setLocation(new Location(LengthUnit.Millimeters, 10, 20, 0, 90));

        assertEquals(90, feeder.getPickLocation().getRotation(), DELTA);
    }

    /** The part angle is an offset on top of the tray angle, not a replacement for it. */
    @Test
    public void partRotationIsAddedToTheTrayAngle() {
        ReferenceRotatedTrayFeeder feeder = tray(2, 2);
        feeder.setLocation(new Location(LengthUnit.Millimeters, 10, 20, 0, 30));
        feeder.setComponentRotationInTray(45);

        assertEquals(75, feeder.getPickLocation().getRotation(), DELTA);
    }

    @Test
    public void partRotationAppliesToEveryPocket() {
        ReferenceRotatedTrayFeeder feeder = tray(2, 2);
        feeder.setComponentRotationInTray(-90);

        feeder.setFeedCount(1);
        assertEquals(-90, feeder.getPickLocation().getRotation(), DELTA);
        feeder.setFeedCount(4);
        assertEquals(-90, feeder.getPickLocation().getRotation(), DELTA);
        assertPickLocation(12, 17, feeder);
    }

    @Test
    public void aFeedCountBeyondTheTrayClampsToTheLastPocket() {
        ReferenceRotatedTrayFeeder feeder = tray(3, 2);

        feeder.setFeedCount(99);

        assertPickLocation(14, 17, feeder);
    }

    @Test
    public void nonPositiveTrayCountsCountAsOne() {
        ReferenceRotatedTrayFeeder feeder = tray(0, -5);

        assertEquals(1, feeder.getEffectiveTrayCountCols());
        assertEquals(1, feeder.getEffectiveTrayCountRows());

        feeder.setFeedCount(7);
        assertPickLocation(10, 20, feeder);
    }

    @Test
    public void offsetsInOtherUnitsAreConverted() {
        ReferenceRotatedTrayFeeder feeder = tray(2, 1);
        feeder.setOffsets(new Location(LengthUnit.Inches, 1, 0, 0, 0));

        feeder.setFeedCount(2);

        assertPickLocation(35.4, 20, feeder);
    }

    @Test
    public void feedAdvancesTheCount() throws Exception {
        ReferenceRotatedTrayFeeder feeder = tray(3, 2);

        feeder.feed(null);
        assertEquals(1, feeder.getFeedCount());
        feeder.feed(null);
        assertEquals(2, feeder.getFeedCount());
    }

    @Test
    public void feedingAnExhaustedTrayReportsItEmpty() throws Exception {
        ReferenceRotatedTrayFeeder feeder = tray(2, 1);
        feeder.setFeedCount(2);

        assertThrows(Feeder.FeederEmptyException.class, () -> feeder.feed(null));
    }

    @Test
    public void theLastPocketOfATrayCanStillBeFed() throws Exception {
        ReferenceRotatedTrayFeeder feeder = tray(2, 1);
        feeder.setFeedCount(1);

        feeder.feed(null);

        assertEquals(2, feeder.getFeedCount());
        assertPickLocation(12, 20, feeder);
    }

    @Test
    public void aPartCanOnlyBeTakenBackAfterAFeed() {
        ReferenceRotatedTrayFeeder feeder = tray(3, 2);

        assertFalse(feeder.canTakeBackPart());

        feeder.setFeedCount(1);
        assertTrue(feeder.canTakeBackPart());
    }
}
