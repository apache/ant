/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.vss;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.EnumeratedAttribute;
import java.io.File;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
/**
 * Performs History commands to Microsoft Visual SourceSafe.
 *
 * @author Balazs Fejes 2
 * @author Glenn_Twiggs@bmc.com
 *
 * @ant.task name="vsshistory" category="scm"
 */

public class MSVSSHISTORY extends MSVSS {

    private String m_FromDate = null;
    private String m_ToDate = null;
    private DateFormat m_DateFormat =
        DateFormat.getDateInstance(DateFormat.SHORT);
    
    private String m_FromLabel = null;
    private String m_ToLabel = null;
    private String m_OutputFileName = null;
    private String m_User = null;
    private int m_NumDays = Integer.MIN_VALUE;
    private String m_Style = "";
    private boolean m_Recursive = false;
    
    public static final String VALUE_FROMDATE = "~d";
    public static final String VALUE_FROMLABEL = "~L";

    public static final String FLAG_OUTPUT = "-O";
    public static final String FLAG_USER = "-U";

    /**
     * Executes the task.
     * <p>
     * Builds a command line to execute ss and then calls Exec's run method
     * to execute the command line.
     */
    public void execute() throws BuildException {
        Commandline commandLine = new Commandline();
        int result = 0;

        // first off, make sure that we've got a command and a vssdir and a label ...
        if (getVsspath() == null) {
            String msg = "vsspath attribute must be set!";
            throw new BuildException(msg, location);
        }

        // now look for illegal combinations of things ...

        // build the command line from what we got the format is
        // ss History elements [-H] [-L] [-N] [-O] [-V] [-Y] [-#] [-?]
        // as specified in the SS.EXE help
        commandLine.setExecutable(getSSCommand());
        commandLine.createArgument().setValue(COMMAND_HISTORY);

        // VSS items
        commandLine.createArgument().setValue(getVsspath());

        // -I-
        commandLine.createArgument().setValue("-I-");  // ignore all errors

        // -V
        // Label an existing file or project version
        getVersionDateCommand(commandLine);
        getVersionLabelCommand(commandLine);

        // -R   
        if (m_Recursive) {
            commandLine.createArgument().setValue(FLAG_RECURSION);
        }

        // -B / -D / -F-
        if (m_Style.length() > 0) {
            commandLine.createArgument().setValue(m_Style);
        }

        // -Y
        getLoginCommand(commandLine);
                
        // -O
        getOutputCommand(commandLine);

        System.out.println("***: " + commandLine);
        
        result = run(commandLine);
        if (result != 0) {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(msg, location);
        }

    }

    /**
     * Set the Start Date for the Comparison of two versions; optional.
     */
    public void setFromDate(String fromDate) {
        if (fromDate.equals("") || fromDate == null) {
            m_FromDate = null;
        } else {
            m_FromDate = fromDate;
        }
    }

    /**
     * Set the Start Label; optional
     */
    public void setFromLabel(String fromLabel) {
        if (fromLabel.equals("") || fromLabel == null) {
            m_FromLabel = null;
        } else {
            m_FromLabel = fromLabel;
        }
    }

    /**
     * Set the End Label ; optional
     */
    public void setToLabel(String toLabel) {
        if (toLabel.equals("") || toLabel == null) {
            m_ToLabel = null;
        } else {
            m_ToLabel = toLabel;
        }
    }

    /**
     * Set the End Date for the Comparison of two versions; optional.
     */
    public void setToDate(String toDate) {
        if (toDate.equals("") || toDate == null) {
            m_ToDate = null;
        } else {
            m_ToDate = toDate;
        }
    }

    /**
     * Set the number of days for comparison; 
     * optional.
     * <p>
     * The default value is 2 days. (maybe)
     */
    public void setNumdays(int numd) {
        m_NumDays = numd;
    }
    
    /**
     * Set the output file name for the history; optional.
     */
    public void setOutput(File outfile) {
        if (outfile == null) {
            m_OutputFileName = null;
        } else {
            m_OutputFileName = outfile.getAbsolutePath();
        }
    }

    /**
     * Format of dates in fromDate and toDate; optional.
     * Used when calculating dates with 
     * the numdays attribute. 
     * This string uses the formatting rules of SimpleDateFormat. 
     *  Defaults to DateFormat.SHORT.
     */
    public void setDateFormat(String dateFormat) {
        if (!(dateFormat.equals("") || dateFormat == null)) {
            m_DateFormat = new SimpleDateFormat(dateFormat);
        }
    }

