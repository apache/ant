/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.DirectoryScanner;

import org.apache.tools.ant.types.FileSet;

/**
 * <p>
 *
 * Create then run <code>JUnitTest</code>'s based on the list of files given by
 * the fileset attribute. <p>
 *
 * Every <code>.java</code> or <code>.class</code> file in the fileset is
 * assumed to be a testcase. A <code>JUnitTest</code> is created for each of
 * these named classes with basic setup inherited from the parent <code>BatchTest</code>
 * .
 *
 * @author <a href="mailto:jeff.martin@synamic.co.uk">Jeff Martin</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 * @see JUnitTest
 */
public final class BatchTest extends BaseTest
{
    /**
     * the list of filesets containing the testcase filename rules
     */
    private ArrayList filesets = new ArrayList();

    /**
     * Convenient method to convert a pathname without extension to a fully
     * qualified classname. For example <tt>org/apache/Whatever</tt> will be
     * converted to <tt>org.apache.Whatever</tt>
     *
     * @param filename the filename to "convert" to a classname.
     * @return the classname matching the filename.
     */
    public static final String javaToClass( String filename )
    {
        return filename.replace( File.separatorChar, '.' );
    }

    /**
     * Return all <tt>JUnitTest</tt> instances obtain by applying the fileset
     * rules.
     *
     * @return an enumeration of all elements of this batchtest that are a <tt>
     *      JUnitTest</tt> instance.
     */
    public final Iterator iterator()
        throws TaskException
    {
        final JUnitTest[] tests = createAllJUnitTest();
        return Arrays.asList( tests ).iterator();
    }

    /**
     * Add a new fileset instance to this batchtest. Whatever the fileset is,
     * only filename that are <tt>.java</tt> or <tt>.class</tt> will be
     * considered as 'candidates'.
     *
     * @param fs the new fileset containing the rules to get the testcases.
     */
    public void addFileSet( FileSet fs )
    {
        filesets.add( fs );
    }

    /**
     * Convenient method to merge the <tt>JUnitTest</tt> s of this batchtest to
     * a <tt>ArrayList</tt> .
     *
     * @param v the vector to which should be added all individual tests of this
     *      batch test.
     */
    final void addTestsTo( ArrayList v )
        throws TaskException
    {
        final JUnitTest[] tests = createAllJUnitTest();
        v.ensureCapacity( v.size() + tests.length );
        for( int i = 0; i < tests.length; i++ )
        {
            v.add( tests[ i ] );
        }
    }

    /**
     * Iterate over all filesets and return the filename of all files that end
     * with <tt>.java</tt> or <tt>.class</tt> . This is to avoid wrapping a <tt>
     * JUnitTest</tt> over an xml file for example. A Testcase is obviously a
     * java file (compiled or not).
     *
     * @return an array of filenames without their extension. As they should
     *      normally be taken from their root, filenames should match their
     *      fully qualified class name (If it is not the case it will fail when
     *      running the test). For the class <tt>org/apache/Whatever.class</tt>
     *      it will return <tt>org/apache/Whatever</tt> .
     */
    private String[] getFilenames()
        throws TaskException
    {
        ArrayList v = new ArrayList();
        final int size = this.filesets.size();
        for( int j = 0; j < size; j++ )
        {
            FileSet fs = (FileSet)filesets.get( j );
            DirectoryScanner ds = fs.getDirectoryScanner();
            ds.scan();
            String[] f = ds.getIncludedFiles();
            for( int k = 0; k < f.length; k++ )
            {
                String pathname = f[ k ];
                if( pathname.endsWith( ".java" ) )
                {
                    v.add( pathname.substring( 0, pathname.length() - ".java".length() ) );
                }
                else if( pathname.endsWith( ".class" ) )
                {
                    v.add( pathname.substring( 0, pathname.length() - ".class".length() ) );
                }
            }
        }

        return (String[])v.toArray( new String[ v.size() ] );
    }

    /**
     * Create all <tt>JUnitTest</tt> s based on the filesets. Each instance is
     * configured to match this instance properties.
     *
     * @return the array of all <tt>JUnitTest</tt> s that belongs to this batch.
     */
    private JUnitTest[] createAllJUnitTest()
        throws TaskException
    {
        String[] filenames = getFilenames();
        JUnitTest[] tests = new JUnitTest[ filenames.length ];
        for( int i = 0; i < tests.length; i++ )
        {
            String classname = javaToClass( filenames[ i ] );
            tests[ i ] = createJUnitTest( classname );
        }
        return tests;
    }

    /**
     * Create a <tt>JUnitTest</tt> that has the same property as this <tt>
     * BatchTest</tt> instance.
     *
     * @param classname the name of the class that should be run as a <tt>
     *      JUnitTest</tt> . It must be a fully qualified name.
     * @return the <tt>JUnitTest</tt> over the given classname.
     */
    private JUnitTest createJUnitTest( String classname )
    {
        JUnitTest test = new JUnitTest();
        test.setName( classname );
        test.setHaltonerror( this.m_haltOnError );
        test.setHaltonfailure( this.m_haltOnFail );
        test.setFiltertrace( this.m_filtertrace );
        test.setFork( this.fork );
        test.setIf( this.ifProperty );
        test.setUnless( this.unlessProperty );
        test.setTodir( this.destDir );
        test.setFailureProperty( failureProperty );
        test.setErrorProperty( errorProperty );
        Iterator list = this.formatters.iterator();
        while( list.hasNext() )
        {
            test.addFormatter( (FormatterElement)list.next() );
        }
        return test;
    }

}
