package org.openpnp.gui.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openpnp.model.Abstract2DLocatable.Side;
import org.openpnp.model.Configuration;
import org.openpnp.model.Placement;

/**
 * Covers the Eagle .brd importer against the demo boards checked into the repository.
 * <p>
 * The exact placement count is deliberately not asserted, because it depends on which elements the
 * importer considers placeable. What is asserted are the invariants a caller relies on: identity,
 * geometry and part assignment.
 */
public class EagleBoardImporterTest {
    private static final File DEMO_BOARD = new File("samples", "Demo Board");

    @TempDir
    private Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        Configuration.initialize(tempDir.resolve(".openpnp").toFile());
        Configuration.get().load();
    }

    private static List<Placement> parse(String board) throws Exception {
        return EagleBoardImporter.parseFile(new File(DEMO_BOARD, board), Side.Top, true, false,
                false);
    }

    @Test
    public void demoBoardYieldsPlacements() throws Exception {
        List<Placement> placements = parse("Demo Board v2.brd");

        assertFalse(placements.isEmpty(), "the board defines 182 elements");
    }

    @Test
    public void everyPlacementIsUsable() throws Exception {
        List<Placement> placements = parse("Demo Board v2.brd");

        Set<String> ids = new HashSet<>();
        for (Placement placement : placements) {
            assertNotNull(placement.getId());
            assertFalse(placement.getId().isEmpty());
            assertTrue(ids.add(placement.getId()), "duplicate reference " + placement.getId());
            assertNotNull(placement.getLocation());
            assertNotNull(placement.getSide());
            assertNotNull(placement.getPart(),
                    "part creation was requested, so every placement should have one");
        }
    }

    @Test
    public void createdPartsAreRegisteredInTheConfiguration() throws Exception {
        List<Placement> placements = parse("Demo Board v2.brd");

        Placement first = placements.get(0);
        assertNotNull(Configuration.get().getPart(first.getPart().getId()),
                "parts the importer creates must be reachable from the configuration");
        assertNotNull(first.getPart().getPackage());
    }

    @Test
    public void bothDemoBoardRevisionsParse() throws Exception {
        List<Placement> v1 = parse("Demo Board v1.brd");
        List<Placement> v2 = parse("Demo Board v2.brd");

        assertFalse(v1.isEmpty());
        assertFalse(v2.isEmpty());
        assertTrue(v2.size() > v1.size(),
                "v2 defines 182 elements against 56 in v1, so it must yield more placements");
    }

    /**
     * The side argument is accepted for symmetry with the other importers but ignored: the layer
     * of each element in the .brd file decides the side. Pins that, since passing Bottom looks like
     * it should restrict or flip the result and does not.
     */
    @Test
    public void sideArgumentIsIgnoredInFavourOfTheBoardFile() throws Exception {
        List<Placement> asTop = EagleBoardImporter.parseFile(
                new File(DEMO_BOARD, "Demo Board v2.brd"), Side.Top, true, false, false);
        List<Placement> asBottom = EagleBoardImporter.parseFile(
                new File(DEMO_BOARD, "Demo Board v2.brd"), Side.Bottom, true, false, false);

        assertEquals(sidesOf(asTop), sidesOf(asBottom));
    }

    private static List<String> sidesOf(List<Placement> placements) {
        List<String> sides = new ArrayList<>();
        for (Placement placement : placements) {
            sides.add(placement.getId() + "=" + placement.getSide());
        }
        return sides;
    }

    @Test
    public void parsingIsRepeatable() throws Exception {
        List<Placement> first = parse("Demo Board v2.brd");
        List<Placement> second = parse("Demo Board v2.brd");

        assertEquals(first.size(), second.size(),
                "the second run reuses the parts created by the first and must still see every "
                        + "placement");
    }
}
