import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraPanel;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.Configuration;

import sun.misc.Unsafe;

/**
 * What the Issues and Solutions probes both have to do before they can ask a configuration
 * anything.
 * <p>
 * These are not part of the build. They read wording off a machine that has actually been loaded,
 * which is the only way to see the sentences the sources assemble at runtime, and they reach into
 * private state to get there. See probes/README section in ../README.md.
 */
final class Probes {
    private Probes() {
    }

    /**
     * Load a configuration built around the given machine.xml, headless.
     * <p>
     * The file is copied into a fresh temporary directory every run, because
     * {@link Configuration#load} writes back the defaults it had to invent: pointing this at a
     * real configuration directory would edit it.
     *
     * @param machineXml the machine.xml to load. A vision-settings.xml beside it is taken too.
     * @param name       a short name for the temporary directory, to tell runs apart
     * @return the loaded machine
     */
    static ReferenceMachine load(Path machineXml, String name) throws Exception {
        conjureMainFrame();

        File dir = new File(System.getProperty("java.io.tmpdir"),
                name + "-" + System.nanoTime() + File.separator + ".openpnp");
        dir.mkdirs();
        Files.copy(machineXml, dir.toPath().resolve("machine.xml"));
        Path visionSettings = machineXml.resolveSibling("vision-settings.xml");
        if (Files.exists(visionSettings)) {
            Files.copy(visionSettings, dir.toPath().resolve("vision-settings.xml"));
        }

        Configuration.initialize(dir);
        Configuration.get().load();
        return (ReferenceMachine) Configuration.get().getMachine();
    }

    /**
     * CameraSolutions asks MainFrame for the camera views while it is looking for issues, so a
     * shell of one has to exist.
     * <p>
     * Allocated without running its constructor: building the real window needs a display, and all
     * that is wanted here is for getCameraView() to answer null. Unsafe is the only way to get an
     * instance of a class whose constructor cannot run in this environment - if this ever stops
     * working, the alternative is to run the probe under a virtual display rather than to make
     * MainFrame constructible.
     */
    private static void conjureMainFrame() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);

        CameraPanel cameraPanel = (CameraPanel) unsafe.allocateInstance(CameraPanel.class);
        Field views = CameraPanel.class.getDeclaredField("cameraViews");
        views.setAccessible(true);
        views.set(cameraPanel, new HashMap<>());

        MainFrame frame = (MainFrame) unsafe.allocateInstance(MainFrame.class);
        Field panel = MainFrame.class.getDeclaredField("cameraPanel");
        panel.setAccessible(true);
        panel.set(frame, cameraPanel);

        Field instance = MainFrame.class.getDeclaredField("mainFrame");
        instance.setAccessible(true);
        instance.set(null, frame);
    }
}
