/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.vss;

import java.io.File;
import java.text.SimpleDateFormat;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Performs History commands to Microsoft Visual SourceSafe.
 *
 * @author Balazs Fejes 2
 * @author Glenn_Twiggs@bmc.com
 * @author Jesse Stockall
 *
 * @ant.task name="vsshistory" category="scm"
 */
public class MSVSSHISTORY extends MSVSS {

    /**
     * Builds a command line to execute ss.
     * @return     The constructed commandline.
     */
    Commandline buildCmdLine() {
        Commandline commandLine = new Commandline();

        // first off, make sure that we've got a command and a vssdir and a label ...
        if (getVsspath() == null) {
            String msg = "vsspath attribute must be set!";
            throw new BuildException(msg, getLocation());
        }

        // build the command line from what we got the format is
        // ss History elements [-H] [-L] [-N] [-O] [-V] [-Y] [-#] [-?]
        // as specified in the SS.EXE help
        commandLine.setExecutable(getSSCommand());
        commandLine.createArgument().setValue(COMMAND_HISTORY);

        // VSS items
        commandLine.createArgument().setValue(getVsspath());
        // -I-
        commandLine.createArgument().setValue(FLAG_AUTORESPONSE_DEF);  // ignore all errors
        // -Vd
        commandLine.createArgument().setValue(getVersionDate());
        // -VL
        commandLine.createArgument().setValue(getVersionLabel());
        // -R
        commandLine.createArgument().setValue(getRecursive());
        // -B / -D / -F-
        commandLine.createArgument().setValue(getStyle());
        // -Y
        commandLine.createArgument().setValue(getLogin());
        // -O
        commandLine.createArgument().setValue(getOutput());

        return commandLine;
    }

    /**
     * Flag to tell the task to recurse down the tree;
     * optional, default false.
     * @param recursive  The boolean value for recursive.
     */
    public void setRecursive(boolean recursive) {
        super.setInternalRecursive(recursive);
    }

    /**
     * Sets the username of the user whose changes we would like to see.; optional
     * @param   user The username.
     */
    public void setUser(String user) {
        super.setInternalUser(user);
    }

    /**
     * Set the Start Date for the comparison of two versions in SourceSafe
     * history.; optional
     * @param   fromDate    The start date.
     */
    public void setFromDate(String fromDate) {
        super.setInternalFromDate(fromDate);
    }

    /**
     * Set the End Date for the Comparison of two versions; optional.
     * @param   toDate    The end date.
     */
    public void setToDate(String toDate) {
        super.setInternalToDate(toDate);
    }

    /**
     * Set the Start Label; optional.
     * @param   fromLabel    The start label.
     */
    public void setFromLabel(String fromLabel) {
        super.setInternalFromLabel(fromLabel);
    }

    /**
     * Set the End label; optional.
     * @param   toLabel    The end label.
     */
    public void setToLabel(String toLabel) {
        super.setInternalToLabel(toLabel);
    }

    /**
     * Set the number of days for comparison;
     * optional.
     * <p>
     * The default value is 2 days. (maybe)
     * @param   numd    The number of days.
     */
    public void setNumdays(int numd) {
        super.setInternalNumDays(numd);
    }

    /**
     * Set the output file name for the history; optional.
     * @param   outfile The output file name.
     */
    public void setOutput(File outfile) {
        if (outfile != null) {
            super.setInternalOutputFilename(outfile.getAbsolutePath());
        }
    }

    /**
     * Format of dates in fromDate and toDate; optional.
     * Used when calculating dates with
     * the numdays attribute.
     * This string uses the formatting rules of SimpleDateFormat.
     *  Defaults to DateFormat.SHORT.
     * @param   dateFormat  The date format.
     */
    public void setDateFormat(String dateFormat) {
        super.setInternalDateFormat(new SimpleDateFormat(dateFormat));
    }

   /**
     * Specify the output style; optional.
     *
     * @param attr valid values:
     * <ul>
     * <li>brief:    -B Display a brief history.
     * <li>codediff: -D Display line-by-line file changes.
     * <li>nofile:   -F- Do not display individual file updates in the project history.
     * <li>default:  No option specified. Display in Source Safe's default format.
     * </ul>
     */
    public void setStyle(BriefCodediffNofile attr) {
        String option = attr.getValue();
        if (option.equals(STYLE_BRIEF)) {
            super.setInternalStyle(FLAG_BRIEF);
        } else if (option.equals(STYLE_CODEDIFF)) {
            super.setInternalStyle(FLAG_CODEDIFF);
        } else if (option.equals(STYLE_DEFAULT)) {
            super.setInternalStyle("");
        } else if (option.equals(STYLE_NOFILE)) {
            super.setInternalStyle(FLAG_NO_FILE);
        } else {
            throw new BuildException("Style " + attr + " unknown.", getLocation());
        }
    }

    /**
     * Extention of EnumeratedAttribute to hold the values for style.
     */
    public static class BriefCodediffNofile extends EnumeratedAttribute {
        /**
         * Gets the list of allowable values.
         * @return The values.
         */
        public String[] getValues() {
            return new String[] {STYLE_BRIEF, STYLE_CODEDIFF, STYLE_NOFILE, STYLE_DEFAULT};
        }
    }
}
