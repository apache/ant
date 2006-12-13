/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs.optional.splash;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;

class SplashScreen extends JWindow implements ActionListener, BuildListener {

    private JLabel text;
    private JProgressBar pb;
    private int total;
    private static final int MIN = 0;
    private static final int MAX = 200;

    public SplashScreen(String msg) {
        init(null);
        setText(msg);
    }

    public SplashScreen(ImageIcon img) {
        init(img);
    }

    protected void init(ImageIcon img) {

        JPanel pan = (JPanel) getContentPane();
        JLabel piccy;
        if (img == null) {
            piccy = new JLabel();
        } else {
            piccy = new JLabel(img);
        }

        piccy.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        text = new JLabel("Building....", JLabel.CENTER);
        text.setFont(new Font("Sans-Serif", Font.BOLD, 12));
        text.setBorder(BorderFactory.createEtchedBorder());

        pb = new JProgressBar(MIN, MAX);
        pb.setBorder(BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        JPanel pan2 = new JPanel();
        pan2.setLayout(new BorderLayout());

        pan2.add(text, BorderLayout.NORTH);
        pan2.add(pb, BorderLayout.SOUTH);

        pan.setLayout(new BorderLayout());
        pan.add(piccy, BorderLayout.CENTER);
        pan.add(pan2, BorderLayout.SOUTH);

        pan.setBorder(BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        pack();

        Dimension size = getSize();
        Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (scr.width - size.width) / 2;
        int y = (scr.height - size.height) / 2;
        setBounds(x, y, size.width, size.height);
    }

    public void setText(String txt) {
        text.setText(txt);
    }

    public void actionPerformed(ActionEvent a) {
        if (total < MAX) {
            total++;
        } else {
            total = MIN;
        }
        pb.setValue(total);
    }

    public void buildStarted(BuildEvent event) {
        actionPerformed(null);
    }

    public void buildFinished(BuildEvent event) {
        pb.setValue(MAX);
        setVisible(false);
        dispose();
    }
    public void targetStarted(BuildEvent event) {
        actionPerformed(null);
    }

    public void targetFinished(BuildEvent event) {
        actionPerformed(null);
    }

    public void taskStarted(BuildEvent event) {
        actionPerformed(null);
    }

    public void taskFinished(BuildEvent event) {
        actionPerformed(null);
    }

    public void messageLogged(BuildEvent event) {
        actionPerformed(null);
    }
}

