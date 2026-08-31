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
import org.openpnp.model.BottomVisionSettings.MaxRotation;
import org.openpnp.model.BottomVisionSettings.PartSizeCheckMethod;
import org.openpnp.model.BottomVisionSettings.PreRotateUsage;

/**
 * The three bottom vision enums used to be declared inside ReferenceBottomVision, which made the
 * model package depend on the reference machine implementation. Moving them is only safe because
 * the serializer writes an enum by its constant name and never mentions the declaring type, so an
 * existing machine.xml keeps loading. These tests hold that property down.
 * <p>
 * The written form is the on disk form, so a round trip through it is the same check as reading a
 * file produced before the move; there is no separate fixture for that.
 */
public class BottomVisionSettingsTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        Configuration.initialize(tempDir.resolve(".openpnp").toFile());
        Configuration.get().load();
    }

    private static BottomVisionSettings nonDefaultSettings() {
        BottomVisionSettings settings = new BottomVisionSettings("BVS-TEST");
        settings.setName("test");
        settings.setPreRotateUsage(PreRotateUsage.AlwaysOff);
        settings.setCheckPartSizeMethod(PartSizeCheckMethod.PadExtents);
        settings.setMaxRotation(MaxRotation.Full);
        return settings;
    }

    private static String serialize(BottomVisionSettings settings) throws Exception {
        StringWriter out = new StringWriter();
        Configuration.createSerializer().write(settings, out);
        return out.toString();
    }

    @Test
    public void enumsAreWrittenAsBareConstantNames() throws Exception {
        String xml = serialize(nonDefaultSettings());

        assertTrue(xml.contains("AlwaysOff"), xml);
        assertTrue(xml.contains("PadExtents"), xml);
        assertTrue(xml.contains("Full"), xml);
    }

    /**
     * The point of the previous test: since the declaring class never reaches the file, where the
     * enums live is a source level decision that old configurations cannot notice.
     */
    @Test
    public void serializedFormDoesNotNameTheDeclaringClass() throws Exception {
        String xml = serialize(nonDefaultSettings());

        assertFalse(xml.contains("ReferenceBottomVision"), xml);
        assertFalse(xml.contains("PreRotateUsage"), xml);
        assertFalse(xml.contains("org.openpnp"), xml);
    }

    @Test
    public void enumsSurviveARoundTrip() throws Exception {
        String xml = serialize(nonDefaultSettings());

        BottomVisionSettings read = Configuration.createSerializer()
                .read(BottomVisionSettings.class, new StringReader(xml));

        assertEquals(PreRotateUsage.AlwaysOff, read.getPreRotateUsage());
        assertEquals(PartSizeCheckMethod.PadExtents, read.getCheckPartSizeMethod());
        assertEquals(MaxRotation.Full, read.getMaxRotation());
    }

}
