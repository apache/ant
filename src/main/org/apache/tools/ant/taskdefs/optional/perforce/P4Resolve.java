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

/**
 * @ant.task category="scm"
 * @author <a href="mailto:antoine@antbuild.com">Antoine Levy-Lambert</a>
 */
public class P4Resolve extends P4Base {
    private String resolvemode = null;


    private boolean redoall; /* -f */
    private boolean simulationmode;  /* -n */
    private boolean forcetextmode;  /* -t */
    private boolean markersforall; /* -v */
    private static final String AUTOMATIC = "automatic";
    private static final String FORCE = "force";
    private static final String SAFE = "safe";
    private static final String THEIRS = "theirs";
    private static final String YOURS = "yours";
    private static final String[] RESOLVE_MODES = {
        AUTOMATIC,
        FORCE,
        SAFE,
        THEIRS,
        YOURS
    };
   /**
    * returns the resolve mode
    * @return  returns the resolve mode
    */
    public String getResolvemode() {
        return resolvemode;
    }
    /**
     * values for resolvemode
     * <ul>
     * <li> automatic -am</li>
     * <li> force -af </li>
     * <li> safe -as </li>
     * <li> theirs -at </li>
     * <li> yours -ay </li>
     * </ul>
     * @param resolvemode one of automatic, force, safe, theirs, yours
     */
    public void setResolvemode(String resolvemode) {
        boolean found = false;
        for (int counter = 0; counter < RESOLVE_MODES.length; counter++) {
            if (resolvemode.equals(RESOLVE_MODES[counter])) {
                found = true;
                break;
            }
        }
        if (found == false) {
            throw new BuildException("Unacceptable value for resolve mode");
        }
        this.resolvemode = resolvemode;
    }

    /**
     * allows previously resolved files to be resolved again
     * @return flag indicating whether one wants to
     * allow previously resolved files to be resolved again
     */
    public boolean isRedoall() {
        return redoall;
    }

    /**
     * set the redoall flag
     * @param redoall flag indicating whether one want to
     * allow previously resolved files to be resolved again
     */
    public void setRedoall(boolean redoall) {
        this.redoall = redoall;
    }

    /**
     * read the simulation mode flag
     * @return flag indicating whether one wants just to simulate
     * the p4 resolve operation whithout actually doing it
     */
    public boolean isSimulationmode() {
        return simulationmode;
    }

    /**
     * sets a flag
     * @param simulationmode set to true, lists the integrations which would be performed,
     * without actually doing them.
     */
    public void setSimulationmode(boolean simulationmode) {
        this.simulationmode = simulationmode;
    }

    /**
     * If set to true, attempts a textual merge, even for binary files
     * @return flag value
     */
    public boolean isForcetextmode() {
        return forcetextmode;
    }

    /**
     * If set to true, attempts a textual merge, even for binary files
     * @param forcetextmode set the flag value
     */
    public void setForcetextmode(boolean forcetextmode) {
        this.forcetextmode = forcetextmode;
    }

    /**
     * If set to true, puts in markers for all changes, conflicting or not
     * @return  flag markersforall value
     */
    public boolean isMarkersforall() {
        return markersforall;
    }

    /**
      * If set to true, puts in markers for all changes, conflicting or not
     * @param markersforall flag true or false
     */
    public void setMarkersforall(boolean markersforall) {
        this.markersforall = markersforall;
    }

    /**
     *  execute the p4 resolve
     * @throws BuildException if there is a wrong resolve mode specified
     *  or no view specified
     */
    public void execute() throws BuildException {
        if (this.resolvemode.equals(AUTOMATIC)) {
            P4CmdOpts = P4CmdOpts + " -am";
        } else if (this.resolvemode.equals(FORCE)) {
            P4CmdOpts = P4CmdOpts + " -af";
        } else if (this.resolvemode.equals(SAFE)) {
            P4CmdOpts = P4CmdOpts + " -as";
        } else if (this.resolvemode.equals(THEIRS)) {
            P4CmdOpts = P4CmdOpts + " -at";
        } else if (this.resolvemode.equals(YOURS)) {
            P4CmdOpts = P4CmdOpts + " -ay";
        } else {
            throw new BuildException("unsupported or absent resolve mode");
        }
        if (P4View == null) {
            throw new BuildException("please specify a view");
        }
        if (this.isRedoall()) {
            P4CmdOpts = P4CmdOpts + " -f";
        }
        if (this.isSimulationmode()) {
            P4CmdOpts = P4CmdOpts + " -n";
        }
        if (this.isForcetextmode()) {
            P4CmdOpts = P4CmdOpts + " -t";
        }
        if (this.isMarkersforall()) {
            P4CmdOpts = P4CmdOpts + " -v";
        }
        execP4Command("-s resolve " + P4CmdOpts + " " + P4View, new SimpleP4OutputHandler(this));
    }
}
