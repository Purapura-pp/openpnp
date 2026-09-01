package org.openpnp.util;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openpnp.Translations;
import org.openpnp.gui.support.SvgIcon;
import org.pmw.tinylog.Logger;

public class IconViewer extends JFrame {
    private JTextField textField;
    private final Action action = new SwingAction();
    private JPanel iconsPanel;
    private JCheckBox chckbxGray;
    private List<JLabel> labels = new ArrayList<JLabel>();

    public IconViewer() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout(0, 0));
        
        JPanel panel = new JPanel();
        getContentPane().add(panel, BorderLayout.NORTH);
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        
        JLabel lblDirectory = new JLabel(Translations.getString("IconViewer.lblDirectory.text")); //$NON-NLS-1$
        panel.add(lblDirectory);
        
        textField = new JTextField();
        textField.setText("/Users/jason/Projects/openpnp/openpnp/src/main/resources/icons");
        panel.add(textField);
        textField.setColumns(30);
        
        JButton btnRefresh = new JButton(Translations.getString("IconViewer.btnRefresh.text")); //$NON-NLS-1$
        panel.add(btnRefresh);
        
        chckbxGray = new JCheckBox(Translations.getString("IconViewer.chckbxGray.text")); //$NON-NLS-1$
        chckbxGray.setAction(action);
        panel.add(chckbxGray);
        
        iconsPanel = new JPanel();
        getContentPane().add(iconsPanel, BorderLayout.CENTER);
        
        btnRefresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    File[] files = new File(textField.getText()).listFiles();
                    iconsPanel.removeAll();
                    labels.clear();
                    for (File file : files) {
                        if (file.getName().endsWith(".svg")) {
                            try {
                                SvgIcon icon = new SvgIcon(file.toURL(), 24, 24);
                                JLabel label = new JLabel(icon);
                                label.setDisabledIcon(icon);
                                label.setEnabled(!chckbxGray.isSelected());
                                iconsPanel.add(label);
                                labels.add(label);
                            }
                            catch (Exception e1) {
                                Logger.debug(e1, "Could not load the icon {}, it is skipped.", file);
                            }
                        }
                    }
                    iconsPanel.revalidate();
                    iconsPanel.repaint();
                }
                catch (Exception e1) {
                    Logger.error(e1, "Could not list the icons in {}.", textField.getText());
                }
            }
        });
    }
    
    public static void main(String[] args) throws Exception {
        IconViewer v = new IconViewer();
        v.setSize(800, 600);
        v.setVisible(true);
    }
    
    private class SwingAction extends AbstractAction {
        public SwingAction() {
            putValue(NAME, "Gray?");
            putValue(SHORT_DESCRIPTION, "Some short description");
        }
        
        public void actionPerformed(ActionEvent e) {
            for (JLabel label : labels) {
                label.setEnabled(!chckbxGray.isSelected());
            }
        }
    }
}
