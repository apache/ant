/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.taskdefs.optional.splash;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;

import javax.swing.JWindow;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;

class SplashScreen extends JWindow implements ActionListener, BuildListener {

    private JLabel text;
    private JProgressBar pb;
    private int total;
    private final int min = 0;
    private final int max = 200;

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

        pb = new JProgressBar(min, max);
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
        if (total < max) {
            total++;
        } else {
            total = min;
        }
        pb.setValue(total);
    }

    public void buildStarted(BuildEvent event){ actionPerformed(null);}
    public void buildFinished(BuildEvent event){
        pb.setValue(max);
        setVisible(false);
        dispose();
    }
    public void targetStarted(BuildEvent event){actionPerformed(null);}
    public void targetFinished(BuildEvent event){actionPerformed(null);}
    public void taskStarted(BuildEvent event){actionPerformed(null);}
    public void taskFinished(BuildEvent event){actionPerformed(null);}
    public void messageLogged(BuildEvent event){actionPerformed(null);}
}

