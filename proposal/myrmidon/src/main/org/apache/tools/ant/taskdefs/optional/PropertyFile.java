/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;

/**
 * PropertyFile task uses java.util.Properties to modify integer, String and
 * Date settings in a property file.<p>
 *
 * The following is an example of its usage:
 * <ul>&lt;target name="setState"&gt;<br>
 *
 *   <ul>&lt;property<br>
 *
 *     <ul>name="header"<br>
 *       value="##Generated file - do not modify!"/&gt;<br>
 *       &lt;propertyfile file="apropfile.properties" comment="${header}"&gt;
 *       <br>
 *       &lt;entry key="product.version.major" type="int" value="5"/&gt;<br>
 *       &lt;entry key="product.version.minor" type="int" value="0"/&gt;<br>
 *       &lt;entry key="product.build.major" type="int" value="0" /&gt;<br>
 *       &lt;entry key="product.build.minor" type="int" operation="+" /&gt;<br>
 *       &lt;entry key="product.build.date" type="date" operation="now" /&gt;
 *       <br>
 *       &lt;entry key="intSet" type="int" operation="=" value="681"/&gt;<br>
 *       &lt;entry key="intDec" type="int" operation="-"/&gt;<br>
 *       &lt;entry key="NeverDate" type="date" operation="never"/&gt;<br>
 *       &lt;entry key="StringEquals" type="string" value="testValue"/&gt;<br>
 *       &lt;entry key="NowDate" type="date" operation="now"/&gt;<br>
 *
 *     </ul>
 *     &lt;/propertyfile&gt;<br>
 *
 *   </ul>
 *   &lt;/target&gt;
 * </ul>
 * <p>
 *
 * The &lt;propertyfile&gt; task must have:<br>
 *
 * <ul>
 *   <li> file</li>
 * </ul>
 * Other parameters are:<br>
 *
 * <ul>
 *   <li> comment, key, operation, type and value (the final four being
 *   eliminated shortly)</li>
 * </ul>
 * The &lt;entry&gt; task must have:<br>
 *
 * <ul>
 *   <li> key</li>
 * </ul>
 * Other parameters are:<br>
 *
 * <ul>
 *   <li> operation</li>
 *   <li> type</li>
 *   <li> value</li>
 *   <li> offset</li>
 * </ul>
 * If type is unspecified, it defaults to string Parameter values:<br>
 *
 * <ul>
 *   <li> operation:</li>
 *   <ul>
 *     <li> "=" (set -- default)</li>
 *     <li> "-" (dec)</li>
 *     <li> "+" (inc)</li>
 *     <li> type:</li>
 *     <ul>
 *       <li> "int"</li>
 *       <li> "date"</li>
 *       <li> "string"</li>
 *     </ul>
 *
 *   </ul>
 *
 *   <li> value:</li>
 *   <ul>
 *     <li> holds the default value, if the property was not found in property
 *     file</li>
 *     <li> "now" In case of type "date", the value "now" will be replaced by
 *     the current date/time and used even if a valid date was found in the
 *     property file.</li>
 *   </ul>
 *
 *   <li> offset:<br>
 *   valid for "-" or "+", the offset (default set to 1) will be added or
 *   subtracted from "int" or "date" type value.</li>
 * </ul>
 * String property types can only use the "=" operation. Date property types can
 * only use the "never" or "now" operations. Int property types can only use the
 * "=", "-" or "+" operations.<p>
 *
 * The message property is used for the property file header, with "\\" being a
 * newline delimiter charater.
 *
 * @author Thomas Christen <a href="mailto:chr@active.ch">chr@active.ch</a>
 * @author Jeremy Mawson <a href="mailto:jem@loftinspace.com.au">
 *      jem@loftinspace.com.au</a>
 */
public class PropertyFile
    extends AbstractTask
{
    private ArrayList m_entries = new ArrayList();

    // Use this to prepend a message to the properties file
    private String m_comment;

    private Properties m_properties;
    private File m_file;

    public void setComment( final String comment )
    {
        m_comment = comment;
    }

    public void setFile( final File file )
    {
        m_file = file;
    }

    public Entry createEntry()
    {
        final Entry entry = new Entry();
        m_entries.add( entry );
        return entry;
    }

    public void execute()
        throws TaskException
    {
        checkParameters();
        readFile();
        executeOperation();
        writeFile();
    }

    /*
     * Returns whether the given parameter has been defined.
     */
    private boolean checkParam( String param )
    {
        return !( ( param == null ) || ( param.equals( "null" ) ) );
    }

    private boolean checkParam( File param )
    {
        return !( param == null );
    }

    private void checkParameters()
        throws TaskException
    {
        if( !checkParam( m_file ) )
        {
            throw new TaskException( "file token must not be null." );
        }
    }

    private void executeOperation()
        throws TaskException
    {
        for( Iterator e = m_entries.iterator(); e.hasNext(); )
        {
            Entry entry = (Entry)e.next();
            entry.executeOn( m_properties );
        }
    }

    private void readFile()
        throws TaskException
    {
        // Create the PropertyFile
        m_properties = new Properties();
        try
        {
            if( m_file.exists() )
            {
                getLogger().info( "Updating property file: " + m_file.getAbsolutePath() );
                FileInputStream fis = null;
                try
                {
                    fis = new FileInputStream( m_file );
                    BufferedInputStream bis = new BufferedInputStream( fis );
                    m_properties.load( bis );
                }
                finally
                {
                    if( fis != null )
                    {
                        fis.close();
                    }
                }
            }
            else
            {
                getLogger().info( "Creating new property file: " +
                                  m_file.getAbsolutePath() );
                FileOutputStream out = null;
                try
                {
                    out = new FileOutputStream( m_file.getAbsolutePath() );
                    out.flush();
                }
                finally
                {
                    if( out != null )
                    {
                        out.close();
                    }
                }
            }
        }
        catch( IOException ioe )
        {
            throw new TaskException( ioe.toString() );
        }
    }

    private void writeFile()
        throws TaskException
    {
        BufferedOutputStream bos = null;
        try
        {
            bos = new BufferedOutputStream( new FileOutputStream( m_file ) );

            // Properties.store is not available in JDK 1.1
            Method m =
                Properties.class.getMethod( "store",
                                            new Class[]{
                                                OutputStream.class,
                                                String.class}
                );
            m.invoke( m_properties, new Object[]{bos, m_comment} );

        }
        catch( NoSuchMethodException nsme )
        {
            m_properties.save( bos, m_comment );
        }
        catch( InvocationTargetException ite )
        {
            Throwable t = ite.getTargetException();
            throw new TaskException( "Error", t );
        }
        catch( IllegalAccessException iae )
        {
            // impossible
            throw new TaskException( "Error", iae );
        }
        catch( IOException ioe )
        {
            throw new TaskException( "Error", ioe );
        }
        finally
        {
            if( bos != null )
            {
                try
                {
                    bos.close();
                }
                catch( IOException ioex )
                {
                }
            }
        }
    }
}
