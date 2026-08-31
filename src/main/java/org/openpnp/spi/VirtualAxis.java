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

package org.openpnp.spi;

/**
 * A VirtualAxis carries a coordinate that has no counterpart in the machine controller, such as
 * the Z of a camera that cannot be moved up or down. It is a CoordinateAxis, so it takes part in
 * transformations, but nothing is ever sent to a driver for it.
 * <p>
 * The distinction matters where a raw location is prepared for the controller: coordinates of
 * virtual axes carry no machine meaning and are substituted rather than commanded.
 */
public interface VirtualAxis extends CoordinateAxis {

}
