/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.junit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.todo.types.EnumeratedAttribute;

/**
 * <p>
 *
 * A wrapper for the implementations of <code>JUnitResultFormatter</code>. In
 * particular, used as a nested <code>&lt;formatter&gt;</code> element in a
 * <code>&lt;junit&gt;</code> task. <p>
 *
 * For example, <code><pre>
 *       &lt;junit printsummary="no" haltonfailure="yes" fork="false"&gt;
 *           &lt;formatter type="plain" usefile="false" /&gt;
 *           &lt;test name="org.apache.ecs.InternationalCharTest" /&gt;
 *       &lt;/junit&gt;</pre></code> adds a <code>plain</code> type
 * implementation (<code>PlainJUnitResultFormatter</code>) to display the
 * results of the test. <p>
 *
 * Either the <code>type</code> or the <code>classname</code> attribute must be
 * set.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @see JUnitTask
 * @see XMLJUnitResultFormatter
 * @see BriefJUnitResultFormatter
 * @see PlainJUnitResultFormatter
 * @see JUnitResultFormatter
 */
public class FormatterElement
{
    private OutputStream out = System.out;
    private boolean useFile = true;

    private String classname;
    private String extension;
    private File outFile;

    /**
     * <p>
     *
     * Set name of class to be used as the formatter. <p>
     *
     * This class must implement <code>JUnitResultFormatter</code>
     *
     * @param classname The new Classname value
     */
    public void setClassname( String classname )
    {
        this.classname = classname;
    }

    public void setExtension( String ext )
    {
        this.extension = ext;
    }

    /**
     * <p>
     *
     * Set output stream for formatter to use. <p>
     *
     * Defaults to standard out.
     *
     * @param out The new Output value
     */
    public void setOutput( OutputStream out )
    {
        this.out = out;
    }

    /**
     * <p>
     *
     * Quick way to use a standard formatter. <p>
     *
     * At the moment, there are three supported standard formatters.
     * <ul>
     *   <li> The <code>xml</code> type uses a <code>XMLJUnitResultFormatter</code>
     *   .
     *   <li> The <code>brief</code> type uses a <code>BriefJUnitResultFormatter</code>
     *   .
     *   <li> The <code>plain</code> type (the default) uses a <code>PlainJUnitResultFormatter</code>
     *   .
     * </ul>
     * <p>
     *
     * Sets <code>classname</code> attribute - so you can't use that attribute
     * if you use this one.
     *
     * @param type The new Type value
     */
    public void setType( TypeAttribute type )
    {
        if( "xml".equals( type.getValue() ) )
        {
            setClassname( "org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter" );
            setExtension( ".xml" );
        }
        else
        {
            if( "brief".equals( type.getValue() ) )
            {
                setClassname( "org.apache.tools.ant.taskdefs.optional.junit.BriefJUnitResultFormatter" );
            }
            else
            {// must be plain, ensured by TypeAttribute
                setClassname( "org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter" );
            }
            setExtension( ".txt" );
        }
    }

    /**
     * Set whether the formatter should log to file.
     *
     * @param useFile The new UseFile value
     */
    public void setUseFile( boolean useFile )
    {
        this.useFile = useFile;
    }

    /**
     * Get name of class to be used as the formatter.
     *
     * @return The Classname value
     */
    public String getClassname()
    {
        return classname;
    }

    public String getExtension()
    {
        return extension;
    }

    /**
     * <p>
     *
     * Set the file which the formatte should log to. <p>
     *
     * Note that logging to file must be enabled .
     *
     * @param out The new Outfile value
     */
    void setOutfile( File out )
    {
        this.outFile = out;
    }

    /**
     * Get whether the formatter should log to file.
     *
     * @return The UseFile value
     */
    boolean getUseFile()
    {
        return useFile;
    }

    JUnitResultFormatter createFormatter()
        throws TaskException
    {
        if( classname == null )
        {
            throw new TaskException( "you must specify type or classname" );
        }

        Class f = null;
        try
        {
            f = Class.forName( classname );
        }
        catch( ClassNotFoundException e )
        {
            throw new TaskException( "Error", e );
        }

        Object o = null;
        try
        {
            o = f.newInstance();
        }
        catch( InstantiationException e )
        {
            throw new TaskException( "Error", e );
        }
        catch( IllegalAccessException e )
        {
            throw new TaskException( "Error", e );
        }

        if( !( o instanceof JUnitResultFormatter ) )
        {
            throw new TaskException( classname + " is not a JUnitResultFormatter" );
        }

        JUnitResultFormatter r = (JUnitResultFormatter)o;

        if( useFile && outFile != null )
        {
            try
            {
                out = new FileOutputStream( outFile );
            }
            catch( java.io.IOException e )
            {
                throw new TaskException( "Error", e );
            }
        }
        r.setOutput( out );
        return r;
    }

    /**
     * <p>
     *
     * Enumerated attribute with the values "plain", "xml" and "brief". <p>
     *
     * Use to enumerate options for <code>type</code> attribute.
     *
     * @author RT
     */
    public static class TypeAttribute extends EnumeratedAttribute
    {
        public String[] getValues()
        {
            return new String[]{"plain", "xml", "brief"};
        }
    }
}
