/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.tasks;

import org.apache.tools.ant.*;
import org.apache.tools.ant.helper.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Stack;
import org.xml.sax.Locator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;
import org.xml.sax.DocumentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.AttributeList;
import org.xml.sax.helpers.XMLReaderAdapter;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.AttributeListImpl;

import org.apache.tools.ant.util.JAXPUtils;

/**
 * Import task.
 *
 * It must be 'top level'. On execution it'll read another file
 * into the same Project. 
 *
 * @author Nicola Ken Barozzi nicolaken@apache.org
 * @author Dominique Devienne DDevienne@lgc.com
 * @author Costin Manolache
 */
public class Import extends Task {
    String file;

    public void setFile( String file ) {
        // I don't think we can use File - different rules
        // for relative paths.
        this.file=file;
    }
    
    /**
     * Initialisation routine called after handler creation
     * with the element name and attributes. The attributes which
     * this handler can deal with are: <code>"default"</code>,
     * <code>"name"</code>, <code>"id"</code> and <code>"basedir"</code>.
     *
     * @param tag Name of the element which caused this handler
     *            to be created. Should not be <code>null</code>.
     *            Ignored in this implementation.
     * @param attrs Attributes of the element which caused this
     *              handler to be created. Must not be <code>null</code>.
     *
     * @exception SAXParseException if an unexpected attribute is
     *            encountered or if the <code>"default"</code> attribute
     *            is missing.
     */
    public void execute() throws BuildException
    {
        if (file == null) {
            throw new BuildException("import element appears without a file attribute");
        }
    
        ProjectHelperImpl2.AntXmlContext context;
        context=(ProjectHelperImpl2.AntXmlContext)project.getReference("ant.parsing.context");

        context.importlevel++;

        project.log("importlevel: "+(context.importlevel-1)+" -> "+(context.importlevel),
                    Project.MSG_DEBUG);
        project.log("Importing file "+file+" from "+
                    context.buildFile.getAbsolutePath(),
                    Project.MSG_VERBOSE);

        // Paths are relative to the build file they're imported from,
        // *not* the current directory (same as entity includes).
        File importedFile = new File(file);
        if (!importedFile.isAbsolute()) {
            importedFile = new File(context.buildFileParent, file);
        }
        if (!importedFile.exists()) {
                throw new BuildException("Cannot find "+file+" imported from "+
                                         context.buildFile.getAbsolutePath());
        }
        
        // Add parent build file to the map to avoid cycles...
        String parentFilename = getPath(context.buildFile);
        if (!context.importedFiles.containsKey(parentFilename)) {
            context.importedFiles.put(parentFilename, context.buildFile);
        }

        // Make sure we import the file only once
        String importedFilename = getPath(importedFile);
        if (context.importedFiles.containsKey(importedFilename)) {
            project.log("\nSkipped already imported file:\n   "+importedFilename+"\n",
                        Project.MSG_WARN);
            context.importlevel--;
            project.log("importlevel: "+context.importlevel+" <- "+
                        (context.importlevel+1) ,Project.MSG_DEBUG);          
            return;
        } else {
            context.importedFiles.put(importedFilename, importedFile);
        }
    
        context.ignoreProjectTag=true;
        context.helper.parse(project, importedFile, new ProjectHelperImpl2.RootHandler(context));
        
        context.importlevel--;
        project.log("importlevel: "+context.importlevel+" <- "+
                            (context.importlevel+1) ,Project.MSG_DEBUG);          
    }
    
    private static String getPath(File file) {
        try {
            return file.getCanonicalPath();
        }
        catch (IOException e) {
            return file.getAbsolutePath();
        }
    }
}
