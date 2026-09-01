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

package org.openpnp.model;

/**
 * How lengths are shown to the user: the units they are converted into and the formats they are
 * rendered with.
 * <p>
 * These are the only things most of the interface wants from {@link Configuration}. A converter or
 * a table model that takes this instead reaches a handful of preferences rather than the machine,
 * the part list, the open job and the ability to write the configuration to disk, and it can be
 * handed fixed values by a test rather than having to set the user's real preferences and put them
 * back afterwards.
 * <p>
 * Read only on purpose. Changing a preference is the settings panel's job, and a consumer that
 * could change one would be changing it for the whole application.
 */
public interface DisplayPreferences {
    /**
     * The units lengths are converted into for display, and assumed to be in when the user types a
     * number with no unit.
     * 
     * @return
     */
    LengthUnit getSystemUnits();

    String getLengthDisplayFormat();

    String getLengthDisplayAlignedFormat();

    String getLengthDisplayFormatWithUnits();

    String getLengthDisplayAlignedFormatWithUnits();

    /**
     * Renders a length the way the interface shows it: converted to the system units and formatted
     * with the configured display format.
     * 
     * @param length
     * @return
     */
    String formatLength(Length length);

    int getVerticalScrollUnitIncrement();
}
