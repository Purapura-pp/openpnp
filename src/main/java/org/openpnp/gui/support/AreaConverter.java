/*
 * Copyright (C) 2021 Tony Luken <tonyluken@att.net>
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

package org.openpnp.gui.support;

import java.util.Locale;

import org.jdesktop.beansbinding.Converter;
import org.openpnp.model.Area;
import org.openpnp.model.AreaUnit;
import org.openpnp.model.Configuration;
import org.openpnp.model.DisplayPreferences;

public class AreaConverter extends Converter<Area, String> {
    /**
     * Where the units come from. Left null by the constructor that predates it, and then resolved
     * to the shared configuration on first use rather than in the constructor, the same way
     * {@link LengthConverter} does it.
     */
    private final DisplayPreferences preferences;

    final String format;
    
    public AreaConverter() {
        this(Configuration.get());
    }

    public AreaConverter(DisplayPreferences preferences) {
        this(preferences, preferences.getLengthDisplayFormat());
    }

    public AreaConverter(String format) {
        this(null, format);
    }

    public AreaConverter(DisplayPreferences preferences, String format) {
        this.preferences = preferences;
        this.format = format;
    }

    private AreaUnit units() {
        DisplayPreferences preferences =
                (this.preferences != null ? this.preferences : Configuration.get());
        return AreaUnit.fromLengthUnit(preferences.getSystemUnits());
    }

    @Override
    public String convertForward(Area area) {
        area = area.convertToUnits(units());
        return String.format(Locale.US, format, area.getValue());
    }

    @Override
    public Area convertReverse(String s) {
        Area area = Area.parse(s, false);
        if (area == null) {
            throw new RuntimeException("Unable to parse " + s);
        }
        if (area.getUnits() == null) {
            area.setUnits(units());
        }
        return area;
    }
}
