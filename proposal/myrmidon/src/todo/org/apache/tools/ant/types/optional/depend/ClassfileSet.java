/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.types.optional.depend;

import java.util.ArrayList;
import java.util.List;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

/**
 * A DepSet is a FileSet, that enlists all classes that depend on a certain
 * class. A DependSet extends FileSets and uses another FileSet as input. The
 * nested FileSet attribute provides the domain, that is used for searching for
 * dependent classes
 *
 * @author <a href="mailto:hengels@innovidata.com">Holger Engels</a>
 */
public class ClassfileSet extends FileSet
{
    private List rootClasses = new ArrayList();

    protected ClassfileSet( ClassfileSet s )
    {
        super( s );
        rootClasses = s.rootClasses;
    }

    public void setRootClass( String rootClass )
        throws TaskException
    {
        rootClasses.add( rootClass );
    }

    /**
     * Return the DirectoryScanner associated with this FileSet.
     *
     * @param p Description of Parameter
     * @return The DirectoryScanner value
     */
    public DirectoryScanner getDirectoryScanner( Project p )
    {
        DependScanner scanner = new DependScanner();
        scanner.setBasedir( getDir( p ) );
        scanner.setRootClasses( rootClasses );
        scanner.scan();
        return scanner;
    }

    public void addConfiguredRoot( ClassRoot root )
    {
        rootClasses.add( root.getClassname() );
    }

    public Object clone()
    {
        if( isReference() )
        {
            return new ClassfileSet( (ClassfileSet)getRef( getProject() ) );
        }
        else
        {
            return new ClassfileSet( this );
        }
    }

    public static class ClassRoot
    {
        private String rootClass;

        public void setClassname( String name )
        {
            this.rootClass = name;
        }

        public String getClassname()
        {
            return rootClass;
        }
    }
}
