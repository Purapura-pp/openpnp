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

package org.openpnp.gui.support;

import java.util.Locale;

import org.jdesktop.beansbinding.Converter;
import org.openpnp.model.Configuration;
import org.openpnp.model.DisplayPreferences;
import org.openpnp.model.Length;

public class LengthConverter extends Converter<Length, String> {
    /**
     * Where the units come from. Left null by the constructors that predate it, and then resolved
     * to the shared configuration on first use rather than in the constructor: a wizard holds its
     * converter in a field that is built before there is a configuration to ask.
     */
    private final DisplayPreferences preferences;

    final String format;

    public LengthConverter() {
        this(Configuration.get());
    }

    public LengthConverter(DisplayPreferences preferences) {
        this(preferences, preferences.getLengthDisplayFormat());
    }

    public LengthConverter(String format) {
        this(null, format);
    }

    public LengthConverter(DisplayPreferences preferences, String format) {
        this.preferences = preferences;
        this.format = format;
    }

    private DisplayPreferences preferences() {
        return (preferences != null ? preferences : Configuration.get());
    }

    @Override
    public String convertForward(Length length) {
        length = length.convertToUnits(preferences().getSystemUnits());
        return String.format(Locale.US, format, length.getValue());
    }

    @Override
    public Length convertReverse(String s) {
        Length length = Length.parseWithDefaultUnits(s, preferences().getSystemUnits());
        if (length == null) {
            throw new RuntimeException("Unable to parse " + s);
        }
        return length;
    }
}
