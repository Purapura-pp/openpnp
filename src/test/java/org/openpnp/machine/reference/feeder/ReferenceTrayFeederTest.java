package org.openpnp.machine.reference.feeder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openpnp.machine.reference.ReferenceFeeder.FeedOptions;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;

/**
 * Covers the pocket indexing and feed counting of the tray feeder.
 * <p>
 * Which way the tray is walked depends on its shape: the longer of the two counts becomes the
 * major axis, so a 3x2 tray and a 2x3 tray are traversed in different orders. That choice is
 * invisible from the configuration screen, so it is pinned here.
 */
public class ReferenceTrayFeederTest {
    private static final double DELTA = 1e-9;

    @TempDir
    private Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        Configuration.initialize(tempDir.resolve(".openpnp").toFile());
        Configuration.get().load();
    }

    /** A tray at (10, 20) with 2 mm column pitch and 3 mm row pitch. */
    private static ReferenceTrayFeeder tray(int countX, int countY) {
        ReferenceTrayFeeder feeder = new ReferenceTrayFeeder();
        feeder.setLocation(new Location(LengthUnit.Millimeters, 10, 20, 0, 0));
        feeder.setOffsets(new Location(LengthUnit.Millimeters, 2, 3, 0, 0));
        feeder.setTrayCountX(countX);
        feeder.setTrayCountY(countY);
        feeder.setPart(new Part("TEST-PART"));
        return feeder;
    }

    private static void assertPickLocation(double x, double y, ReferenceTrayFeeder feeder) {
        Location actual = feeder.getPickLocation().convertToUnits(LengthUnit.Millimeters);
        assertEquals(x, actual.getX(), DELTA, "X of pocket " + feeder.getFeedCount());
        assertEquals(y, actual.getY(), DELTA, "Y of pocket " + feeder.getFeedCount());
    }

    @Test
    public void anUnfedTrayPicksFromItsOwnLocation() {
        ReferenceTrayFeeder feeder = tray(3, 2);

        assertEquals(0, feeder.getFeedCount());
        assertPickLocation(10, 20, feeder);
    }

    @Test
    public void theFirstPocketIsTheFeederLocation() {
        ReferenceTrayFeeder feeder = tray(3, 2);
        feeder.setFeedCount(1);

        assertPickLocation(10, 20, feeder);
    }

    /** With more columns than rows, each column is filled before moving across. */
    @Test
    public void aWideTrayIsWalkedColumnByColumn() {
        ReferenceTrayFeeder feeder = tray(3, 2);

        feeder.setFeedCount(1);
        assertPickLocation(10, 20, feeder);
        feeder.setFeedCount(2);
        assertPickLocation(10, 23, feeder);
        feeder.setFeedCount(3);
        assertPickLocation(12, 20, feeder);
        feeder.setFeedCount(4);
        assertPickLocation(12, 23, feeder);
        feeder.setFeedCount(5);
        assertPickLocation(14, 20, feeder);
        feeder.setFeedCount(6);
        assertPickLocation(14, 23, feeder);
    }

    /** With more rows than columns the traversal switches to row by row. */
    @Test
    public void aTallTrayIsWalkedRowByRow() {
        ReferenceTrayFeeder feeder = tray(2, 3);

        feeder.setFeedCount(1);
        assertPickLocation(10, 20, feeder);
        feeder.setFeedCount(2);
        assertPickLocation(12, 20, feeder);
        feeder.setFeedCount(3);
        assertPickLocation(10, 23, feeder);
        feeder.setFeedCount(4);
        assertPickLocation(12, 23, feeder);
        feeder.setFeedCount(5);
        assertPickLocation(10, 26, feeder);
        feeder.setFeedCount(6);
        assertPickLocation(12, 26, feeder);
    }

    /** A square tray takes the column by column branch, since the test is >= rather than >. */
    @Test
    public void aSquareTrayIsWalkedColumnByColumn() {
        ReferenceTrayFeeder feeder = tray(2, 2);

        feeder.setFeedCount(2);
        assertPickLocation(10, 23, feeder);
        feeder.setFeedCount(3);
        assertPickLocation(12, 20, feeder);
    }

    @Test
    public void aSingleRowTrayAdvancesAlongX() {
        ReferenceTrayFeeder feeder = tray(4, 1);

        feeder.setFeedCount(1);
        assertPickLocation(10, 20, feeder);
        feeder.setFeedCount(2);
        assertPickLocation(12, 20, feeder);
        feeder.setFeedCount(4);
        assertPickLocation(16, 20, feeder);
    }

    @Test
    public void aSingleColumnTrayAdvancesAlongY() {
        ReferenceTrayFeeder feeder = tray(1, 4);

        feeder.setFeedCount(1);
        assertPickLocation(10, 20, feeder);
        feeder.setFeedCount(2);
        assertPickLocation(10, 23, feeder);
        feeder.setFeedCount(4);
        assertPickLocation(10, 29, feeder);
    }

    @Test
    public void aFeedCountBeyondTheTrayClampsToTheLastPocket() {
        ReferenceTrayFeeder feeder = tray(3, 2);

        feeder.setFeedCount(99);

        assertPickLocation(14, 23, feeder);
    }

    /**
     * A tray configured with no rows or columns still has to yield one usable pocket rather than
     * dividing by zero. Regression for the tray counts below one.
     */
    @Test
    public void nonPositiveTrayCountsCountAsOne() {
        ReferenceTrayFeeder feeder = tray(0, -5);

        assertEquals(1, feeder.getEffectiveTrayCountX());
        assertEquals(1, feeder.getEffectiveTrayCountY());

        feeder.setFeedCount(1);
        assertPickLocation(10, 20, feeder);
        feeder.setFeedCount(7);
        assertPickLocation(10, 20, feeder);
    }

    @Test
    public void offsetsInOtherUnitsAreConverted() {
        ReferenceTrayFeeder feeder = tray(2, 1);
        feeder.setOffsets(new Location(LengthUnit.Inches, 1, 0, 0, 0));

        feeder.setFeedCount(2);

        assertPickLocation(35.4, 20, feeder);
    }

    @Test
    public void theRotationOfTheFeederLocationIsKept() {
        ReferenceTrayFeeder feeder = tray(2, 1);
        feeder.setLocation(new Location(LengthUnit.Millimeters, 10, 20, 0, 45));

        feeder.setFeedCount(2);

        assertEquals(45, feeder.getPickLocation().getRotation(), DELTA,
                "the pocket offset must not rotate the part");
    }

    @Test
    public void feedAdvancesTheCount() throws Exception {
        ReferenceTrayFeeder feeder = tray(3, 2);

        feeder.feed(null);
        assertEquals(1, feeder.getFeedCount());
        feeder.feed(null);
        assertEquals(2, feeder.getFeedCount());
    }

    @Test
    public void feedingAnExhaustedTrayReportsItEmpty() throws Exception {
        ReferenceTrayFeeder feeder = tray(2, 1);
        feeder.setFeedCount(2);

        assertThrows(Feeder.FeederEmptyException.class, () -> feeder.feed(null));
    }

    @Test
    public void theLastPocketOfATrayCanStillBeFed() throws Exception {
        ReferenceTrayFeeder feeder = tray(2, 1);
        feeder.setFeedCount(1);

        feeder.feed(null);

        assertEquals(2, feeder.getFeedCount());
        assertPickLocation(12, 20, feeder);
    }

    /** Skipping suppresses one advance and then puts itself back to normal. */
    @Test
    public void skipNextSuppressesASingleAdvance() throws Exception {
        ReferenceTrayFeeder feeder = tray(3, 2);
        feeder.setFeedCount(1);
        feeder.setFeedOptions(FeedOptions.SkipNext);

        feeder.feed(null);
        assertEquals(1, feeder.getFeedCount(), "the skipped feed must not advance the pocket");
        assertEquals(FeedOptions.Normal, feeder.getFeedOptions(), "skipping is a one shot");

        feeder.feed(null);
        assertEquals(2, feeder.getFeedCount());
    }

    /**
     * The suppression is bypassed while the count is still zero, so a fresh feeder always lands on
     * its first pocket no matter which option is set.
     */
    @Test
    public void theVeryFirstFeedAdvancesEvenWhenSkipping() throws Exception {
        ReferenceTrayFeeder feeder = tray(3, 2);
        feeder.setFeedOptions(FeedOptions.SkipNext);

        feeder.feed(null);

        assertEquals(1, feeder.getFeedCount());
    }

    @Test
    public void disableSuppressesEveryAdvanceAndStays() throws Exception {
        ReferenceTrayFeeder feeder = tray(3, 2);
        feeder.setFeedCount(1);
        feeder.setFeedOptions(FeedOptions.Disable);

        feeder.feed(null);
        feeder.feed(null);

        assertEquals(1, feeder.getFeedCount());
        assertEquals(FeedOptions.Disable, feeder.getFeedOptions(),
                "unlike SkipNext, this one is not cleared by a feed");
    }

    @Test
    public void aPartCanOnlyBeTakenBackAfterAFeed() {
        ReferenceTrayFeeder feeder = tray(3, 2);

        assertFalse(feeder.canTakeBackPart());

        feeder.setFeedCount(1);
        assertTrue(feeder.canTakeBackPart());
    }

    @Test
    public void trayFeedersSupportFeedOptions() {
        assertTrue(tray(1, 1).supportsFeedOptions());
    }
}
