/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.depend;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

/**
 * An iterator which iterates through the contents of a java directory. The
 * iterator should be created with the directory at the root of the Java
 * namespace.
 *
 * @author Conor MacNeill
 */
public class DirectoryIterator implements ClassFileIterator
{

    /**
     * The length of the root directory. This is used to remove the root
     * directory from full paths.
     */
    int rootLength;

    /**
     * The current directory iterator. As directories encounter lower level
     * directories, the current iterator is pushed onto the iterator stack and a
     * new iterator over the sub directory becomes the current directory. This
     * implements a depth first traversal of the directory namespace.
     */
    private Iterator currentEnum;

    /**
     * This is a stack of current iterators supporting the depth first traversal
     * of the directory tree.
     */
    private Stack enumStack;

    /**
     * Creates a directory iterator. The directory iterator is created to scan
     * the root directory. If the changeInto flag is given, then the entries
     * returned will be relative to this directory and not the current
     * directory.
     *
     * @param rootDirectory the root if the directory namespace which is to be
     *      iterated over
     * @param changeInto if true then the returned entries will be relative to
     *      the rootDirectory and not the current directory.
     * @exception IOException Description of Exception
     * @throws IOException if there is a problem reading the directory
     *      information.
     */
    public DirectoryIterator( File rootDirectory, boolean changeInto )
        throws IOException
    {
        super();

        enumStack = new Stack();

        if( rootDirectory.isAbsolute() || changeInto )
        {
            rootLength = rootDirectory.getPath().length() + 1;
        }
        else
        {
            rootLength = 0;
        }

        ArrayList filesInRoot = getDirectoryEntries( rootDirectory );

        currentEnum = filesInRoot.iterator();
    }

    /**
     * Template method to allow subclasses to supply elements for the iteration.
     * The directory iterator maintains a stack of iterators covering each level
     * in the directory hierarchy. The current iterator covers the current
     * directory being scanned. If the next entry in that directory is a
     * subdirectory, the current iterator is pushed onto the stack and a new
     * iterator is created for the subdirectory. If the entry is a file, it is
     * returned as the next element and the iterator remains valid. If there are
     * no more entries in the current directory, the topmost iterator on the
     * statck is popped off to become the current iterator.
     *
     * @return the next ClassFile in the iteration.
     */
    public ClassFile getNextClassFile()
    {
        ClassFile next = null;

        try
        {
            while( next == null )
            {
                if( currentEnum.hasNext() )
                {
                    File element = (File)currentEnum.next();

                    if( element.isDirectory() )
                    {

                        // push the current iterator onto the stack and then
                        // iterate through this directory.
                        enumStack.push( currentEnum );

                        ArrayList files = getDirectoryEntries( element );

                        currentEnum = files.iterator();
                    }
                    else
                    {

                        // we have a file. create a stream for it
                        FileInputStream inFileStream = new FileInputStream( element );

                        if( element.getName().endsWith( ".class" ) )
                        {

                            // create a data input stream from the jar input stream
                            ClassFile javaClass = new ClassFile();

                            javaClass.read( inFileStream );

                            next = javaClass;
                        }
                    }
                }
                else
                {
                    // this iterator is exhausted. Can we pop one off the stack
                    if( enumStack.empty() )
                    {
                        break;
                    }
                    else
                    {
                        currentEnum = (Iterator)enumStack.pop();
                    }
                }
            }
        }
        catch( IOException e )
        {
            next = null;
        }

        return next;
    }

    /**
     * Get a vector covering all the entries (files and subdirectories in a
     * directory).
     *
     * @param directory the directory to be scanned.
     * @return a vector containing File objects for each entry in the directory.
     */
    private ArrayList getDirectoryEntries( File directory )
    {
        ArrayList files = new ArrayList();

        // File[] filesInDir = directory.listFiles();
        String[] filesInDir = directory.list();

        if( filesInDir != null )
        {
            int length = filesInDir.length;

            for( int i = 0; i < length; ++i )
            {
                files.add( new File( directory, filesInDir[ i ] ) );
            }
        }

        return files;
    }

}

