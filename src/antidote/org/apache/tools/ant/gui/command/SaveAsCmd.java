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
package org.apache.tools.ant.gui.command;
import org.apache.tools.ant.gui.core.AppContext;
import org.apache.tools.ant.gui.event.ErrorEvent;
import org.apache.tools.ant.gui.acs.ACSProjectElement;
import java.io.*;
import org.apache.tools.ant.gui.core.XMLFileFilter;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.JOptionPane;
import java.net.URL;
import java.net.MalformedURLException;


/**
 * Command for doing a "Save as" type of save.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class SaveAsCmd extends AbstractCommand {
    /** File to save to. */
    private URL _location = null;
    /** Project to save. */
    private ACSProjectElement _project = null;

	/** 
	 * Standard ctor.
	 * 
	 * @param context Application context.
	 */
    public SaveAsCmd(AppContext context) {
        super(context);
    }

	/** 
	 * Set the location to save to
	 * 
	 * @param location location to save to.
	 */
    public void setLocation(URL location) {
        _location = location;
    }

    /** 
     * Set the specific project to save (instead of the default).
     * 
     * @param project Project to save.
     */
    public void setProject(ACSProjectElement project) {
        _project = project;
    }


	/** 
	 * Save the project to the current file name.
	 * 
	 */
    public void run() {
        FileFilter filter = new XMLFileFilter(getContext().getResources());

        if(_project == null) {
            _project = getContext().getSelectionManager().getSelectedProject();
        }

        if(_project != null) {
            // If no location is specified, then this truly is a SaveAs 
            // command. Provide the user the UI to select the output.
            if(_location == null) {
                JFileChooser chooser = new JFileChooser();
                chooser.addChoosableFileFilter(filter);
                int val = chooser.showSaveDialog(
                    getContext().getParentFrame());
                if(val == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    if(file.exists()) {
                        String title = getContext().getResources().
                            getString(SaveCmd.class, "title");
                        String message = getContext().getResources().
                            getMessage(SaveCmd.class, "overwrite", 
                                       new Object[] { file.toString()});
                        val = JOptionPane.showConfirmDialog(
                            getContext().getParentFrame(), message, title, 
                            JOptionPane.YES_NO_OPTION);
                        // If cancelled unset file.
                        if(val != JOptionPane.YES_OPTION) {
                            return;
                        }
                    }
                    try {
                        _location = new URL(
                            "file", null, file.getAbsolutePath());
                    }
                    catch(MalformedURLException ex) {
                        // Shouldn't happen. Save will just not
                        // happen.
                        ex.printStackTrace();
                    }
                }
            }
            
            // If a location is now available, do the save operation.
            if(_location != null) {
                try {
                    getContext().getProjectManager().saveAs(
                        _project, _location);
                }
                catch(IOException ex) {
                    String message = getContext().getResources().getMessage(
                        SaveCmd.class, "saveError", 
                        new Object[] { _location.toString() });
                    
                    getContext().getEventBus().
                        postEvent(new ErrorEvent(getContext(), message, ex));
                }
            }
        }
        else {
            // We shouldn't ever get here.
            String message = getContext().getResources().getString(
                SaveCmd.class, "noProject"); 
            
            getContext().getEventBus().
                postEvent(new ErrorEvent(getContext(), message));
            
        }
    }

}
