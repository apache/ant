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
package org.apache.tools.ant.gui.command;
import org.apache.tools.ant.gui.AppContext;
import org.apache.tools.ant.gui.ProjectProxy;
import org.apache.tools.ant.gui.event.ErrorEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.tools.ant.gui.XMLFileFilter;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.JOptionPane;

/**
 * Command to execute the saving of the current build file.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class SaveCmd implements Command {
    /** Name of the action the command maps to. */
    public static final String ACTION_NAME = "save";

    /** The application context */
    private AppContext _context = null;
    /** Filter for showing only XML files. */
    private FileFilter _filter = null;
    /** File to save to. */
    private File _file = null;

	/** 
	 * Standard ctor with file.
	 * 
	 * @param context Application context.
	 * @param file File to save to, or null.
	 */
    public SaveCmd(AppContext context, File file) {
        _context = context;
        _filter = new XMLFileFilter(_context.getResources());
        _file = file;
    }

	/** 
	 * Standard ctor.
	 * 
	 * @param context Application context. 
	 */
    public SaveCmd(AppContext context) {
        this(context, context.getProject() == null ? null : 
             context.getProject().getFile());
    }

	/** 
	 * Save the project to the current file name.
	 * 
	 */
    public void execute() {
        ProjectProxy project = _context.getProject();
        if(project != null) {
            if(_file == null) {
                // XXX code here to select a file to save to.
                JFileChooser chooser = new JFileChooser();
                chooser.addChoosableFileFilter(_filter);
                int val = chooser.showSaveDialog(_context.getParentFrame());
                if(val == JFileChooser.APPROVE_OPTION) {
                    _file = chooser.getSelectedFile();
                    if(_file.exists()) {
                        String title = _context.getResources().getString(
                            SaveCmd.class, "title");                         
                        String message = _context.getResources().getMessage(
                            SaveCmd.class, "overwrite", 
                            new Object[] {_file.toString()});
                        val = JOptionPane.showConfirmDialog(
                            _context.getParentFrame(), message, title, 
                            JOptionPane.YES_NO_OPTION);
                        // If cancelled unset file.
                        if(val != JOptionPane.YES_OPTION) {
                            _file = null;
                        }
                    }
                }
            }
            
            if(_file != null) {
                project.setFile(_file);
                FileWriter out = null;
                try {
                    out = new FileWriter(_file);
                    project.write(out);
                }
                catch(IOException ex) {
                    String message = _context.getResources().getMessage(
                        SaveCmd.class, "saveError", 
                        new Object[] { _file.toString() });
                    
                    _context.getEventBus().
                        postEvent(new ErrorEvent(_context, message));
                }
                finally {
                    if (out != null) {
                        try {
                            out.flush();
                            out.close();
                        }
                        catch(IOException ex) {
                            // Intentionally ignored.
                        }
                    }
                }
            }
        }
        else {
            // We shouldn't ever get here.
            String message = _context.getResources().getString(
                SaveCmd.class, "noProject"); 
            
            _context.getEventBus().
                postEvent(new ErrorEvent(_context, message));
            
        }
    }
}
