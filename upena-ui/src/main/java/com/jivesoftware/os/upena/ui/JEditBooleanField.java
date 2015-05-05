/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.upena.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JToggleButton;

class JEditBooleanField implements JField<String> {

    public String name;
    public String field;
    public JToggleButton toggle;
    public JLabel viewer;

    public JEditBooleanField(String name, String field) {
        this.name = name;
        this.field = field;
    }

    @Override
    public boolean isFilterable() {
        return true;
    }

    @Override
    public JComponent getEditor(int w, final IPicked picked) {
        toggle = new JToggleButton((field.length() == 0) ? "null" : field);
        toggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        setValue(Boolean.toString(toggle.isSelected()));
                        if (picked != null) {
                            picked.picked(null, null);
                        }
                    }
                });

            }
        });
        return toggle;
    }

    @Override
    public JComponent getViewer(int w) {
        viewer = new JLabel(field);
        return viewer;
    }

    @Override
    public String getValue() {
        return field;
    }

    @Override
    public String getViewValue() {
        return field;
    }


    @Override
    public void setValue(String value) {
        field = value;
        if (toggle != null) {
            if (value.length() > 0) {
                toggle.setText(value);
                toggle.setSelected(Boolean.parseBoolean(value));
                toggle.revalidate();
            } else {
                toggle.setText("null");
                toggle.revalidate();
            }
        }
        if (viewer != null) {
            viewer.setText(value);
            viewer.revalidate();
        }
    }

    @Override
    public void clear() {
        setValue("");
    }

    @Override
    public JComponent getClear(final IPicked picked) {
        JButton b = new JButton(Util.icon("clear-left"));
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        clear();
                        if (picked != null) {
                            picked.picked(null, null);
                        }
                    }
                });

            }
        });
        return b;
    }

    @Override
    public String name() {
        return name;
    }
}