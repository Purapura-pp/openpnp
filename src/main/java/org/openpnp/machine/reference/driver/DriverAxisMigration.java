/*
 * Copyright (C) 2020 <mark@makr.zone>
 * inspired and based on work
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.driver;

import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.axis.ReferenceVirtualAxis;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.base.AbstractHeadMountable;

/**
 * Synthesizes the default axes and axis mapping for a driver, as needed when a configuration
 * predating the global axes implementation is loaded, or when a driver is created from scratch.
 * <p>
 * This lives in the reference machine layer rather than on the driver base class because it
 * instantiates concrete axis and nozzle implementations: a driver base class shared by all machine
 * implementations has no business deciding which axis class to create.
 */
public class DriverAxisMigration {

    private DriverAxisMigration() {
    }

    public static void createAxisMappingDefaults(Driver driver, ReferenceMachine machine)
            throws Exception {
        if (machine.getAxes().size() == 0) {
            // Create and map the standard axes to the HeadMountables. 
            ReferenceControllerAxis axisX = migrateAxis(driver, machine, Axis.Type.X, "");
            ReferenceControllerAxis axisY = migrateAxis(driver, machine, Axis.Type.Y, "");

            for (Camera hm : machine.getDefaultHead().getCameras()) {
                ((AbstractHeadMountable)hm).setAxisX(axisX);
                ((AbstractHeadMountable)hm).setAxisY(axisY);
                assignCameraVirtualAxes(machine, hm);
            }
            for (Nozzle hm : machine.getDefaultHead().getNozzles()) {
                // Note, we create dedicated axes per Nozzle, assuming this is a test driver that does not
                // care about shared/dedicated axes or a single nozzle test GcodeDriver.  
                ReferenceControllerAxis axisZ = migrateAxis(driver, machine, Axis.Type.Z, hm.getName());
                ReferenceControllerAxis axisRotation = migrateAxis(driver, machine, Axis.Type.Rotation, hm.getName());
                if (hm instanceof ReferenceNozzle) {
                    axisRotation.setLimitRotation(((ReferenceNozzle) hm).isLimitRotation());
                }
                ((AbstractHeadMountable)hm).setAxisX(axisX);
                ((AbstractHeadMountable)hm).setAxisY(axisY);
                ((AbstractHeadMountable)hm).setAxisZ(axisZ);
                ((AbstractHeadMountable)hm).setAxisRotation(axisRotation);
                if (hm instanceof ReferenceNozzle) {
                    ((ReferenceNozzle)hm).migrateSafeZ();
                }
            }
            // No movable actuators mapped for these test drivers.
        }
    }

    public static void assignCameraVirtualAxes(ReferenceMachine machine, Camera hm)
            throws Exception {
        // Assign virtual axes to cameras.
        if (hm.getAxisZ() == null) {
            ReferenceVirtualAxis axisZ = new ReferenceVirtualAxis();
            axisZ.setType(Type.Z);
            axisZ.setName("z"+hm.getName());
            machine.addAxis(axisZ);
            ((AbstractHeadMountable)hm).setAxisZ(axisZ);
        }
        if (hm.getAxisRotation() == null) {
            ReferenceVirtualAxis axisRotation = new ReferenceVirtualAxis();
            axisRotation.setType(Type.Rotation);
            axisRotation.setName("rotation"+hm.getName());
            machine.addAxis(axisRotation);
            ((AbstractHeadMountable)hm).setAxisRotation(axisRotation);
        }
    }

    public static ReferenceControllerAxis migrateAxis(Driver driver, ReferenceMachine machine,
            Axis.Type type, String suffix) throws Exception {
        ReferenceControllerAxis axis;
        axis = new ReferenceControllerAxis();
        axis.setType(type);
        axis.setName(type.toString().toLowerCase()+suffix);
        axis.setDriver(driver);
        machine.addAxis(axis);
        return axis;
    }
}
