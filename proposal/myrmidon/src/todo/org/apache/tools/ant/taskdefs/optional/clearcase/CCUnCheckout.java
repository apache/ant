/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.clearcase;

import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.Commandline;

/**
 * Task to perform UnCheckout command to ClearCase. <p>
 *
 * The following attributes are interpretted:
 * <tableborder="1">
 *
 *   <tr>
 *
 *     <th>
 *       Attribute
 *     </th>
 *
 *     <th>
 *       Values
 *     </th>
 *
 *     <th>
 *       Required
 *     </th>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       viewpath
 *     </td>
 *
 *     <td>
 *       Path to the ClearCase view file or directory that the command will
 *       operate on
 *     </td>
 *
 *     <td>
 *       No
 *     </td>
 *
 *     <tr>
 *
 *       <tr>
 *
 *         <td>
 *           keepcopy
 *         </td>
 *
 *         <td>
 *           Specifies whether to keep a copy of the file with a .keep extension
 *           or not
 *         </td>
 *
 *         <td>
 *           No
 *         </td>
 *
 *         <tr>
 *
 *         </table>
 *
 *
 * @author Curtis White
 */
public class CCUnCheckout extends ClearCase
{

    /**
     * -keep flag -- keep a copy of the file with .keep extension
     */
    public final static String FLAG_KEEPCOPY = "-keep";
    /**
     * -rm flag -- remove the copy of the file
     */
    public final static String FLAG_RM = "-rm";
    private boolean m_Keep = false;

    /**
     * Set keepcopy flag status
     *
     * @param keep the status to set the flag to
     */
    public void setKeepCopy( boolean keep )
    {
        m_Keep = keep;
    }

    /**
     * Get keepcopy flag status
     *
     * @return boolean containing status of keep flag
     */
    public boolean getKeepCopy()
    {
        return m_Keep;
    }

    /**
     * Executes the task. <p>
     *
     * Builds a command line to execute cleartool and then calls Exec's run
     * method to execute the command line.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        Commandline commandLine = new Commandline();

        // Default the viewpath to basedir if it is not specified
        if( getViewPath() == null )
        {
            setViewPath( getBaseDirectory().getPath() );
        }

        // build the command line from what we got the format is
        // cleartool uncheckout [options...] [viewpath ...]
        // as specified in the CLEARTOOL.EXE help
        commandLine.setExecutable( getClearToolCommand() );
        commandLine.addArgument( COMMAND_UNCHECKOUT );

        checkOptions( commandLine );

        final int result = run( commandLine );
        if( result != 0 )
        {
            final String message = "Failed executing: " + commandLine.toString();
            throw new TaskException( message );
        }
    }

    /**
     * Check the command line options.
     *
     * @param cmd Description of Parameter
     */
    private void checkOptions( Commandline cmd )
    {
        // ClearCase items
        if( getKeepCopy() )
        {
            // -keep
            cmd.addArgument( FLAG_KEEPCOPY );
        }
        else
        {
            // -rm
            cmd.addArgument( FLAG_RM );
        }

        // viewpath
        cmd.addArgument( getViewPath() );
    }

}

