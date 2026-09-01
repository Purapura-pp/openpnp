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
import org.openpnp.model.Configuration;
import org.openpnp.model.DisplayPreferences;
import org.openpnp.model.Volume;
import org.openpnp.model.VolumeUnit;

public class VolumeConverter extends Converter<Volume, String> {
    /**
     * Where the units come from. Left null by the constructor that predates it, and then resolved
     * to the shared configuration on first use rather than in the constructor, the same way
     * {@link LengthConverter} does it.
     */
    private final DisplayPreferences preferences;

    final String format;
    
    public VolumeConverter() {
        this(Configuration.get());
    }

    public VolumeConverter(DisplayPreferences preferences) {
        this(preferences, preferences.getLengthDisplayFormat());
    }

    public VolumeConverter(String format) {
        this(null, format);
    }

    public VolumeConverter(DisplayPreferences preferences, String format) {
        this.preferences = preferences;
        this.format = format;
    }

    private VolumeUnit units() {
        DisplayPreferences preferences =
                (this.preferences != null ? this.preferences : Configuration.get());
        return VolumeUnit.fromLengthUnit(preferences.getSystemUnits());
    }

    @Override
    public String convertForward(Volume volume) {
        volume = volume.convertToUnits(units());
        return String.format(Locale.US, format, volume.getValue());
    }

    @Override
    public Volume convertReverse(String s) {
        Volume volume = Volume.parse(s, false);
        if (volume == null) {
            throw new RuntimeException("Unable to parse " + s);
        }
        if (volume.getUnits() == null) {
            volume.setUnits(units());
        }
        return volume;
    }
}
