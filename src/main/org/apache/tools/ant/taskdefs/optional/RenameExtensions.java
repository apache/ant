/*
 * The Apache Software License, Version 1.1
 * 
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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
 *
 * Task to rename files based on extension. This task has the following
 * properties which can be set:
 * <ul>
 * <li>fromExtension: </li>
 * <li>toExtension: </li>
 * <li>srcDir: </li>
 * <li>replace: </li>
 * </ul>
 */

package org.apache.tools.ant.taskdefs.optional;

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.Mapper;

/**
 *
 * @author dIon Gillard <a href="mailto:dion@multitask.com.au">dion@multitask.com.au</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version 1.2
 */
public class RenameExtensions extends MatchingTask {

    private String fromExtension = "";
    private String toExtension = "";
    private boolean replace = false;
    private File srcDir;

    private Mapper.MapperType globType;


    /** Creates new RenameExtensions */
    public RenameExtensions() {
        super();
        globType = new Mapper.MapperType();
        globType.setValue("glob");
    }

    /** store fromExtension **/
    public void setFromExtension(String from) {
        fromExtension = from;
    }

    /** store toExtension **/
    public void setToExtension(String to) {
        toExtension = to;
    }

    /**
     * store replace attribute - this determines whether the target file
     * should be overwritten if present
     */
    public void setReplace(boolean replace) {
        this.replace = replace;
    }

    /**
     * Set the source dir to find the files to be renamed.
     */
    public void setSrcDir(File srcDir) {
        this.srcDir = srcDir;
    }

    /**
     * Executes the task, i.e. does the actual compiler call
     */
    public void execute() throws BuildException {

        // first off, make sure that we've got a from and to extension
        if (fromExtension == null || toExtension == null || srcDir == null) {
            throw new BuildException( "srcDir, fromExtension and toExtension " +
                                      "attributes must be set!" );
        }

        log("DEPRECATED - The renameext task is deprecated.  Use move instead.",
            Project.MSG_WARN);
        log("Replace this with:", Project.MSG_INFO);
        log("<move todir=\""+srcDir+"\" overwrite=\""+replace+"\">", 
            Project.MSG_INFO);
        log("  <fileset dir=\""+srcDir+"\" />", Project.MSG_INFO);
        log("  <mapper type=\"glob\"", Project.MSG_INFO);
        log("          from=\"*"+fromExtension+"\"", Project.MSG_INFO);
        log("          to=\"*"+toExtension+"\" />", Project.MSG_INFO);
        log("</move>", Project.MSG_INFO);
        log("using the same patterns on <fileset> as you\'ve used here", 
            Project.MSG_INFO);

        Move move = (Move)project.createTask("move");
        move.setOwningTarget(target);
        move.setTaskName(getTaskName());
        move.setLocation(getLocation());
        move.setTodir(srcDir);
        move.setOverwrite(replace);

        fileset.setDir(srcDir);
        move.addFileset(fileset);

        Mapper me = move.createMapper();
        me.setType(globType);
        me.setFrom("*"+fromExtension);
        me.setTo("*"+toExtension);

        move.execute();
    }

}
