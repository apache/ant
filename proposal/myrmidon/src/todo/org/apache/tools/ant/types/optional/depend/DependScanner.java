/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.types.optional.depend;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.util.depend.Dependencies;
import org.apache.tools.ant.util.depend.Filter;

/**
 * An interface used to describe the actions required by any type of directory
 * scanner.
 *
 * @author RT
 */
public class DependScanner extends DirectoryScanner
{
    List included = new LinkedList();
    File baseClass;
    File basedir;

    private List rootClasses;

    /**
     * Sets the basedir for scanning. This is the directory that is scanned
     * recursively.
     *
     * @param basedir the (non-null) basedir for scanning
     */
    public void setBasedir( String basedir )
    {
        setBasedir( new File( basedir.replace( '/', File.separatorChar ).replace( '\\', File.separatorChar ) ) );
    }

    /**
     * Sets the basedir for scanning. This is the directory that is scanned
     * recursively.
     *
     * @param basedir the basedir for scanning
     */
    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    public void setCaseSensitive( boolean isCaseSensitive )
    {
    }

    public void setExcludes( String[] excludes )
    {
    }

    public void setIncludes( String[] includes )
    {
    }

    /**
     * Sets the domain, where dependant classes are searched
     *
     * @param rootClasses The new RootClasses value
     */
    public void setRootClasses( List rootClasses )
    {
        this.rootClasses = rootClasses;
    }

    /**
     * Gets the basedir that is used for scanning.
     *
     * @return the basedir that is used for scanning
     */
    public File getBasedir()
    {
        return basedir;
    }

    public String[] getExcludedDirectories()
    {
        return null;
    }

    public String[] getExcludedFiles()
    {
        return null;
    }

    public String[] getIncludedDirectories()
    {
        return new String[ 0 ];
    }

    /**
     * Get the names of the class files, baseClass depends on
     *
     * @return the names of the files
     */
    public String[] getIncludedFiles()
    {
        int count = included.size();
        String[] files = new String[ count ];
        for( int i = 0; i < count; i++ )
        {
            files[ i ] = included.get( i ) + ".class";
            //System.err.println("  " + files[i]);
        }
        return files;
    }

    public String[] getNotIncludedDirectories()
    {
        return null;
    }

    public String[] getNotIncludedFiles()
    {
        return null;
    }

    public void addDefaultExcludes()
    {
    }

    /**
     * Scans the base directory for files that baseClass depends on
     *
     */
    public void scan()
    {
        Dependencies visitor = new Dependencies();

        Set set = new TreeSet();

        final String base;
        try
        {
            base = basedir.getCanonicalPath() + File.separator;
        }
        catch( Exception e )
        {
            throw new IllegalArgumentException( e.getMessage() );
        }

        for( Iterator rootClassIterator = rootClasses.iterator(); rootClassIterator.hasNext(); )
        {
            Set newSet = new HashSet();
            String start = (String)rootClassIterator.next();
            start = start.replace( '.', '/' );

            newSet.add( start );
            set.add( start );

            do
            {
                Iterator i = newSet.iterator();
                while( i.hasNext() )
                {
                    String fileName = base + ( (String)i.next() ).replace( '/', File.separatorChar ) + ".class";

                    try
                    {
                        JavaClass javaClass = new ClassParser( fileName ).parse();
                        javaClass.accept( visitor );
                    }
                    catch( IOException e )
                    {
                        System.err.println( "exception: " + e.getMessage() );
                    }
                }
                newSet.clear();
                newSet.addAll( visitor.getDependencies() );
                visitor.clearDependencies();

                Dependencies.applyFilter( newSet,
                                          new Filter()
                                          {
                                              public boolean accept( Object object )
                                              {
                                                  String fileName = base + ( (String)object ).replace( '/', File.separatorChar ) + ".class";
                                                  return new File( fileName ).exists();
                                              }
                                          } );
                newSet.removeAll( set );
                set.addAll( newSet );
            } while( newSet.size() > 0 );
        }

        included.clear();
        included.addAll( set );
    }
}
