/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.apache.myrmidon.api.TaskException;

/**
 * Class for scanning a directory for files/directories that match a certain
 * criteria. <p>
 *
 * These criteria consist of a set of include and exclude patterns. With these
 * patterns, you can select which files you want to have included, and which
 * files you want to have excluded. <p>
 *
 * The idea is simple. A given directory is recursively scanned for all files
 * and directories. Each file/directory is matched against a set of include and
 * exclude patterns. Only files/directories that match at least one pattern of
 * the include pattern list, and don't match a pattern of the exclude pattern
 * list will be placed in the list of files/directories found. <p>
 *
 * When no list of include patterns is supplied, "**" will be used, which means
 * that everything will be matched. When no list of exclude patterns is
 * supplied, an empty list is used, such that nothing will be excluded. <p>
 *
 * The pattern matching is done as follows: The name to be matched is split up
 * in path segments. A path segment is the name of a directory or file, which is
 * bounded by <code>File.separator</code> ('/' under UNIX, '\' under Windows).
 * E.g. "abc/def/ghi/xyz.java" is split up in the segments "abc", "def", "ghi"
 * and "xyz.java". The same is done for the pattern against which should be
 * matched. <p>
 *
 * Then the segments of the name and the pattern will be matched against each
 * other. When '**' is used for a path segment in the pattern, then it matches
 * zero or more path segments of the name. <p>
 *
 * There are special case regarding the use of <code>File.separator</code>s at
 * the beginningof the pattern and the string to match:<br>
 * When a pattern starts with a <code>File.separator</code>, the string to match
 * must also start with a <code>File.separator</code>. When a pattern does not
 * start with a <code>File.separator</code>, the string to match may not start
 * with a <code>File.separator</code>. When one of these rules is not obeyed,
 * the string will not match. <p>
 *
 * When a name path segment is matched against a pattern path segment, the
 * following special characters can be used: '*' matches zero or more
 * characters, '?' matches one character. <p>
 *
 * Examples: <p>
 *
 * "**\*.class" matches all .class files/dirs in a directory tree. <p>
 *
 * "test\a??.java" matches all files/dirs which start with an 'a', then two more
 * characters and then ".java", in a directory called test. <p>
 *
 * "**" matches everything in a directory tree. <p>
 *
 * "**\test\**\XYZ*" matches all files/dirs that start with "XYZ" and where
 * there is a parent directory called test (e.g. "abc\test\def\ghi\XYZ123"). <p>
 *
 * Case sensitivity may be turned off if necessary. By default, it is turned on.
 * <p>
 *
 * Example of usage: <pre>
 *   String[] includes = {"**\\*.class"};
 *   String[] excludes = {"modules\\*\\**"};
 *   ds.setIncludes(includes);
 *   ds.setExcludes(excludes);
 *   ds.setBasedir(new File("test"));
 *   ds.setCaseSensitive(true);
 *   ds.scan();
 *
 *   System.out.println("FILES:");
 *   String[] files = ds.getIncludedFiles();
 *   for (int i = 0; i < files.length;i++) {
 *     System.out.println(files[i]);
 *   }
 * </pre> This will scan a directory called test for .class files, but excludes
 * all .class files in all directories under a directory called "modules"
 *
 * @author Arnout J. Kuiper <a href="mailto:ajkuiper@wxs.nl">ajkuiper@wxs.nl</a>
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 */
public class DirectoryScanner
    implements FileScanner
{

    /**
     * Have the ArrayLists holding our results been built by a slow scan?
     */
    private boolean m_haveSlowResults;

    /**
     * Should the file system be treated as a case sensitive one?
     */
    private boolean m_isCaseSensitive = true;

    /**
     * Is everything we've seen so far included?
     */
    private boolean m_everythingIncluded = true;

    /**
     * The base directory which should be scanned.
     */
    private File m_basedir;

    /**
     * The files that where found and matched at least one includes, and also
     * matched at least one excludes.
     */
    private ArrayList m_dirsExcluded;

    /**
     * The directories that where found and matched at least one includes, and
     * matched no excludes.
     */
    private ArrayList m_dirsIncluded;

    /**
     * The directories that where found and did not match any includes.
     */
    private ArrayList m_dirsNotIncluded;

    /**
     * The patterns for the files that should be excluded.
     */
    private String[] m_excludes;

    /**
     * The files that where found and matched at least one includes, and also
     * matched at least one excludes.
     */
    private ArrayList m_filesExcluded;

    /**
     * The files that where found and matched at least one includes, and matched
     * no excludes.
     */
    private ArrayList m_filesIncluded;

    /**
     * The files that where found and did not match any includes.
     */
    private ArrayList m_filesNotIncluded;

    /**
     * The patterns for the files that should be included.
     */
    private String[] m_includes;

    /**
     * Sets the basedir for scanning. This is the directory that is scanned
     * recursively. All '/' and '\' characters are replaced by <code>File.separatorChar</code>
     * . So the separator used need not match <code>File.separatorChar</code>.
     *
     * @param basedir the (non-null) basedir for scanning
     */
    public void setBasedir( final String basedir )
    {
        setBasedir( new File( basedir.replace( '/', File.separatorChar ).replace( '\\', File.separatorChar ) ) );
    }

    /**
     * Sets the basedir for scanning. This is the directory that is scanned
     * recursively.
     *
     * @param basedir the basedir for scanning
     */
    public void setBasedir( final File basedir )
    {
        m_basedir = basedir;
    }

    /**
     * Sets the case sensitivity of the file system
     *
     * @param isCaseSensitive The new CaseSensitive value
     */
    public void setCaseSensitive( final boolean isCaseSensitive )
    {
        m_isCaseSensitive = isCaseSensitive;
    }

    /**
     * Sets the set of exclude patterns to use. All '/' and '\' characters are
     * replaced by <code>File.separatorChar</code>. So the separator used need
     * not match <code>File.separatorChar</code>. <p>
     *
     * When a pattern ends with a '/' or '\', "**" is appended.
     *
     * @param excludes list of exclude patterns
     */
    public void setExcludes( final String[] excludes )
    {
        if( excludes == null )
        {
            m_excludes = null;
        }
        else
        {
            m_excludes = new String[ excludes.length ];
            for( int i = 0; i < excludes.length; i++ )
            {
                String pattern;
                pattern = excludes[ i ].replace( '/', File.separatorChar ).replace( '\\', File.separatorChar );
                if( pattern.endsWith( File.separator ) )
                {
                    pattern += "**";
                }
                m_excludes[ i ] = pattern;
            }
        }
    }

    /**
     * Sets the set of include patterns to use. All '/' and '\' characters are
     * replaced by <code>File.separatorChar</code>. So the separator used need
     * not match <code>File.separatorChar</code>. <p>
     *
     * When a pattern ends with a '/' or '\', "**" is appended.
     *
     * @param includes list of include patterns
     */
    public void setIncludes( final String[] includes )
    {
        if( includes == null )
        {
            m_includes = null;
        }
        else
        {
            m_includes = new String[ includes.length ];
            for( int i = 0; i < includes.length; i++ )
            {
                String pattern;
                pattern = includes[ i ].replace( '/', File.separatorChar ).replace( '\\', File.separatorChar );
                if( pattern.endsWith( File.separator ) )
                {
                    pattern += "**";
                }
                m_includes[ i ] = pattern;
            }
        }
    }

    /**
     * Gets the basedir that is used for scanning. This is the directory that is
     * scanned recursively.
     *
     * @return the basedir that is used for scanning
     */
    public File getBasedir()
    {
        return m_basedir;
    }

    /**
     * Get the names of the directories that matched at least one of the include
     * patterns, an matched also at least one of the exclude patterns. The names
     * are relative to the basedir.
     *
     * @return the names of the directories
     */
    public String[] getExcludedDirectories()
        throws TaskException
    {
        slowScan();
        int count = m_dirsExcluded.size();
        String[] directories = new String[ count ];
        for( int i = 0; i < count; i++ )
        {
            directories[ i ] = (String)m_dirsExcluded.get( i );
        }
        return directories;
    }

    /**
     * Get the names of the files that matched at least one of the include
     * patterns, an matched also at least one of the exclude patterns. The names
     * are relative to the basedir.
     *
     * @return the names of the files
     */
    public String[] getExcludedFiles()
        throws TaskException
    {
        slowScan();
        int count = m_filesExcluded.size();
        String[] files = new String[ count ];
        for( int i = 0; i < count; i++ )
        {
            files[ i ] = (String)m_filesExcluded.get( i );
        }
        return files;
    }

    /**
     * Get the names of the directories that matched at least one of the include
     * patterns, an matched none of the exclude patterns. The names are relative
     * to the basedir.
     *
     * @return the names of the directories
     */
    public String[] getIncludedDirectories()
    {
        int count = m_dirsIncluded.size();
        String[] directories = new String[ count ];
        for( int i = 0; i < count; i++ )
        {
            directories[ i ] = (String)m_dirsIncluded.get( i );
        }
        return directories;
    }

    /**
     * Get the names of the files that matched at least one of the include
     * patterns, and matched none of the exclude patterns. The names are
     * relative to the basedir.
     *
     * @return the names of the files
     */
    public String[] getIncludedFiles()
    {
        int count = m_filesIncluded.size();
        String[] files = new String[ count ];
        for( int i = 0; i < count; i++ )
        {
            files[ i ] = (String)m_filesIncluded.get( i );
        }
        return files;
    }

    /**
     * Get the names of the directories that matched at none of the include
     * patterns. The names are relative to the basedir.
     *
     * @return the names of the directories
     */
    public String[] getNotIncludedDirectories()
        throws TaskException
    {
        slowScan();
        int count = m_dirsNotIncluded.size();
        String[] directories = new String[ count ];
        for( int i = 0; i < count; i++ )
        {
            directories[ i ] = (String)m_dirsNotIncluded.get( i );
        }
        return directories;
    }

    /**
     * Get the names of the files that matched at none of the include patterns.
     * The names are relative to the basedir.
     *
     * @return the names of the files
     */
    public String[] getNotIncludedFiles()
        throws TaskException
    {
        slowScan();
        int count = m_filesNotIncluded.size();
        String[] files = new String[ count ];
        for( int i = 0; i < count; i++ )
        {
            files[ i ] = (String)m_filesNotIncluded.get( i );
        }
        return files;
    }

    /**
     * Has the scanner excluded or omitted any files or directories it came
     * accross?
     *
     * @return true if all files and directories that have been found, are
     *      included.
     */
    public boolean isEverythingIncluded()
    {
        return m_everythingIncluded;
    }

    /**
     * Adds the array with default exclusions to the current exclusions set.
     */
    public void addDefaultExcludes()
    {
        int excludesLength = m_excludes == null ? 0 : m_excludes.length;
        String[] newExcludes;
        newExcludes = new String[ excludesLength + ScannerUtil.DEFAULTEXCLUDES.length ];
        if( excludesLength > 0 )
        {
            System.arraycopy( m_excludes, 0, newExcludes, 0, excludesLength );
        }
        for( int i = 0; i < ScannerUtil.DEFAULTEXCLUDES.length; i++ )
        {
            newExcludes[ i + excludesLength ] = ScannerUtil.DEFAULTEXCLUDES[ i ].replace( '/', File.separatorChar ).replace( '\\', File.separatorChar );
        }
        m_excludes = newExcludes;
    }

    /**
     * Scans the base directory for files that match at least one include
     * pattern, and don't match any exclude patterns.
     *
     */
    public void scan()
        throws TaskException
    {
        if( m_basedir == null )
        {
            throw new IllegalStateException( "No basedir set" );
        }
        if( !m_basedir.exists() )
        {
            throw new IllegalStateException( "basedir " + m_basedir
                                             + " does not exist" );
        }
        if( !m_basedir.isDirectory() )
        {
            throw new IllegalStateException( "basedir " + m_basedir
                                             + " is not a directory" );
        }

        if( m_includes == null )
        {
            // No includes supplied, so set it to 'matches all'
            m_includes = new String[ 1 ];
            m_includes[ 0 ] = "**";
        }
        if( m_excludes == null )
        {
            m_excludes = new String[ 0 ];
        }

        m_filesIncluded = new ArrayList();
        m_filesNotIncluded = new ArrayList();
        m_filesExcluded = new ArrayList();
        m_dirsIncluded = new ArrayList();
        m_dirsNotIncluded = new ArrayList();
        m_dirsExcluded = new ArrayList();

        if( isIncluded( "" ) )
        {
            if( !isExcluded( "" ) )
            {
                m_dirsIncluded.add( "" );
            }
            else
            {
                m_dirsExcluded.add( "" );
            }
        }
        else
        {
            m_dirsNotIncluded.add( "" );
        }
        scandir( m_basedir, "", true );
    }

    /**
     * Tests whether a name matches against at least one exclude pattern.
     *
     * @param name the name to match
     * @return <code>true</code> when the name matches against at least one
     *      exclude pattern, <code>false</code> otherwise.
     */
    protected boolean isExcluded( String name )
    {
        for( int i = 0; i < m_excludes.length; i++ )
        {
            if( ScannerUtil.matchPath( m_excludes[ i ], name, m_isCaseSensitive ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether a name matches against at least one include pattern.
     *
     * @param name the name to match
     * @return <code>true</code> when the name matches against at least one
     *      include pattern, <code>false</code> otherwise.
     */
    protected boolean isIncluded( final String name )
    {
        for( int i = 0; i < m_includes.length; i++ )
        {
            if( ScannerUtil.matchPath( m_includes[ i ], name, m_isCaseSensitive ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether a name matches the start of at least one include pattern.
     *
     * @param name the name to match
     * @return <code>true</code> when the name matches against at least one
     *      include pattern, <code>false</code> otherwise.
     */
    protected boolean couldHoldIncluded( final String name )
    {
        for( int i = 0; i < m_includes.length; i++ )
        {
            if( ScannerUtil.matchPatternStart( m_includes[ i ], name, m_isCaseSensitive ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Scans the passed dir for files and directories. Found files and
     * directories are placed in their respective collections, based on the
     * matching of includes and excludes. When a directory is found, it is
     * scanned recursively.
     *
     * @param dir the directory to scan
     * @param vpath the path relative to the basedir (needed to prevent problems
     *      with an absolute path when using dir)
     * @param fast Description of Parameter
     * @see #filesIncluded
     * @see #filesNotIncluded
     * @see #filesExcluded
     * @see #dirsIncluded
     * @see #dirsNotIncluded
     * @see #dirsExcluded
     */
    protected void scandir( final File dir, final String vpath, final boolean fast )
        throws TaskException
    {
        String[] newfiles = dir.list();

        if( newfiles == null )
        {
            /*
             * two reasons are mentioned in the API docs for File.list
             * (1) dir is not a directory. This is impossible as
             * we wouldn't get here in this case.
             * (2) an IO error occurred (why doesn't it throw an exception
             * then???)
             */
            throw new TaskException( "IO error scanning directory "
                                     + dir.getAbsolutePath() );
        }

        for( int i = 0; i < newfiles.length; i++ )
        {
            String name = vpath + newfiles[ i ];
            File file = new File( dir, newfiles[ i ] );
            if( file.isDirectory() )
            {
                if( isIncluded( name ) )
                {
                    if( !isExcluded( name ) )
                    {
                        m_dirsIncluded.add( name );
                        if( fast )
                        {
                            scandir( file, name + File.separator, fast );
                        }
                    }
                    else
                    {
                        m_everythingIncluded = false;
                        m_dirsExcluded.add( name );
                        if( fast && couldHoldIncluded( name ) )
                        {
                            scandir( file, name + File.separator, fast );
                        }
                    }
                }
                else
                {
                    m_everythingIncluded = false;
                    m_dirsNotIncluded.add( name );
                    if( fast && couldHoldIncluded( name ) )
                    {
                        scandir( file, name + File.separator, fast );
                    }
                }
                if( !fast )
                {
                    scandir( file, name + File.separator, fast );
                }
            }
            else if( file.isFile() )
            {
                if( isIncluded( name ) )
                {
                    if( !isExcluded( name ) )
                    {
                        m_filesIncluded.add( name );
                    }
                    else
                    {
                        m_everythingIncluded = false;
                        m_filesExcluded.add( name );
                    }
                }
                else
                {
                    m_everythingIncluded = false;
                    m_filesNotIncluded.add( name );
                }
            }
        }
    }

    /**
     * Toplevel invocation for the scan. <p>
     *
     * Returns immediately if a slow scan has already been requested.
     */
    protected void slowScan()
        throws TaskException
    {
        if( m_haveSlowResults )
        {
            return;
        }

        String[] excl = new String[ m_dirsExcluded.size() ];
        excl = (String[])m_dirsExcluded.toArray( excl );

        String[] notIncl = new String[ m_dirsNotIncluded.size() ];
        notIncl = (String[])m_dirsNotIncluded.toArray( notIncl );

        for( int i = 0; i < excl.length; i++ )
        {
            if( !couldHoldIncluded( excl[ i ] ) )
            {
                scandir( new File( m_basedir, excl[ i ] ),
                         excl[ i ] + File.separator, false );
            }
        }

        for( int i = 0; i < notIncl.length; i++ )
        {
            if( !couldHoldIncluded( notIncl[ i ] ) )
            {
                scandir( new File( m_basedir, notIncl[ i ] ),
                         notIncl[ i ] + File.separator, false );
            }
        }

        m_haveSlowResults = true;
    }

    public ArrayList getDirsExcluded()
    {
        return m_dirsExcluded;
    }

    public void setDirsExcluded( ArrayList dirsExcluded )
    {
        m_dirsExcluded = dirsExcluded;
    }

    public ArrayList getDirsIncluded()
    {
        return m_dirsIncluded;
    }

    public void setDirsIncluded( ArrayList dirsIncluded )
    {
        m_dirsIncluded = dirsIncluded;
    }

    public ArrayList getDirsNotIncluded()
    {
        return m_dirsNotIncluded;
    }

    public void setDirsNotIncluded( ArrayList dirsNotIncluded )
    {
        m_dirsNotIncluded = dirsNotIncluded;
    }

    public String[] getExcludes()
    {
        return m_excludes;
    }

    public ArrayList getFilesExcluded()
    {
        return m_filesExcluded;
    }

    public void setFilesExcluded( ArrayList filesExcluded )
    {
        m_filesExcluded = filesExcluded;
    }

    public ArrayList getFilesIncluded()
    {
        return m_filesIncluded;
    }

    public void setFilesIncluded( ArrayList filesIncluded )
    {
        m_filesIncluded = filesIncluded;
    }

    public ArrayList getFilesNotIncluded()
    {
        return m_filesNotIncluded;
    }

    public void setFilesNotIncluded( ArrayList filesNotIncluded )
    {
        m_filesNotIncluded = filesNotIncluded;
    }

    public String[] getIncludes()
    {
        return m_includes;
    }
}
