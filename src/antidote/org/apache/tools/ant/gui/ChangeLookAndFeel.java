/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2000 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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
package org.apache.tools.ant.gui;

import org.apache.tools.ant.gui.event.ErrorEvent;
import org.apache.tools.ant.gui.util.WindowUtils;

import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Dialog for changing the current Look and Feel
 *
 * @version $Revision$
 * @author Erik Meade
 */
public class ChangeLookAndFeel extends JDialog {

	private AppContext _context;

	private JList lookAndFeels;
	/**
	 * Standard ctor.
	 *
	 * @param context Application context.
	 */
	public ChangeLookAndFeel(AppContext context) {
		super(context.getParentFrame(), true);
		_context = context;
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		setTitle(context.getResources().getString(
			getClass(), "title"));

		// Add the OK button.
		JButton ok = new JButton(
			context.getResources().getString(getClass(), "ok"));
		ok.addActionListener(new ActionHandler());
		JPanel p = new JPanel();
		p.add(ok);
		Container contentPane = getContentPane();
        getRootPane().setDefaultButton(ok);
        p.setPreferredSize(new Dimension(150, p.getPreferredSize().height));
		contentPane.add(BorderLayout.SOUTH, p);

		// Add the list of look and feels
		p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(
            context.getResources().getString(getClass(), "border")));

		UIManager.LookAndFeelInfo[] lookAndFeelInfos = 
            UIManager.getInstalledLookAndFeels();
		String[] lookAndFeelNames = new String[lookAndFeelInfos.length];
		for (int i = 0; i < lookAndFeelInfos.length; i++) {
			lookAndFeelNames[i] = lookAndFeelInfos[i].getName();
		}

		lookAndFeels = new JList(lookAndFeelNames);
		lookAndFeels.setSelectedValue(
            UIManager.getLookAndFeel().getName(), true);
		p.add(lookAndFeels);
		contentPane.add(BorderLayout.CENTER, p);

		// Just go ahead and show it...
		pack();
		WindowUtils.centerWindow(context.getParentFrame(), this);
		setVisible(true);
	}

	/** Handles press of the OK button. */
	private class ActionHandler implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			try {
				UIManager.setLookAndFeel(getLookAndFeelClass(
                    (String)lookAndFeels.getSelectedValue()));
				SwingUtilities.updateComponentTreeUI(
                    _context.getParentFrame());
			} catch (Exception ex) {
                _context.getEventBus().postEvent(
                    new ErrorEvent(
                        _context, _context.getResources().getString(
                            ChangeLookAndFeel.class, "error") + ex));
			}
			WindowUtils.sendCloseEvent(ChangeLookAndFeel.this);
		}
	}

	/** 
	 * Get the Look and Feel class for the given name.
	 * 
	 * @param lookAndFeelName Name of the look and feel.
	 * @return Class name to load.
	 */
	private String getLookAndFeelClass(String lookAndFeelName) {
		UIManager.LookAndFeelInfo[] lookAndFeelInfos = 
            UIManager.getInstalledLookAndFeels();
		for (int i = 0; i < lookAndFeelInfos.length; i++) {
			if (lookAndFeelInfos[i].getName().equals(lookAndFeelName))
				return lookAndFeelInfos[i].getClassName();
		}
		return null;
	}
}
