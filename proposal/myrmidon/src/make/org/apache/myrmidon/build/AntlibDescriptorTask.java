/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.build;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.UpToDate;
import org.apache.tools.ant.types.FileSet;
import xdoclet.DocletTask;
import xdoclet.TemplateSubTask;

/**
 * A Task that generates Myrmidon Antlib descriptors from source files,
 * using the XDoclet engine and "@ant:" tags.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 */
public class AntlibDescriptorTask
    extends DocletTask
{
    private static final String DESCRIPTOR_TEMPLATE = "/org/apache/myrmidon/build/ant-descriptor.j";
    private static final String ROLES_TEMPLATE = "/org/apache/myrmidon/build/ant-roles.j";

    private TemplateSubTask m_antDocs;
    private String m_libName;
    private String m_descriptorFileName;
    private String m_rolesFileName;

    /**
     * Specifies the Antlib name, which is used to name the generated files.
     */
    public void setLibName( final String libName )
    {
        m_libName = libName;
    }

    /**
     * Specifies the name of the file for the antlib types descriptor (optional).
     */
    public void setDescriptorName( final String descriptorFileName )
    {
        m_descriptorFileName = descriptorFileName;
    }

    /**
     * Specifies the name of the file for the antlib roles descriptor (optional).
     */
    public void setRolesDescriptorName( final String rolesFileName )
    {
        m_rolesFileName = rolesFileName;
    }

    public void addAntdoc( final AntDocSubTask antDocs )
    {
        m_antDocs = antDocs;
    }

    public void execute() throws BuildException
    {
        // Add the base directories of all the filesets to the sourcepath
        final Vector filesets = getFilesets();
        for( int i = 0; i < filesets.size(); i++ )
        {
            final FileSet fileSet = (FileSet)filesets.elementAt(i );
            final File basedir = fileSet.getDir( project );
            createSourcepath().setLocation( basedir );
        }

        // Add template subtasks.
        final TemplateSubTask descriptorTemplate =
            makeTemplateSubTask( DESCRIPTOR_TEMPLATE, getDescriptorFileName() );
        addTemplate( descriptorTemplate );

        final TemplateSubTask rolesTemplate =
            makeTemplateSubTask( ROLES_TEMPLATE, getRolesFileName() );
        addTemplate( rolesTemplate );

        if( null != m_antDocs )
        {
            addTemplate( m_antDocs );
        }

        if( !upToDate() )
        {
            log( "Generating Antlib descriptors for: " + m_libName );
            super.execute();
        }
    }

    /**
     * Creates a TemplateSubTask for a given template, which is read in
     * as a resource.
     */
    private TemplateSubTask makeTemplateSubTask( final String templateLocation,
                                                 final String destinationFile )
    {
        final TemplateSubTask templateSubTask = new TemplateSubTask();
        final String templateFile =
            getClass().getResource( templateLocation ).getFile();
        templateSubTask.setTemplateFile( new File( templateFile ) );
        templateSubTask.setDestinationFile( destinationFile );
        return templateSubTask;
    }

    /**
     * Checks if the descriptor file is up-to-date.
     */
    private boolean upToDate()
    {
        // Use the UpToDate task to check if descriptors are up-to-date.
        final UpToDate uptodateTask = (UpToDate)project.createTask( "uptodate" );

        final File destFile = new File( getDestDir(), getDescriptorFileName() );
        uptodateTask.setTargetFile( destFile );

        final Iterator filesets = getFilesets().iterator();
        while( filesets.hasNext() )
        {
            final FileSet fileSet = (FileSet)filesets.next();
            uptodateTask.addSrcfiles( fileSet );
        }

        return uptodateTask.eval();
    }

    /**
     * Return the filename for the antlib type descriptor. If not specified,
     * the default filename is returned.
     */
    private String getDescriptorFileName()
    {

        if( m_descriptorFileName == null )
        {
            return m_libName + "-ant-descriptor.xml";
        }
        else
        {
            return m_descriptorFileName;
        }
    }

    /**
     * Return the filename for the antlib roles descriptor. If not specified,
     * the default filename is returned.
     */
    private String getRolesFileName()
    {
        if( m_rolesFileName == null )
        {
            return m_libName + "-ant-roles.xml";
        }
        else
        {
            return m_rolesFileName;
        }
    }

}
