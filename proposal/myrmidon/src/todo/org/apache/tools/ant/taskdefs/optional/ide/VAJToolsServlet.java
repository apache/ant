/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.ide;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.StringUtils;

/**
 * Abstract base class to provide common services for the VAJ tool API servlets
 *
 * @author Wolf Siberski, based on servlets written by Glenn McAllister
 */
public abstract class VAJToolsServlet extends HttpServlet
{

    // constants for servlet param names
    public final static String DIR_PARAM = "dir";
    public final static String INCLUDE_PARAM = "include";
    public final static String EXCLUDE_PARAM = "exclude";
    public final static String CLASSES_PARAM = "cls";
    public final static String SOURCES_PARAM = "src";
    public final static String RESOURCES_PARAM = "res";
    public final static String DEFAULT_EXCLUDES_PARAM = "dex";
    public final static String PROJECT_NAME_PARAM = "project";

    // current request
    HttpServletRequest request;

    // response to current request
    HttpServletResponse response;

    // implementation of VAJUtil used by the servlet
    VAJUtil util;

    /**
     * Respond to a HTTP request. This method initializes the servlet and
     * handles errors. The real work is done in the abstract method
     * executeRequest()
     *
     * @param req Description of Parameter
     * @param res Description of Parameter
     * @exception ServletException Description of Exception
     * @exception IOException Description of Exception
     */
    public void doGet( HttpServletRequest req, HttpServletResponse res )
        throws ServletException, IOException
    {
        try
        {
            response = res;
            request = req;
            initRequest();
            executeRequest();
        }
        catch( BuildException e )
        {
            util.log( "Error occured: " + e.getMessage(), VAJUtil.MSG_ERR );
        }
        catch( Exception e )
        {
            try
            {
                if( !( e instanceof BuildException ) )
                {
                    String trace = StringUtils.getStackTrace( e );
                    util.log( "Program error in " + this.getClass().getName()
                         + ":\n" + trace, VAJUtil.MSG_ERR );
                }
            }
            catch( Throwable t )
            {
                t.printStackTrace();
            }
            finally
            {
                if( !( e instanceof BuildException ) )
                {
                    throw new ServletException( e.getMessage() );
                }
            }
        }
    }

    /**
     * Get the boolean value of a parameter.
     *
     * @param param Description of Parameter
     * @return The BooleanParam value
     */
    protected boolean getBooleanParam( String param )
    {
        return getBooleanParam( param, false );
    }

    /**
     * Get the boolean value of a parameter, with a default value if the
     * parameter hasn't been passed to the servlet.
     *
     * @param param Description of Parameter
     * @param defaultValue Description of Parameter
     * @return The BooleanParam value
     */
    protected boolean getBooleanParam( String param, boolean defaultValue )
    {
        String value = getFirstParamValueString( param );
        if( value != null )
        {
            return toBoolean( value );
        }
        else
        {
            return defaultValue;
        }
    }

    /**
     * Returns the first encountered value for a parameter.
     *
     * @param param Description of Parameter
     * @return The FirstParamValueString value
     */
    protected String getFirstParamValueString( String param )
    {
        String[] paramValuesArray = request.getParameterValues( param );
        if( paramValuesArray == null )
        {
            return null;
        }
        return paramValuesArray[0];
    }

    /**
     * Returns all values for a parameter.
     *
     * @param param Description of Parameter
     * @return The ParamValues value
     */
    protected String[] getParamValues( String param )
    {
        return request.getParameterValues( param );
    }


    /**
     * Execute the request by calling the appropriate VAJ tool API methods. This
     * method must be implemented by the concrete servlets
     */
    protected abstract void executeRequest();

    /**
     * initialize the servlet.
     *
     * @exception IOException Description of Exception
     */
    protected void initRequest()
        throws IOException
    {
        response.setContentType( "text/ascii" );
        if( util == null )
        {
            util = new VAJLocalServletUtil();
        }
    }

    /**
     * A utility method to translate the strings "yes", "true", and "ok" to
     * boolean true, and everything else to false.
     *
     * @param string Description of Parameter
     * @return Description of the Returned Value
     */
    protected boolean toBoolean( String string )
    {
        String lower = string.toLowerCase();
        return ( lower.equals( "yes" ) || lower.equals( "true" ) || lower.equals( "ok" ) );
    }

    /**
     * Get the VAJUtil implementation
     *
     * @return The Util value
     */
    VAJUtil getUtil()
    {
        return util;
    }

    /**
     * Adaptation of VAJUtil for servlet context.
     *
     * @author RT
     */
    class VAJLocalServletUtil extends VAJLocalUtil
    {
        public void log( String msg, int level )
        {
            try
            {
                if( msg != null )
                {
                    msg = msg.replace( '\r', ' ' );
                    int i = 0;
                    while( i < msg.length() )
                    {
                        int nlPos = msg.indexOf( '\n', i );
                        if( nlPos == -1 )
                        {
                            nlPos = msg.length();
                        }
                        response.getWriter().println( Integer.toString( level )
                             + " " + msg.substring( i, nlPos ) );
                        i = nlPos + 1;
                    }
                }
            }
            catch( IOException e )
            {
                throw new BuildException( "logging failed. msg was: "
                     + e.getMessage() );
            }
        }
    }
}