    /**
     * Builds the version date command.
     * @param cmd the commandline the command is to be added to
     */
    private void getVersionDateCommand(Commandline cmd) throws BuildException {
        if (m_FromDate == null && m_ToDate == null 
            && m_NumDays == Integer.MIN_VALUE) {
            return;
        }
        
        if (m_FromDate != null && m_ToDate != null) {
            cmd.createArgument().setValue(FLAG_VERSION_DATE + m_ToDate 
                + VALUE_FROMDATE + m_FromDate);
        } else if (m_ToDate != null && m_NumDays != Integer.MIN_VALUE) {
            String startDate = null;
            try {
                startDate = calcDate(m_ToDate, m_NumDays);
            } catch (ParseException ex) {
                String msg = "Error parsing date: " + m_ToDate;
                throw new BuildException(msg, location);
            }
            cmd.createArgument().setValue(FLAG_VERSION_DATE + m_ToDate + VALUE_FROMDATE + startDate);
        } else if (m_FromDate != null && m_NumDays != Integer.MIN_VALUE) {
            String endDate = null;
            try {
                endDate = calcDate(m_FromDate, m_NumDays);
            } catch (ParseException ex) {
                String msg = "Error parsing date: " + m_FromDate;
                throw new BuildException(msg, location);
            }
            cmd.createArgument().setValue(FLAG_VERSION_DATE + endDate + VALUE_FROMDATE + m_FromDate);
        } else {
            if (m_FromDate != null) {
                cmd.createArgument().setValue(FLAG_VERSION + VALUE_FROMDATE + m_FromDate);
            } else {
                cmd.createArgument().setValue(FLAG_VERSION_DATE + m_ToDate);
            }
        }
    }

    /**
     * Builds the version date command.
     * @param cmd the commandline the command is to be added to
     */
    private void getVersionLabelCommand(Commandline cmd) throws BuildException {
        if (m_FromLabel == null && m_ToLabel == null) {
            return;
        }
        
        if (m_FromLabel != null && m_ToLabel != null) {
            cmd.createArgument().setValue(FLAG_VERSION_LABEL + m_ToLabel + VALUE_FROMLABEL + m_FromLabel);
        } else if (m_FromLabel != null) {
            cmd.createArgument().setValue(FLAG_VERSION + VALUE_FROMLABEL + m_FromLabel);
        } else {
            cmd.createArgument().setValue(FLAG_VERSION_LABEL + m_ToLabel);
        }
    }
    
    /**
     * Builds the version date command.
     * @param cmd the commandline the command is to be added to
     */
    private void getOutputCommand(Commandline cmd) {
        if (m_OutputFileName != null) {
            cmd.createArgument().setValue(FLAG_OUTPUT + m_OutputFileName);
        }
    }

    /**
     * Builds the User command.
     * @param cmd the commandline the command is to be added to
     */
    private void getUserCommand(Commandline cmd) {
        if (m_User != null) {
            cmd.createArgument().setValue(FLAG_USER + m_User);
        }
    }

     /**
     * Calculates the start date for version comparison.
     * <p>
     * Calculates the date numDay days earlier than startdate.
     */
    private String calcDate(String fromDate, int numDays) throws ParseException {
        String toDate = null;
        Date currdate = new Date();
        Calendar calend = new GregorianCalendar();
        currdate = m_DateFormat.parse(fromDate); 
        calend.setTime(currdate);
        calend.add(Calendar.DATE, numDays);
        toDate = m_DateFormat.format(calend.getTime());
        return toDate;
    }

    /**
     * Flag to tell the task to recurse down the tree;
     * optional, default false.
     */

    public void setRecursive(boolean recursive) {
        m_Recursive = recursive;
    }

    /**
     * Name the user whose changes we would like to see; optional
     */
    public void setUser(String user) {
        m_User = user;
    }

    /**
     * @return the 'recursive' command if the attribute was 'true', otherwise an empty string
     */
    private void getRecursiveCommand(Commandline cmd) {
        if (!m_Recursive) {
            return;
        } else {
            cmd.createArgument().setValue(FLAG_RECURSION);
        }
    }

    /**
     * Specify the output style; optional.
     *
     * @param option valid values:
     * <ul>
     * <li>brief:    -B Display a brief history. 
     * <li>codediff: -D Display line-by-line file changes. 
     * <li>nofile:   -F- Do not display individual file updates in the project history. 
     * <li>default:  No option specified. Display in Source Safe's default format.
     * </ul>
     */
    public void setStyle(BriefCodediffNofile attr) {
        String option = attr.getValue();
        if (option.equals("brief")) {
            m_Style = "-B";
        } else if (option.equals("codediff")) {
            m_Style = "-D";
        } else if (option.equals("default")) {
            m_Style = "";
        } else if (option.equals("nofile")) {
            m_Style = "-F-";
        } else {
            throw new BuildException("Style " + attr + " unknown.");
        }
    }

    public static class BriefCodediffNofile extends EnumeratedAttribute {
       public String[] getValues() {
           return new String[] {"brief", "codediff", "nofile", "default"};
       }
   }
}
