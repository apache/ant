/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.perforce;

import java.io.File;
import java.util.ArrayList;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

/**
 * P4Add - add the specified files to perforce. <b>Example Usage:</b>
 * <tableborder="1">
 *
 *   <th>
 *     Function
 *   </th>
 *
 *   <th>
 *     Command
 *   </th>
 *
 *   <tr>
 *
 *     <td>
 *       Add files using P4USER, P4PORT and P4CLIENT settings specified
 *     </td>
 *
 *     <td>
 *       &lt;P4add <br>
 *       P4view="//projects/foo/main/source/..." <br>
 *       P4User="fbloggs" <br>
 *       P4Port="km01:1666" <br>
 *       P4Client="fbloggsclient"&gt;<br>
 *       &lt;fileset basedir="dir" includes="**&#47;*.java"&gt;<br>
 *       &lt;/p4add&gt;
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       Add files using P4USER, P4PORT and P4CLIENT settings defined in
 *       environment
 *     </td>
 *
 *     <td>
 *       &lt;P4add P4view="//projects/foo/main/source/..." /&gt;<br>
 *       &lt;fileset basedir="dir" includes="**&#47;*.java"&gt;<br>
 *       &lt;/p4add&gt;
 *     </td>
 *
 *   </tr>
 *
 *   <tr>
 *
 *     <td>
 *       Specify the length of command line arguments to pass to each invocation
 *       of p4
 *     </td>
 *
 *     <td>
 *       &lt;p4add Commandlength="450"&gt;
 *     </td>
 *
 *   </tr>
 *
 * </table>
 *
 *
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 * @author <A HREF="mailto:ashundi@tibco.com">Anli Shundi</A>
 */
public class P4Add extends P4Base
{
    private String addCmd = "";
    private ArrayList filesets = new ArrayList();
    private int m_cmdLength = 450;

    private int m_changelist;

    public void setChangelist( int changelist )
        throws TaskException
    {
        if( changelist <= 0 )
            throw new TaskException( "P4Add: Changelist# should be a positive number" );

        this.m_changelist = changelist;
    }

    public void setCommandlength( int len )
        throws TaskException
    {
        if( len <= 0 )
            throw new TaskException( "P4Add: Commandlength should be a positive number" );
        this.m_cmdLength = len;
    }

    public void addFileset( FileSet set )
    {
        filesets.add( set );
    }

    public void execute()
        throws TaskException
    {

        if( P4View != null )
        {
            addCmd = P4View;
        }

        P4CmdOpts = ( m_changelist > 0 ) ? ( "-c " + m_changelist ) : "";

        StringBuffer filelist = new StringBuffer();

        for( int i = 0; i < filesets.size(); i++ )
        {
            FileSet fs = (FileSet)filesets.get( i );
            DirectoryScanner ds = fs.getDirectoryScanner( getProject() );
            //File fromDir = fs.getDir(project);

            String[] srcFiles = ds.getIncludedFiles();
            if( srcFiles != null )
            {
                for( int j = 0; j < srcFiles.length; j++ )
                {
                    File f = new File( ds.getBasedir(), srcFiles[ j ] );
                    filelist.append( " " ).append( '"' ).append( f.getAbsolutePath() ).append( '"' );
                    if( filelist.length() > m_cmdLength )
                    {
                        execP4Add( filelist );
                        filelist.setLength( 0 );
                    }
                }
                if( filelist.length() > 0 )
                {
                    execP4Add( filelist );
                }
            }
            else
            {
                log( "No files specified to add!", Project.MSG_WARN );
            }
        }

    }

    private void execP4Add( StringBuffer list )
    {
        log( "Execing add " + P4CmdOpts + " " + addCmd + list, Project.MSG_INFO );

        execP4Command( "-s add " + P4CmdOpts + " " + addCmd + list, new SimpleP4OutputHandler( this ) );
    }
}
