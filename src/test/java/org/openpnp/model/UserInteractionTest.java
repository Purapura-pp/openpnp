package org.openpnp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The model used to put its questions to the user through JOptionPane directly, which tied it to
 * a running GUI. These tests cover the seam that replaced it: what a run with nobody watching
 * does, and that the GUI can take over.
 */
public class UserInteractionTest {
    @TempDir
    Path tempDir;

    /** Records what it was asked so a test can check the question actually reached it. */
    private static class RecordingUserInteraction implements UserInteraction {
        final List<String> confirmations = new ArrayList<>();
        final List<String> errors = new ArrayList<>();
        boolean answer;

        @Override
        public boolean confirm(String title, String message) {
            confirmations.add(title);
            return answer;
        }

        @Override
        public void reportError(String title, String message) {
            errors.add(title + ": " + message);
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        Configuration.initialize(tempDir.resolve(".openpnp").toFile());
        Configuration.get().load();
    }

    @Test
    public void aRunWithoutAGuiGetsTheNonInteractiveImplementation() {
        assertNotNull(Configuration.get().getUserInteraction());
    }

    @Test
    public void theNonInteractiveImplementationDeclinesRatherThanAnsweringForTheUser() {
        UserInteraction nonInteractive = UserInteraction.nonInteractive();

        assertFalse(nonInteractive.confirm("Save foo.board.xml?", "Do you want to save?"),
                "declining is the branch that leaves the file alone");
    }

    @Test
    public void reportingAnErrorWithNobodyWatchingDoesNotThrow() {
        UserInteraction.nonInteractive().reportError("Save Error", "disk full");
    }

    @Test
    public void theInstalledImplementationIsTheOneThatGetsAsked() {
        RecordingUserInteraction recording = new RecordingUserInteraction();
        Configuration.get().setUserInteraction(recording);

        assertSame(recording, Configuration.get().getUserInteraction());

        Configuration.get().getUserInteraction().reportError("Save Error", "disk full");
        assertEquals(List.of("Save Error: disk full"), recording.errors);
    }

    @Test
    public void clearingTheImplementationFallsBackRatherThanLeavingANull() {
        Configuration.get().setUserInteraction(new RecordingUserInteraction());

        Configuration.get().setUserInteraction(null);

        assertNotNull(Configuration.get().getUserInteraction(),
                "a null would turn every later question into a NullPointerException");
        assertFalse(Configuration.get().getUserInteraction().confirm("t", "m"));
    }
}
