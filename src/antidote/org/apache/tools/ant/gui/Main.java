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
import org.apache.tools.ant.gui.core.*;
import org.apache.tools.ant.gui.util.XMLHelper;
import org.apache.tools.ant.gui.command.LoadFileCmd;
import javax.swing.*;
import java.awt.BorderLayout;
import java.io.File;

/**
 * Launch point for the Antidote GUI. Configurs it as an application.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class Main {
	/** 
	 * Application start.
	 * 
	 * @param args TBD
	 */
    public static void main(String[] args) {
        XMLHelper.init();

        try {
            JFrame f = new JFrame("Antidote");
            AppContext context = new AppContext(f);
            EventResponder resp = new EventResponder(context);
            Antidote gui = new Antidote(context);

            f.setDefaultCloseOperation(3 /*JFrame.EXIT_ON_CLOSE*/);
            f.setJMenuBar(context.getActions().createMenuBar());
            f.getContentPane().add(BorderLayout.CENTER, gui);
            f.getContentPane().add(BorderLayout.NORTH, 
                                   context.getActions().createToolBar());

            ImageIcon icon = 
                context.getResources().loadImageIcon("icon-small.gif");
            if(icon != null) {
                f.setIconImage(icon.getImage());
            }
            else {
                System.out.println("Application icon not found.");
            }
            f.pack();

            f.setVisible(true);

            // XXX this will change once full command line argument parsing
            // is supported.
            if(args.length > 0) {
                LoadFileCmd load = new LoadFileCmd(context);
                load.setFile(new File(args[0]));
                load.run();
            }
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
