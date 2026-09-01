/*
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

package org.openpnp.spi.base;

import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;

/**
 * A part of a machine that knows which machine it is part of.
 * <p>
 * An axis, a feeder, a driver, a signaler, a nozzle tip and a head mountable all need the machine
 * they hang off: to reach the motion planner, to find their siblings, or to remove themselves when
 * the user says so. None of them used to hold a reference to it, so each one asked
 * {@link Configuration} for the machine of the moment instead, which is a different question with
 * the same answer only as long as there is exactly one machine in the process.
 * <p>
 * {@link AbstractMachine} fills the reference in, from its {@code @Commit} method after a load and
 * from {@code addAxis}, {@code addFeeder} and their siblings at runtime. That is the same mechanism
 * heads have always used, and it runs before any {@code configurationLoaded} callback, so an
 * element that is part of a machine has this available from the moment the machine does.
 */
public abstract class AbstractMachineElement extends AbstractModelObject implements MachineElement {
    /**
     * Deliberately not serialized. The machine writes it back on every load, and an element that
     * named its machine in the XML would put a cycle in a file the user has to be able to read.
     */
    private AbstractMachine machine;

    /**
     * The machine this element is part of, or the one that exists if it has not been attached to a
     * machine yet.
     * 
     * @return
     */
    @Override
    public AbstractMachine getMachine() {
        return MachineElement.machineOf(this, machine);
    }

    @Override
    public void setMachine(AbstractMachine machine) {
        this.machine = machine;
    }
}
