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

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.lang.reflect.Constructor;

/**
 * The root class for the Ant GUI. Assembles all the graphical components
 * based on the configuration files.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class Antidote extends JPanel {


    /** Logging console. */
    private Console _console = null;

    /** Source of application state data. */
    private AppContext _context = null;

	/** 
	 * Default ctor.
	 * 
	 */
    public Antidote(AppContext context) {
        setLayout(new BorderLayout());

        _context = context;

        _console = new Console(_context);


        // Add the various editors/views to the editing area.
        JSplitPane splitter = new JSplitPane();
        splitter.add(JSplitPane.LEFT, populateEditors("left"));
        splitter.add(JSplitPane.RIGHT, populateEditors("right"));

        add(BorderLayout.CENTER, splitter);

        add(BorderLayout.SOUTH, _console);

        setPreferredSize(new Dimension(640, 480));
    }


	/** 
	 * Instantiate the configured editors.
	 * 
     * @return Component containing the editor(s).
	 */
    private JComponent populateEditors(String prefix) {

        String[] classNames = _context.getResources().
            getStringArray(getClass(), prefix + ".editors");

        AntEditor[] editors = new AntEditor[classNames.length];

        for(int i = 0; i < classNames.length; i++) {
            try {
                Class editorType = Class.forName(classNames[i]);

                Constructor ctor = 
                    editorType.getConstructor(AntEditor.CTOR_PARAMS);

                editors[i] = 
                    (AntEditor) ctor.newInstance(new Object[] { _context });
            }
            catch(Exception ex) {
                // XXX log me.
                ex.printStackTrace();
            }
        }

        if(editors.length == 1) {
            return editors[0];
        }
        else if(editors.length > 1) {
            JTabbedPane tabbed = new JTabbedPane(JTabbedPane.BOTTOM);

            for(int i = 0; i < editors.length; i++) {
                tabbed.addTab(editors[i].getName(), editors[i]);
            }
            return tabbed;
        }
        else {
            return new JLabel("I shouldn't be here...");
        }
    }
}
