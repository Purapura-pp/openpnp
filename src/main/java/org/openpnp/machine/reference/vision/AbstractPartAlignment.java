package org.openpnp.machine.reference.vision;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.wizards.BottomVisionSettingsConfigurationWizard;
import org.openpnp.model.AbstractPartSettingsHolder;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Part;
import org.openpnp.model.PartSettingsHolder;
import org.openpnp.model.PartSettingsRoot;
import org.openpnp.spi.PartAlignment;
import org.openpnp.util.VisionUtils;
import org.pmw.tinylog.Logger;

public abstract class AbstractPartAlignment extends AbstractPartSettingsHolder implements PartSettingsRoot, PartAlignment {

    @Override 
    public PartSettingsHolder getParentHolder(PartSettingsHolder partSettingsHolder) {
        if (partSettingsHolder instanceof Part) {
            return ((Part) partSettingsHolder).getPackage();
        }
        else if (partSettingsHolder instanceof org.openpnp.model.Package) {
            return this;
        }
        else {
            return null;
        }
    }

    @Override 
    public BottomVisionSettings getInheritedVisionSettings(PartSettingsHolder partSettingsHolder) {
        while (partSettingsHolder != null) {
            BottomVisionSettings visionSettings = partSettingsHolder.getBottomVisionSettings();
            if (visionSettings != null) {
                return visionSettings;
            }
            partSettingsHolder = getParentHolder(partSettingsHolder);
        }
        return null;
    }

    @Override
    public BottomVisionSettings getVisionSettings(PartSettingsHolder partSettingsHolder) {
        return partSettingsHolder.getBottomVisionSettings();
    }

    public static BottomVisionSettings getInheritedVisionSettings(PartSettingsHolder partSettingsHolder, boolean allowDisabled) {
        AbstractPartAlignment partAlignment = getPartAlignment(partSettingsHolder, allowDisabled);
        if (partAlignment != null) {
            BottomVisionSettings visionSettings = partAlignment.getInheritedVisionSettings(partSettingsHolder);
            if (partAlignment.canHandle(partSettingsHolder, allowDisabled)) {
                return visionSettings;
            }
        }
        return null;
    }

    public static AbstractPartAlignment getPartAlignment(PartSettingsHolder partSettingsHolder, boolean allowDisabled) {
        // The enabled first, then disabled search lives on PartSettingsRoot, so that a settings
        // holder can reach its root without going through this class. ReferenceBottomVision is the
        // only PartAlignment there is, so narrowing after the search cannot pick differently.
        // TODO: if there are ever multiple Alignment<->VisionSettings classes, they would have to be matched up here.
        PartAlignment partAlignment = PartSettingsRoot.getBottomVisionRoot(allowDisabled);
        return (partAlignment instanceof AbstractPartAlignment
                ? (AbstractPartAlignment) partAlignment : null);
    }

    public static AbstractPartAlignment getPartAlignment(PartSettingsHolder partSettingsHolder) {
        return getPartAlignment(partSettingsHolder, false);
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }

    @Override
    public Wizard getPartConfigurationWizard(PartSettingsHolder partSettingsHolder) {
        BottomVisionSettings visionSettings = getInheritedVisionSettings(partSettingsHolder);
        if (visionSettings == null) {
            return null;
        }
        try {
            visionSettings.getPipeline().setProperty("camera", VisionUtils.getBottomVisionCamera());
        }
        catch (Exception e) {
            Logger.debug(e, "No bottom vision camera available to preset on the bottom vision pipeline.");
        }
        return new BottomVisionSettingsConfigurationWizard(visionSettings, partSettingsHolder);
    }
}
