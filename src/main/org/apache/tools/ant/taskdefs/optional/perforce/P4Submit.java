/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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
/*
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.tools.ant.taskdefs.optional.perforce;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import java.util.Vector;

/** Submits a numbered changelist to Perforce.
 *
 * <B>Note:</B> P4Submit cannot (yet) submit the default changelist.
 * This shouldn't be a problem with the ANT task as the usual flow is
 * P4Change to create a new numbered change followed by P4Edit then P4Submit.
 *
 * Example Usage:-<br>
 * &lt;p4submit change="${p4.change}" /&gt;
 *
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 *
 * @ant.task category="scm"
 */
public class P4Submit extends P4Base {

    //ToDo: If dealing with default cl need to parse out <enter description here>
    public String change;

    /**
     * @param change The changelist number to submit; required.
     */
    public void setChange(String change) {
        this.change = change;
    }

    public void execute() throws BuildException {
        if (change != null) {
            execP4Command("submit -c " + change, (P4HandlerAdapter) new P4SubmitAdapter());
        } else {
            //here we'd parse the output from change -o into submit -i
            //in order to support default change.
            throw new BuildException("No change specified (no support for default change yet....");
        }
    }

    public class P4SubmitAdapter extends P4HandlerAdapter {
        public void process(String line) {
            log(line, Project.MSG_VERBOSE);
            getProject().setProperty("p4.needsresolve","0");
            // this type of output might happen
            // Change 18 renamed change 20 and submitted.
            if (util.match("/renamed/", line)) {
                try {
                    Vector myarray = new Vector();
                    util.split(myarray, line);
                    boolean found = false;
                    for (int counter = 0; counter < myarray.size(); counter++) {
                        if (found == true) {
                            int changenumber = Integer.parseInt((String) myarray.elementAt(counter + 1));
                            log("Perforce change renamed " + changenumber, Project.MSG_INFO);
                            getProject().setProperty("p4.change", "" + changenumber);
                            found = false;
                        }
                        if (((String) (myarray.elementAt(counter))).equals("renamed")) {
                            found = true;
                        }
                    }
                }
                        // NumberFormatException or ArrayOutOfBondsException could happen here
                catch (Exception e) {
                    String msg = "Failed to parse " + line  + "\n"
                            + " due to " + e.getMessage();
                    throw new BuildException(msg, e, getLocation());
                }
            }
            if (util.match("/p4 submit -c/",line)) {
                getProject().setProperty("p4.needsresolve","1");
            }

        }
    }

}
