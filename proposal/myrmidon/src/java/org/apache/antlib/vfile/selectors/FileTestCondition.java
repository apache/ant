/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.vfile.selectors;

import org.apache.myrmidon.framework.Condition;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.aut.vfs.FileObject;
import org.apache.antlib.vfile.FileSelector;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * A condition that applies a set of file selectors to a file.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant:type type="condition" name="file-test"
 */
public class FileTestCondition
    implements Condition
{
    private final static Resources REZ
        = ResourceManager.getPackageResources( FileTestCondition.class );

    private FileObject m_file;
    private AndFileSelector m_selector = new AndFileSelector();

    /**
     * Sets the file to test.
     */
    public void setFile( final FileObject file )
    {
        m_file = file;
    }

    /**
     * Adds a selector.
     */
    public void add( final FileSelector selector )
    {
        m_selector.add( selector );
    }

    /**
     * Evaluates this condition.
     */
    public boolean evaluate( final TaskContext context )
        throws TaskException
    {
        if( m_file == null )
        {
            final String message = REZ.getString( "filetestcondition.no-file.error" );
            throw new TaskException( message );
        }
        return m_selector.accept( m_file, null, context );
    }
}
