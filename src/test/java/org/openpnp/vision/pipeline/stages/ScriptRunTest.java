package org.openpnp.vision.pipeline.stages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openpnp.model.Configuration;

/**
 * Covers the containment rules of {@link ScriptRun#resolveScript()}. The stage runs unattended on
 * pipelines that are shared between users, so a script outside the scripts directory must never be
 * reached, no matter how the path spells it.
 */
public class ScriptRunTest {
    @TempDir
    Path configurationDirectory;

    private Path scriptsDirectory;

    @BeforeEach
    public void setUp() throws Exception {
        Configuration.initialize(configurationDirectory.toFile());
        scriptsDirectory = configurationDirectory.resolve("scripts");
        Files.createDirectories(scriptsDirectory);
    }

    private static ScriptRun stageFor(String path) {
        ScriptRun stage = new ScriptRun();
        stage.setFile(new File(path));
        return stage;
    }

    private static ScriptRun stageFor(Path path) {
        return stageFor(path.toString());
    }

    @Test
    public void absolutePathInsideScriptsDirectoryIsAccepted() throws Exception {
        Path script = Files.createFile(scriptsDirectory.resolve("vision.js"));

        File resolved = stageFor(script).resolveScript();

        assertEquals(script.toRealPath().toFile(), resolved);
    }

    @Test
    public void pathInNestedSubdirectoryIsAccepted() throws Exception {
        Path nested = Files.createDirectories(scriptsDirectory.resolve("mine/vision"));
        Path script = Files.createFile(nested.resolve("locate.js"));

        File resolved = stageFor(script).resolveScript();

        assertEquals(script.toRealPath().toFile(), resolved);
    }

    @Test
    public void relativePathIsResolvedAgainstScriptsDirectory() throws Exception {
        Path script = Files.createFile(scriptsDirectory.resolve("relative.js"));

        File resolved = stageFor("relative.js").resolveScript();

        assertEquals(script.toRealPath().toFile(), resolved);
    }

    @Test
    public void missingFileInsideScriptsDirectoryStillResolves() throws Exception {
        // Absence is reported by the caller, not here, so that a stale stage keeps behaving the
        // way it always has: silently skipped rather than failing the pipeline.
        Path absent = scriptsDirectory.resolve("absent.js");

        File resolved = stageFor(absent).resolveScript();

        assertEquals(absent.toFile(), resolved);
    }

    @Test
    public void absolutePathOutsideScriptsDirectoryIsRejected() throws Exception {
        Path outside = Files.createFile(configurationDirectory.resolve("outside.js"));

        Exception e = assertThrows(Exception.class, () -> stageFor(outside).resolveScript());
        assertTrue(e.getMessage().contains("only runs scripts inside"), e.getMessage());
    }

    @Test
    public void parentTraversalOutOfScriptsDirectoryIsRejected() throws Exception {
        Files.createFile(configurationDirectory.resolve("escaped.js"));

        Exception e = assertThrows(Exception.class,
                () -> stageFor("../escaped.js").resolveScript());
        assertTrue(e.getMessage().contains("only runs scripts inside"), e.getMessage());
    }

    @Test
    public void traversalThatComesBackInsideIsAccepted() throws Exception {
        Path script = Files.createFile(scriptsDirectory.resolve("back.js"));

        File resolved = stageFor("../scripts/back.js").resolveScript();

        assertEquals(script.toRealPath().toFile(), resolved);
    }

    @Test
    public void uncPathIsRejected() throws Exception {
        Assumptions.assumeTrue(File.separatorChar == '\\', "UNC paths are a Windows concept");

        Exception e = assertThrows(Exception.class,
                () -> stageFor("\\\\attacker\\share\\evil.js").resolveScript());
        assertTrue(e.getMessage().contains("only runs scripts inside"), e.getMessage());
    }

    @Test
    public void mappedNetworkDriveIsRejected() throws Exception {
        Assumptions.assumeTrue(File.separatorChar == '\\', "Drive letters are a Windows concept");

        // A drive letter reads as an ordinary local path, so it is only the containment check that
        // stands between a mapped share and unattended execution.
        Exception e = assertThrows(Exception.class,
                () -> stageFor("Z:\\evil.js").resolveScript());
        assertTrue(e.getMessage().contains("only runs scripts inside"), e.getMessage());
    }

    @Test
    public void linkInsideScriptsDirectoryLeadingOutIsRejected() throws Exception {
        Path target = Files.createFile(configurationDirectory.resolve("target.js"));
        Path link = scriptsDirectory.resolve("link.js");
        try {
            Files.createSymbolicLink(link, target);
        }
        catch (FileSystemException | UnsupportedOperationException e) {
            // Creating a link needs a privilege that Windows does not grant by default.
            Assumptions.assumeTrue(false, "Cannot create symbolic links here: " + e.getMessage());
        }

        Exception e = assertThrows(Exception.class, () -> stageFor(link).resolveScript());
        assertTrue(e.getMessage().contains("leads out of it"), e.getMessage());
    }

    @Test
    public void missingScriptsDirectoryIsReported() throws Exception {
        // The in-memory Configuration used by headless callers has no scripts directory at all.
        Configuration.initialize();

        Exception e = assertThrows(Exception.class,
                () -> stageFor("anything.js").resolveScript());
        assertTrue(e.getMessage().contains("requires a configured scripts directory"),
                e.getMessage());
    }
}
