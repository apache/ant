/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.build;

import com.sun.javadoc.ClassDoc;
import java.io.File;
import java.net.URL;
import xdoclet.TemplateSubTask;
import xdoclet.XDocletException;

/**
 * Generates the XML Documentation for Ant types (including tasks and DataTypes).
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class AntDocSubTask 
    extends TemplateSubTask
{
    public final static String SUBTASK_NAME = "antdoc";

    private static final String GENERATED_FILE_NAME = "{0}.xml";
    private static final String DEFAULT_TEMPLATE_FILE = 
        "/org/apache/myrmidon/build/type.j";

    private File m_docsDestDir;

    public AntDocSubTask()
    {
        setTemplateFile( new File( DEFAULT_TEMPLATE_FILE ) );
        setDestinationFile( GENERATED_FILE_NAME );

        final TemplateSubTask.ExtentTypes extent = new TemplateSubTask.ExtentTypes();
        extent.setValue( "hierarchy" );
        setExtent( extent );
    }

    /**
     * Specifies the directory that is the destination of generated generated 
     * xml documentation for types.
     */
    public void setDocsDestDir( final File docsDestDir )
    {
        m_docsDestDir = docsDestDir;
    }

    public String getSubTaskName()
    {
        return SUBTASK_NAME;
    }

    /**
     * Called to validate configuration parameters.
     */
    public void validateOptions() 
        throws XDocletException
    {
        super.validateOptions();

        if( null == m_docsDestDir )
        {
            throw new XDocletException( "'docsDestDir' attribute is missing ." );
        }
    }

    protected boolean matchesGenerationRules( final ClassDoc clazz )
        throws XDocletException
    {
        if( !super.matchesGenerationRules( clazz ) )
        {
            return false;
        }
        else if( clazz.isAbstract() )
        {
            return false;
        }
        else
        {
            return true;
        }
    }
}
