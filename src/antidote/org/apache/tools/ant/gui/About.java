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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
import org.apache.tools.ant.gui.util.WindowUtils;

import org.apache.tools.ant.gui.core.AppContext;
import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Dialog displaying information on the application.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class About extends JDialog {

	/** 
	 * Standard ctor.
	 * 
	 * @param context Application context.
	 */
	public About(AppContext context) {
		super(context.getParentFrame(), true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		String version = null;
		String date = null;

		Properties props = new Properties();

		try {
			props.load(getClass().getResourceAsStream("version.txt"));
		}
		catch(IOException ex) {
			// XXX log me.
			ex.printStackTrace();
			return;
		}

		version = props.getProperty("VERSION", "??");
		date = props.getProperty("DATE", "??");

		String message = context.getResources().getMessage(
			getClass(), "message", 
			new Object[] { version, date });

		String title = context.getResources().getString(
			getClass(), "title");
		setTitle(title);

        JTextPane contents = new JTextPane();
        contents.setContentType("text/html");
        contents.setText(message);
        contents.setEditable(false);
        // XXX Still not sure why this is necessary. JTextPane doesn't 
        // seem to report a "true" preferred size.
        contents.setPreferredSize(
            new Dimension(contents.getPreferredSize().width, 450));
		getContentPane().add(BorderLayout.CENTER, contents);

		// Add the OK button.
		JButton ok = new JButton(
			context.getResources().getString(getClass(), "ok"));
		ok.addActionListener(new ActionHandler());
		JPanel p = new JPanel();
		p.add(ok);
		getContentPane().add(BorderLayout.SOUTH, p);

        getRootPane().setDefaultButton(ok);


		// Just go ahead and show it...
		pack();
		WindowUtils.centerWindow(context.getParentFrame(), this);
		setVisible(true);
	}

	/** Handles press of the OK button. */
	private class ActionHandler implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			WindowUtils.sendCloseEvent(About.this);
		}
	}

}
