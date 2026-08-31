package org.openpnp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * AbstractPartSettingsHolder, which Part and Package extend, used to live in
 * machine.reference.vision - that inheritance was the model's largest remaining dependency on the
 * reference machine. It only moved because the two attributes it contributes are serialized by
 * field name, so where the class is declared cannot reach the file.
 * <p>
 * No sample configuration carries these attributes, so nothing else would catch a regression here.
 */
public class PartSettingsHolderSerializationTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        Configuration.initialize(tempDir.resolve(".openpnp").toFile());
        Configuration.get().load();
    }

    private static String serialize(Object object) throws Exception {
        StringWriter out = new StringWriter();
        Configuration.createSerializer().write(object, out);
        return out.toString();
    }

    private static BottomVisionSettings bottomVisionSettings() {
        BottomVisionSettings settings = new BottomVisionSettings("BVS-TEST");
        settings.setName("test bottom vision");
        Configuration.get().addVisionSettings(settings);
        return settings;
    }

    private static FiducialVisionSettings fiducialVisionSettings() {
        FiducialVisionSettings settings = new FiducialVisionSettings("FVS-TEST");
        settings.setName("test fiducial vision");
        Configuration.get().addVisionSettings(settings);
        return settings;
    }

    /** A Part will not serialize without a package, that attribute being required. */
    private static Part aPart() {
        Part part = new Part("R0805");
        part.setPackage(new Package("0805"));
        return part;
    }

    @Test
    public void aPartRecordsItsVisionSettingsByIdAndNotByDeclaringClass() throws Exception {
        Part part = aPart();
        part.setBottomVisionSettings(bottomVisionSettings());
        part.setFiducialVisionSettings(fiducialVisionSettings());

        String xml = serialize(part);

        assertTrue(xml.contains("BVS-TEST"), xml);
        assertTrue(xml.contains("FVS-TEST"), xml);
        assertFalse(xml.contains("machine"), xml);
        assertFalse(xml.contains("AbstractPartSettingsHolder"), xml);
    }

    @Test
    public void aPackageRecordsItsVisionSettingsTheSameWay() throws Exception {
        Package pkg = new Package("0805");
        pkg.setBottomVisionSettings(bottomVisionSettings());

        String xml = serialize(pkg);

        assertTrue(xml.contains("BVS-TEST"), xml);
        assertFalse(xml.contains("AbstractPartSettingsHolder"), xml);
    }

    /**
     * The ids are read back into the inherited fields. Resolving them to objects happens later, on
     * configuration load, so this asserts the fields rather than writing the holder out again -
     * doing that would run persist() against an unresolved holder and clear them, which is how the
     * class has always behaved.
     */
    @Test
    public void theInheritedAttributesAreReadBackIntoTheInheritedFields() throws Exception {
        Package pkg = new Package("0805");
        pkg.setBottomVisionSettings(bottomVisionSettings());
        pkg.setFiducialVisionSettings(fiducialVisionSettings());

        String xml = serialize(pkg);
        Package read = Configuration.createSerializer().read(Package.class, new StringReader(xml));

        assertEquals("0805", read.getId());
        assertEquals("BVS-TEST", read.bottomVisionId);
        assertEquals("FVS-TEST", read.fiducialVisionId);
    }

    @Test
    public void aHolderWithNoVisionSettingsOmitsTheAttributes() throws Exception {
        String xml = serialize(aPart());

        assertFalse(xml.contains("BVS-"), xml);
        assertFalse(xml.contains("FVS-"), xml);
    }
}
