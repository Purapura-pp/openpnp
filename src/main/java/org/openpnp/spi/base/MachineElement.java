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

import org.openpnp.model.Configuration;
import org.pmw.tinylog.Logger;

/**
 * Something the machine hands a reference to itself while it is being loaded.
 * <p>
 * Most of these are {@link AbstractMachineElement}s, which is where the reference is kept. The job
 * processor and the fiducial locator hang off the machine in machine.xml exactly as an axis or a
 * feeder does, but their class hierarchies are elsewhere, so they keep their own. What
 * {@link AbstractMachine#attach} looks for is this interface: belonging to a machine is a matter of
 * being able to hold the reference, not of descending from one particular class.
 */
public interface MachineElement {
    AbstractMachine getMachine();

    void setMachine(AbstractMachine machine);

    /**
     * The machine an element holding {@code attached} should answer with.
     * <p>
     * Between the constructor and the call that attaches it - the window in which a wizard has made
     * something the user has not confirmed yet - there is no machine to name, and the singleton is
     * asked, as everything did before. Keeping that fallback in one place means an element used
     * early behaves as it did, and the remaining reach for global state stays where it can be seen.
     * 
     * @param element
     * @param attached
     * @return
     */
    static AbstractMachine machineOf(MachineElement element, AbstractMachine attached) {
        if (attached == null) {
            Logger.trace("{} was asked for its machine before it was attached to one.",
                    element.getClass().getSimpleName());
            return (AbstractMachine) Configuration.get().getMachine();
        }
        return attached;
    }
}
