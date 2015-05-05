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

import com.jivesoftware.os.amza.shared.RingHost;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

public class JAmza extends JPanel {

    RequestHelperProvider requestHelperProvider;
    JPanel viewResults;
    JTextField hostName;
    JTextField port;

    public JAmza(RequestHelperProvider requestHelperProvider) {
        this.requestHelperProvider = requestHelperProvider;
        initComponents();
    }

    private void initComponents() {

        viewResults = new JPanel(new SpringLayout());
        viewResults.setPreferredSize(new Dimension(800, 400));

        JPanel m = new JPanel(new SpringLayout());

        m.add(new JLabel("host-name"));
        m.add(new JLabel("port"));

        hostName = new JTextField("", 120);
        hostName.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //
            }
        });
        hostName.setMaximumSize(new Dimension(120, 24));
        m.add(hostName);

        port = new JTextField("", 120);
        port.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //
            }
        });
        port.setMaximumSize(new Dimension(120, 24));
        m.add(port);

        SpringUtils.makeCompactGrid(m, 2, 2, 24, 24, 16, 6);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));

        JButton tableNames = new JButton("List Tables");
        tableNames.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                java.awt.EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        tables();
                    }
                });
            }
        });
        buttons.add(tableNames);

        JButton list = new JButton("List Ring");
        list.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                java.awt.EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ring();
                    }
                });
            }
        });
        buttons.add(list);

        JButton add = new JButton("Add Amza Host");
        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        addRingHost();
                    }
                });
            }
        });
        buttons.add(add);

        JButton remove = new JButton("Remove Amza Remove");
        remove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Util.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        removeRingHost();
                    }
                });
            }
        });
        buttons.add(remove);

        JPanel routes = new JPanel();
        routes.setLayout(new BoxLayout(routes, BoxLayout.Y_AXIS));

        routes.add(m);

        routes.add(buttons);

        JScrollPane scrollRoutes = new JScrollPane(viewResults,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        routes.add(scrollRoutes);
        routes.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        setLayout(new BorderLayout(8, 8));
        add(routes, BorderLayout.CENTER);

        setPreferredSize(new Dimension(600, 400));

        Util.invokeLater(new Runnable() {
            @Override
            public void run() {
                ring();
            }
        });
    }

    public void tables() {

        List tableNames = requestHelperProvider.get().executeRequest("",
                "/amza/tables", List.class, null);
        System.out.println(tableNames);
        if (tableNames != null) {
            viewResults.removeAll();
            int count = 0;
            for (Object h : tableNames) {
                viewResults.add(new JLabel("Table:" + h));
                count++;
            }
            SpringUtils.makeCompactGrid(viewResults, count, 1, 0, 0, 0, 0);
            viewResults.revalidate();
            viewResults.repaint();
        } else {
            viewResults.removeAll();
            viewResults.add(new JLabel("No results"));
            viewResults.revalidate();
        }
        viewResults.getParent().revalidate();
        viewResults.getParent().repaint();
    }

    public void ring() {

        List ring = requestHelperProvider.get().executeRequest("",
                "/amza/ring", List.class, null);
        System.out.println(ring);
        if (ring != null) {
            viewResults.removeAll();
            int count = 0;
            for (Object h : ring) {
                viewResults.add(new JLabel("Amza host:" + h));
                count++;
            }
            SpringUtils.makeCompactGrid(viewResults, count, 1, 0, 0, 0, 0);
            viewResults.revalidate();
            viewResults.repaint();
        } else {
            viewResults.removeAll();
            viewResults.add(new JLabel("No results"));
            viewResults.revalidate();
        }
        viewResults.getParent().revalidate();
        viewResults.getParent().repaint();
    }

    public void addRingHost() {

        RingHost host = new RingHost(hostName.getText(), Integer.parseInt(port.getText()));
        Boolean response = requestHelperProvider.get().executeRequest(host,
                "/amza/ring/add", Boolean.class, null);

        if (response != null) {
            viewResults.removeAll();
            ring();
        } else {
            viewResults.removeAll();
            viewResults.add(new JLabel("No results"));
            viewResults.revalidate();
        }
        viewResults.getParent().revalidate();
        viewResults.getParent().repaint();
    }

    public void removeRingHost() {

        RingHost host = new RingHost(hostName.getText(), Integer.parseInt(port.getText()));
        Boolean response = requestHelperProvider.get().executeRequest(host,
                "/amza/ring/remove", Boolean.class, null);

        if (response != null) {
            viewResults.removeAll();
            ring();
        } else {
            viewResults.removeAll();
            viewResults.add(new JLabel("No results"));
            viewResults.revalidate();
        }
        viewResults.getParent().revalidate();
        viewResults.getParent().repaint();
    }
}