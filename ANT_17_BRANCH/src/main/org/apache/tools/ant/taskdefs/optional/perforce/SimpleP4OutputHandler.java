/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
/*
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.tools.ant.taskdefs.optional.perforce;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.StringUtils;

/**
 * simple implementation of P4HandlerAdapter used by tasks which are not
 * actually processing the output from Perforce
 */
public class SimpleP4OutputHandler extends P4HandlerAdapter {

    // CheckStyle:VisibilityModifier OFF - bc
    P4Base parent;
    // CheckStyle:VisibilityModifier ON

    /**
     * simple constructor
     * @param parent  a P4Base instance
     */
    public SimpleP4OutputHandler(P4Base parent) {
        this.parent = parent;
    }

    /**
     * process one line of stderr/stdout
     * if error conditions are detected, then setters are called on the
     * parent
     * @param line line of output
     * @throws BuildException does not throw exceptions any more
     */
    public void process(String line) throws BuildException {
        if (parent.util.match("/^exit/", line)) {
            return;
        }

        //Throw exception on errors (except up-to-date)
        //
        //When a server is down, the code expects :
        //Perforce client error:
        //Connect to server failed; check $P4PORT.
        //TCP connect to localhost:1666 failed.
        //connect: localhost:1666: Connection refused
        //Some forms producing commands (p4 -s change -o) do tag the output
        //others don't.....
        //Others mark errors as info, for example edit a file
        //which is already open for edit.....
        //Just look for error: - catches most things....

        if (parent.util.match("/^error:/", line)
            || parent.util.match("/^Perforce client error:/", line)) {
            //when running labelsync, if view elements are in sync,
            //Perforce produces a line of output
            //looking like this one :
            //error: //depot/file2 - label in sync.
            if (!parent.util.match("/label in sync/", line)
                && !parent.util.match("/up-to-date/", line)) {
                parent.setInError(true);
            } else {
                //sync says "error:" when a file is up-to-date
                line = parent.util.substitute("s/^[^:]*: //", line);
            }
        } else if (parent.util.match("/^info.*?:/", line)) {
            //sometimes there's "info1:
            line = parent.util.substitute("s/^[^:]*: //", line);
        }
        parent.log(line, parent.getInError() ? Project.MSG_ERR : Project.MSG_INFO);

        if (parent.getInError()) {
            parent.setErrorMessage(parent.getErrorMessage() + line + StringUtils.LINE_SEP);
        }
    }
}
