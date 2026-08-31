package org.openpnp.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The open job used to be held by the job tab, so model objects that needed it went through
 * MainFrame.get(). That returns a static field which is null when there is no GUI, and none of the
 * call sites checked, so using the model headless threw a NullPointerException rather than simply
 * finding no job.
 * <p>
 * These tests run with no GUI at all - MainFrame is never constructed - and assert that the model
 * answers sensibly instead of throwing. Without a job open, nothing belongs to one.
 */
public class HeadlessJobAccessTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        Configuration.initialize(tempDir.resolve(".openpnp").toFile());
        Configuration.get().load();
    }

    @Test
    public void noJobIsOpenToBeginWith() {
        assertNull(Configuration.get().getJob(),
                "a fresh configuration has no open job, and that is not an error");
    }

    @Test
    public void aBoardLocationIsNotADescendantOfAJobThatIsNotOpen() {
        BoardLocation boardLocation = new BoardLocation(new Board());

        assertFalse(boardLocation.isDescendantOfJob());
    }

    @Test
    public void disposingALocationWithoutAnOpenJobDoesNotThrow() {
        BoardLocation boardLocation = new BoardLocation(new Board());

        assertDoesNotThrow(() -> boardLocation.dispose());
    }

    @Test
    public void nothingIsInUseByAJobThatIsNotOpen() {
        Board board = new Board();

        assertFalse(Configuration.get().isInUse(board));
    }

    @Test
    public void addingAChildToAPanelWithoutAnOpenJobDoesNotThrow() {
        Panel definition = new Panel();
        Panel panel = new Panel(definition);
        BoardLocation child = new BoardLocation(new Board());
        child.setDefinition(child);

        assertDoesNotThrow(() -> panel.setChild(0, child));
    }

    @Test
    public void theOpenJobIsVisibleToTheModelOnceItIsSet() {
        Job job = new Job();

        Configuration.get().setJob(job);

        assertSame(job, Configuration.get().getJob());
        assertNotNull(job.getRootPanelLocation());
    }

    @Test
    public void aLocationUnderTheOpenJobIsRecognisedAsADescendant() {
        Job job = new Job();
        Configuration.get().setJob(job);
        BoardLocation boardLocation = new BoardLocation(new Board());
        boardLocation.setParent(job.getRootPanelLocation());

        assertTrue(boardLocation.isDescendantOfJob());
    }
}
