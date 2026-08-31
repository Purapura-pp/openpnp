package org.openpnp.model;

import org.openpnp.gui.support.Wizard;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class FiducialVisionSettings extends AbstractVisionSettings {

    @Element(required = false)
    private Length parallaxDiameter = new Length(0, LengthUnit.Millimeters);

    @Attribute(required = false)
    private double parallaxAngle = 0;

    @Attribute(required = false)
    protected int maxVisionPasses = 3;

    @Element(required = false)
    protected Length maxLinearOffset = new Length(0.2, LengthUnit.Millimeters);

    /**
     * @return null. Which wizard configures these settings is a decision for whoever is showing
     * them; see VisionSettingsPanel. A settings object naming a wizard from the reference machine
     * is what used to make the model depend on it.
     */
    @Override
    public Wizard getConfigurationWizard() {
        return null;
    }

    public FiducialVisionSettings() {
        super(Configuration.createId("FVS"));
    }

    public FiducialVisionSettings(String id) {
        super(id);
    }

    public Length getParallaxDiameter() {
        return parallaxDiameter;
    }

    public void setParallaxDiameter(Length parallaxDiameter) {
        Object oldValue = this.parallaxDiameter;
        this.parallaxDiameter = parallaxDiameter;
        firePropertyChange("parallaxDiameter", oldValue, parallaxDiameter);
    }

    public double getParallaxAngle() {
        return parallaxAngle;
    }

    public void setParallaxAngle(double parallaxAngle) {
        Object oldValue = this.parallaxAngle;
        this.parallaxAngle = parallaxAngle;
        firePropertyChange("parallaxAngle", oldValue, parallaxAngle);
    }

    public int getMaxVisionPasses() {
        return maxVisionPasses;
    }

    public void setMaxVisionPasses(int maxVisionPasses) {
        Object oldValue = this.maxVisionPasses;
        this.maxVisionPasses = maxVisionPasses;
        firePropertyChange("maxVisionPasses", oldValue, maxVisionPasses);
    }

    public Length getMaxLinearOffset() {
        return maxLinearOffset;
    }

    public void setMaxLinearOffset(Length maxLinearOffset) {
        Object oldValue = this.maxLinearOffset;
        this.maxLinearOffset = maxLinearOffset;
        firePropertyChange("maxLinearOffset", oldValue, maxLinearOffset);
    }

    public void setValues(FiducialVisionSettings another) {
        setEnabled(another.isEnabled());
        setMaxVisionPasses(another.getMaxVisionPasses());
        setMaxLinearOffset(another.getMaxLinearOffset());
        setParallaxDiameter(another.getParallaxDiameter());
        setParallaxAngle(another.getParallaxAngle());
        try {
            setPipeline(another.getPipeline().clone());
        }
        catch (CloneNotSupportedException e) {
            Logger.error(e, "Could not copy the pipeline from vision settings {}, the current pipeline was kept.", another.getName());
        }
        Configuration.get().fireVisionSettingsChanged();
    }

    @Override
    public void resetToDefault() {
        FiducialVisionSettings stockVisionSettings = (FiducialVisionSettings) Configuration.get()
                .getVisionSettings(AbstractVisionSettings.STOCK_FIDUCIAL_ID);
        setValues(stockVisionSettings);
    }
}
