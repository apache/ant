/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.util.*;
import org.apache.tools.ant.taskdefs.compilers.*;
import org.apache.tools.ant.Task;

import java.io.*;

import java.util.*;

/**
 * This task is the manager for RecorderEntry's.  It is this class
 * that holds all entries, modifies them every time the <recorder>
 * task is called, and addes them to the build listener process.
 * @see RecorderEntry
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 * @version 0.5
 *
 */
public class Recorder extends Task {

    //////////////////////////////////////////////////////////////////////
    // ATTRIBUTES

    /** The name of the file to record to. */
    private String filename = null;
    /** Whether or not to append.  Need Boolean to record an unset
     *  state (null).
     */
    private Boolean append = null;
    /** Whether to start or stop recording.  Need Boolean to record an
     *  unset state (null).
     */
    private Boolean start = null;
    /** What level to log?  -1 means not initialized yet. */
    private int loglevel = -1;
    /** The list of recorder entries. */
    private static Hashtable recorderEntries = new Hashtable();

    //////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS / INITIALIZERS

    //////////////////////////////////////////////////////////////////////
    // ACCESSOR METHODS

    /**
     * Sets the name of the file to log to, and the name of the recorder entry.
     * @param fname File name of logfile.
     */
    public void setName( String fname ) {
        filename = fname;
    }

    /**
     * Sets the action for the associated recorder entry.
     * @param action The action for the entry to take: start or stop.
     */
    public void setAction( ActionChoices action ) {
        if ( action.getValue().equalsIgnoreCase( "start" ) ) {
            start = Boolean.TRUE;
        } else {
            start = Boolean.FALSE;
        }
    }

    /**
     * Whether or not the logger should append to a previous file.
     */
    public void setAppend( boolean append ) {
        this.append = new Boolean(append);
    }

    /**
     * Sets the level to which this recorder entry should log to.
     * @see VerbosityLevelChoices
     */
    public void setLoglevel( VerbosityLevelChoices level ){
        //I hate cascading if/elseif clauses !!!
        String lev = level.getValue();
        if ( lev.equalsIgnoreCase("error") ) {
            loglevel = Project.MSG_ERR;
        } else if ( lev.equalsIgnoreCase("warn") ){
            loglevel = Project.MSG_WARN;
        } else if ( lev.equalsIgnoreCase("info") ){
            loglevel = Project.MSG_INFO;
        } else if ( lev.equalsIgnoreCase("verbose") ){
            loglevel = Project.MSG_VERBOSE;
        } else if ( lev.equalsIgnoreCase("debug") ){
            loglevel = Project.MSG_DEBUG;
        }
    }

    //////////////////////////////////////////////////////////////////////
    // CORE / MAIN BODY

    /**
     * The main execution.
     */
    public void execute() throws BuildException {
        if ( filename == null )
            throw new BuildException( "No filename specified" );

        getProject().log( "setting a recorder for name " + filename,
            Project.MSG_DEBUG );

        // get the recorder entry
        RecorderEntry recorder = getRecorder( filename, getProject() );
        // set the values on the recorder
        recorder.setMessageOutputLevel( loglevel );
        recorder.setRecordState( start );
    }

    //////////////////////////////////////////////////////////////////////
    // INNER CLASSES

    /**
     * A list of possible values for the <code>setAction()</code> method.
     * Possible values include: start and stop.
     */
    public static class ActionChoices extends EnumeratedAttribute {
        private static final String[] values = {"start", "stop"};
        public String[] getValues() {
            return values;
        }
    }

    /**
     * A list of possible values for the <code>setLoglevel()</code> method.
     * Possible values include: error, warn, info, verbose, debug.
     */
    public static class VerbosityLevelChoices extends EnumeratedAttribute {
        private static final String[] values = { "error", "warn", "info",
            "verbose", "debug"};
        public String[] getValues() {
            return values;
        }
    }

    /**
     * Gets the recorder that's associated with the passed in name.
     * If the recorder doesn't exist, then a new one is created.
     */
    protected RecorderEntry getRecorder( String name, Project proj ) throws BuildException {
        Object o = recorderEntries.get(name);
        RecorderEntry entry;
        if ( o == null ) {
            // create a recorder entry
            try {
                entry = new RecorderEntry( name );
                PrintStream out = null;
                if ( append == null ) {
                    out = new PrintStream(
                        new FileOutputStream(name));
                } else {
                    out = new PrintStream(
                        new FileOutputStream(name, append.booleanValue()));
                }
                entry.setErrorPrintStream(out);
                entry.setOutputPrintStream(out);
            } catch ( IOException ioe ) {
                throw new BuildException( "Problems creating a recorder entry",
                    ioe );
            }
            proj.addBuildListener(entry);
            recorderEntries.put(name, entry);
        } else {
            entry = (RecorderEntry) o;
        }
        return entry;
    }

}
