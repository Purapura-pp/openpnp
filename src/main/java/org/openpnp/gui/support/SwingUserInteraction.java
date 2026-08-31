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

import javax.swing.JOptionPane;

import org.openpnp.gui.MainFrame;
import org.openpnp.model.UserInteraction;

/**
 * Answers the model's questions by putting a dialog in front of the user. MainFrame installs this
 * while starting up; before that, and in any run without a GUI, the non-interactive default
 * applies.
 */
public class SwingUserInteraction implements UserInteraction {
    @Override
    public boolean confirm(String title, String message) {
        // Cancel and no both mean the same thing to every caller: do not go ahead.
        return JOptionPane.showConfirmDialog(MainFrame.get(), message, title,
                JOptionPane.YES_NO_CANCEL_OPTION) == JOptionPane.YES_OPTION;
    }

    @Override
    public void reportError(String title, String message) {
        MessageBoxes.errorBox(MainFrame.get(), title, message);
    }
}
