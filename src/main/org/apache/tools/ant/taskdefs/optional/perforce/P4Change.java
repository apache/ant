/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.tools.ant.taskdefs.optional.perforce;

import java.io.*;

import org.apache.tools.ant.*;

/** P4Change - grab a new changelist from Perforce.
 *
 * P4Change creates a new changelist in perforce. P4Change sets the property
 * ${p4.change} with the new changelist number. This should then be passed into
 * p4edit and p4submit.
 *
 * @see P4Edit
 * @see P4Submit
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 *
 */
public class P4Change extends P4Base {

    protected String emptyChangeList = null;
    protected String description = "AutoSubmit By Ant";

    public void execute() throws BuildException {

        if(emptyChangeList == null) emptyChangeList = getEmptyChangeList();
        final Project myProj = project;

        P4Handler handler = new P4HandlerAdapter() {
                public void process(String line) {
                    if (util.match("/Change/", line)) {
                    
                        //Remove any non-numerical chars - should leave the change number
                        line = util.substitute("s/[^0-9]//g", line);
                                
                        int changenumber = Integer.parseInt(line);
                        log("Change Number is "+changenumber, Project.MSG_INFO);
                        myProj.setProperty("p4.change", ""+changenumber);

                    } else if(util.match("/error/", line)) {
                        throw new BuildException("Perforce Error, check client settings and/or server");
                    }
                                
                }};

        handler.setOutput(emptyChangeList);

        execP4Command("change -i", handler);
    }


    public String getEmptyChangeList() throws BuildException {
        final StringBuffer stringbuf = new StringBuffer();
        
        execP4Command("change -o", new P4HandlerAdapter() {
                public void process(String line) {
                    if(!util.match("/^#/",line)){
                        if(util.match("/error/", line)) {
                                
                            log("Client Error", Project.MSG_VERBOSE);
                            throw new BuildException("Perforce Error, check client settings and/or server");
                                    
                        } else if(util.match("/<enter description here>/",line)) {

                            line = util.substitute("s/<enter description here>/" + description + "/", line);
                                        
                        } else if(util.match("/\\/\\//", line)) {
                            //Match "//" for begining of depot filespec
                            return;
                        }
                                    
                        stringbuf.append(line);
                        stringbuf.append("\n");
                                
                    }
                }});
                
        return stringbuf.toString();
    }

    /* Set Description Variable. */
    public void setDescription(String desc){
        this.description = desc;
    }

} //EoF
