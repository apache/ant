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

import org.apache.tools.ant.*;

/** P4Sync  - synchronise client space to a perforce depot view.
 *  The API allows additional functionality of the "p4 sync" command 
 * (such as "p4 sync -f //...#have" or other exotic invocations).</P>
 *
 * <b>Example Usage:</b>
 * <table border="1">
 * <th>Function</th><th>Command</th>
 * <tr><td>Sync to head using P4USER, P4PORT and P4CLIENT settings specified</td><td>&lt;P4Sync <br>P4view="//projects/foo/main/source/..." <br>P4User="fbloggs" <br>P4Port="km01:1666" <br>P4Client="fbloggsclient" /&gt;</td></tr>
 * <tr><td>Sync to head using P4USER, P4PORT and P4CLIENT settings defined in environment</td><td>&lt;P4Sync P4view="//projects/foo/main/source/..." /&gt;</td></tr>
 * <tr><td>Force a re-sync to head, refreshing all files</td><td>&lt;P4Sync force="yes" P4view="//projects/foo/main/source/..." /&gt;</td></tr>
 * <tr><td>Sync to a label</td><td>&lt;P4Sync label="myPerforceLabel" /&gt;</td></tr>
 * </table>
 *
 * ToDo:  Add decent label error handling for non-exsitant labels
 *
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 */
public class P4Sync extends P4Base {

    String label;
    private String syncCmd = "";

    public void setLabel(String label) throws BuildException { 
        if(label == null && !label.equals(""))
                throw new BuildException("P4Sync: Labels cannot be Null or Empty");

        this.label = label;

    }


    public void setForce(String force) throws BuildException {
        if(force == null && !label.equals(""))
                throw new BuildException("P4Sync: If you want to force, set force to non-null string!");
            P4CmdOpts = "-f";
        }
        
    public void execute() throws BuildException {


        if (P4View != null) {
                syncCmd = P4View;
        }

        
        if(label != null && !label.equals("")) {
                syncCmd = syncCmd + "@" + label;
        } 

        
        log("Execing sync "+P4CmdOpts+" "+syncCmd, Project.MSG_VERBOSE);

        execP4Command("-s sync "+P4CmdOpts+" "+syncCmd, new SimpleP4OutputHandler(this));
    }
}
