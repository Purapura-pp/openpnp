package org.openpnp.model;

import org.openpnp.spi.FiducialLocator;
import org.openpnp.spi.Machine;
import org.openpnp.spi.PartAlignment;

/**
 * Root part settings holder, i.e. the Vision configuration objects in the Machine Setup. 
 *
 */
public interface PartSettingsRoot extends PartSettingsHolder {
    public abstract PartSettingsHolder getParentHolder(PartSettingsHolder partSettingsHolder);
    public abstract AbstractVisionSettings getVisionSettings(PartSettingsHolder partSettingsHolder);
    public abstract AbstractVisionSettings getInheritedVisionSettings(PartSettingsHolder partSettingsHolder);

    /**
     * Finds the machine's bottom vision root. Kept here rather than on the reference
     * implementation, because a settings holder needs to reach its root without knowing which
     * machine implementation is installed.
     * 
     * @param allowDisabled Whether a disabled alignment may be returned when there is no enabled
     * one. An enabled one always wins.
     * @return The root, or null if the machine has no part alignment at all.
     */
    static PartAlignment getBottomVisionRoot(boolean allowDisabled) {
        Machine machine = (Configuration.isInstanceInitialized()
                ? Configuration.get().getMachine() : null);
        if (machine == null) {
            return null;
        }
        for (boolean acceptDisabled : (allowDisabled ? new boolean[] {false, true}
                : new boolean[] {false})) {
            for (PartAlignment partAlignment : machine.getPartAlignments()) {
                if (partAlignment.isEnabled() || acceptDisabled) {
                    return partAlignment;
                }
            }
        }
        return null;
    }

    /**
     * @return The machine's fiducial vision root, or null if there is no machine yet.
     */
    static FiducialLocator getFiducialVisionRoot() {
        Machine machine = (Configuration.isInstanceInitialized()
                ? Configuration.get().getMachine() : null);
        return machine == null ? null : machine.getFiducialLocator();
    }
}
