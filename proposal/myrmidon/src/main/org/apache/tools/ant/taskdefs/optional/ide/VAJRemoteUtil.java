/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.ide;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Task;

/**
 * Helper class for VAJ tasks. Holds Workspace singleton and wraps IvjExceptions
 * into TaskExceptions
 *
 * @author Wolf Siberski, TUI Infotec GmbH
 */
class VAJRemoteUtil implements VAJUtil
{
    // calling task
    Task caller;

    // VAJ remote tool server
    String remoteServer;

    public VAJRemoteUtil( Task caller, String remote )
    {
        this.caller = caller;
        this.remoteServer = remote;
    }

    /**
     * export the array of Packages
     *
     * @param destDir Description of Parameter
     * @param includePatterns Description of Parameter
     * @param excludePatterns Description of Parameter
     * @param exportClasses Description of Parameter
     * @param exportDebugInfo Description of Parameter
     * @param exportResources Description of Parameter
     * @param exportSources Description of Parameter
     * @param useDefaultExcludes Description of Parameter
     * @param overwrite Description of Parameter
     */
    public void exportPackages( File destDir,
                                String[] includePatterns, String[] excludePatterns,
                                boolean exportClasses, boolean exportDebugInfo, boolean exportResources,
                                boolean exportSources, boolean useDefaultExcludes, boolean overwrite )
    {
        try
        {
            String request = "http://" + remoteServer + "/servlet/vajexport?"
                + VAJExportServlet.WITH_DEBUG_INFO + "=" + exportDebugInfo + "&"
                + VAJExportServlet.OVERWRITE_PARAM + "=" + overwrite + "&"
                + assembleImportExportParams( destDir,
                                              includePatterns, excludePatterns,
                                              exportClasses, exportResources,
                                              exportSources, useDefaultExcludes );
            sendRequest( request );
        }
        catch( Exception ex )
        {
            throw new TaskException( "Error", ex );
        }
    }

    /**
     * Do the import.
     *
     * @param importProject Description of Parameter
     * @param srcDir Description of Parameter
     * @param includePatterns Description of Parameter
     * @param excludePatterns Description of Parameter
     * @param importClasses Description of Parameter
     * @param importResources Description of Parameter
     * @param importSources Description of Parameter
     * @param useDefaultExcludes Description of Parameter
     */
    public void importFiles(
        String importProject, File srcDir,
        String[] includePatterns, String[] excludePatterns,
        boolean importClasses, boolean importResources,
        boolean importSources, boolean useDefaultExcludes )
    {
        try
        {
            String request = "http://" + remoteServer + "/servlet/vajimport?"
                + VAJImportServlet.PROJECT_NAME_PARAM + "="
                + importProject + "&"
                + assembleImportExportParams( srcDir,
                                              includePatterns, excludePatterns,
                                              importClasses, importResources,
                                              importSources, useDefaultExcludes );
            sendRequest( request );
        }
        catch( Exception ex )
        {
            throw new TaskException( "Error", ex );
        }

    }

    /**
     * Load specified projects.
     *
     * @param projectDescriptions Description of Parameter
     */
    public void loadProjects( ArrayList projectDescriptions )
    {
        try
        {
            String request = "http://" + remoteServer + "/servlet/vajload?";
            String delimiter = "";
            for( Iterator e = projectDescriptions.iterator(); e.hasNext(); )
            {
                VAJProjectDescription pd = (VAJProjectDescription)e.next();
                request = request
                    + delimiter + VAJLoadServlet.PROJECT_NAME_PARAM
                    + "=" + pd.getName().replace( ' ', '+' )
                    + "&" + VAJLoadServlet.VERSION_PARAM
                    + "=" + pd.getVersion().replace( ' ', '+' );
                //the first param needs no delimiter, but all other
                delimiter = "&";
            }
            sendRequest( request );
        }
        catch( Exception ex )
        {
            throw new TaskException( "Error", ex );
        }
    }

    /**
     * logs a message.
     *
     * @param msg Description of Parameter
     * @param level Description of Parameter
     */
    public void log( String msg, int level )
    {
        caller.log( msg, level );
    }

    /**
     * Assemble string for parameters common for import and export Helper method
     * to remove double code.
     *
     * @param dir Description of Parameter
     * @param includePatterns Description of Parameter
     * @param excludePatterns Description of Parameter
     * @param includeClasses Description of Parameter
     * @param includeResources Description of Parameter
     * @param includeSources Description of Parameter
     * @param useDefaultExcludes Description of Parameter
     * @return Description of the Returned Value
     */
    private String assembleImportExportParams(
        File dir,
        String[] includePatterns, String[] excludePatterns,
        boolean includeClasses, boolean includeResources,
        boolean includeSources, boolean useDefaultExcludes )
    {
        String result =
            VAJToolsServlet.DIR_PARAM + "="
            + dir.getAbsolutePath().replace( '\\', '/' ) + "&"
            + VAJToolsServlet.CLASSES_PARAM + "=" + includeClasses + "&"
            + VAJToolsServlet.RESOURCES_PARAM + "=" + includeResources + "&"
            + VAJToolsServlet.SOURCES_PARAM + "=" + includeSources + "&"
            + VAJToolsServlet.DEFAULT_EXCLUDES_PARAM + "=" + useDefaultExcludes;

        if( includePatterns != null )
        {
            for( int i = 0; i < includePatterns.length; i++ )
            {
                result = result + "&" + VAJExportServlet.INCLUDE_PARAM + "="
                    + includePatterns[ i ].replace( ' ', '+' ).replace( '\\', '/' );
            }
        }
        if( excludePatterns != null )
        {
            for( int i = 0; i < excludePatterns.length; i++ )
            {
                result = result + "&" + VAJExportServlet.EXCLUDE_PARAM + "="
                    + excludePatterns[ i ].replace( ' ', '+' ).replace( '\\', '/' );
            }
        }

        return result;
    }

    /**
     * Sends a servlet request.
     *
     * @param request Description of Parameter
     */
    private void sendRequest( String request )
    {
        boolean requestFailed = false;
        try
        {
            log( "Request: " + request, MSG_DEBUG );

            //must be HTTP connection
            URL requestUrl = new URL( request );
            HttpURLConnection connection =
                (HttpURLConnection)requestUrl.openConnection();

            InputStream is = null;
            // retry three times
            for( int i = 0; i < 3; i++ )
            {
                try
                {
                    is = connection.getInputStream();
                    break;
                }
                catch( IOException ex )
                {
                }
            }
            if( is == null )
            {
                log( "Can't get " + request, MSG_ERR );
                throw new TaskException( "Couldn't execute " + request );
            }

            // log the response
            BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
            String line = br.readLine();
            while( line != null )
            {
                int level = MSG_ERR;
                try
                {
                    // the first char of each line contains the log level
                    level = Integer.parseInt( line.substring( 0, 1 ) );
                    if( level == MSG_ERR )
                    {
                        requestFailed = true;
                    }
                }
                catch( Exception e )
                {
                    log( "Response line doesn't contain log level!", MSG_ERR );
                }
                log( line.substring( 2 ), level );
                line = br.readLine();
            }

        }
        catch( IOException ex )
        {
            log( "Error sending tool request to VAJ" + ex, MSG_ERR );
            throw new TaskException( "Couldn't execute " + request );
        }
        if( requestFailed )
        {
            throw new TaskException( "VAJ tool request failed" );
        }
    }
}
