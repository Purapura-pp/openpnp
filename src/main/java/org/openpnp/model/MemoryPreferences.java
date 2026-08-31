/*
 * Copyright (C) 2026 Jason von Nieda <jason@vonnieda.org>
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.AbstractPreferences;

/**
 * Preferences that live for as long as the process and are backed by nothing.
 * <p>
 * The in memory {@link Configuration} exists so that a caller can have a machine without touching
 * anything on disk, but its preferences still went to the real user node. Running the tests
 * therefore rewrote the display units, the theme and the window geometry of whatever OpenPnP is
 * installed on the same account, and left them rewritten if a test failed before it could put them
 * back. Preferences read here also carried over between tests, which is not the isolation a test
 * expects from a fresh Configuration.
 */
class MemoryPreferences extends AbstractPreferences {
    private final Map<String, String> values = new ConcurrentHashMap<>();
    private final Map<String, MemoryPreferences> children = new ConcurrentHashMap<>();

    MemoryPreferences() {
        super(null, "");
    }

    private MemoryPreferences(MemoryPreferences parent, String name) {
        super(parent, name);
    }

    @Override
    protected void putSpi(String key, String value) {
        values.put(key, value);
    }

    @Override
    protected String getSpi(String key) {
        return values.get(key);
    }

    @Override
    protected void removeSpi(String key) {
        values.remove(key);
    }

    @Override
    protected void removeNodeSpi() {
        values.clear();
        children.clear();
    }

    @Override
    protected String[] keysSpi() {
        return values.keySet().toArray(new String[0]);
    }

    @Override
    protected String[] childrenNamesSpi() {
        return children.keySet().toArray(new String[0]);
    }

    @Override
    protected AbstractPreferences childSpi(String name) {
        return children.computeIfAbsent(name, child -> new MemoryPreferences(this, child));
    }

    @Override
    protected void syncSpi() {
        // There is no backing store to reconcile with.
    }

    @Override
    protected void flushSpi() {
        // There is no backing store to write to.
    }
}
