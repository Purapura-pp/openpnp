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

package org.openpnp.util;

import java.awt.Color;

import org.simpleframework.xml.transform.Transform;

/**
 * Reads and writes a Color as #aarrggbb, so that simple-xml can persist one. Register it on a
 * Persister with a RegistryMatcher; simple-xml has no built in transform for Color.
 */
public class ColorTransform implements Transform<Color> {

    @Override
    public Color read(String value) throws Exception {
        // Long.decode rather than Integer, since an alpha of 0x80 or above overflows a signed int.
        return new Color(Long.decode(value).intValue(), true);
    }

    @Override
    public String write(Color value) throws Exception {
        return String.format("#%08x", value.getRGB());
    }
}
