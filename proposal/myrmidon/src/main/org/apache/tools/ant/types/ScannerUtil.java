/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.PatternUtil;
import org.apache.myrmidon.framework.PatternSet;

/**
 *
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author Arnout J. Kuiper <a href="mailto:ajkuiper@wxs.nl">ajkuiper@wxs.nl</a>
 * @author <a href="mailto:umagesh@rediffmail.com">Magesh Umasankar</a>
 * @version $Revision$ $Date$
 */
public class ScannerUtil
{
    /**
     * Patterns that should be excluded by default.
     */
    public final static String[] DEFAULTEXCLUDES = new String[]
    {
        "**/*~",
        "**/#*#",
        "**/.#*",
        "**/%*%",
        "**/CVS",
        "**/CVS/**",
        "**/.cvsignore",
        "**/SCCS",
        "**/SCCS/**",
        "**/vssver.scc"
    };

    /**
     * Matches a string against a pattern. The pattern contains two special
     * characters: '*' which means zero or more characters, '?' which means one
     * and only one character.
     *
     * @param pattern the (non-null) pattern to match against
     * @param str the (non-null) string that must be matched against the pattern
     * @return <code>true</code> when the string matches against the pattern,
     *      <code>false</code> otherwise.
     */
    public static boolean match( final String pattern, final String str )
    {
        return match( pattern, str, true );
    }

    /**
     * Matches a string against a pattern. The pattern contains two special
     * characters: '*' which means zero or more characters, '?' which means one
     * and only one character.
     *
     * @param pattern the (non-null) pattern to match against
     * @param str the (non-null) string that must be matched against the pattern
     * @param isCaseSensitive Description of Parameter
     * @return <code>true</code> when the string matches against the pattern,
     *      <code>false</code> otherwise.
     */
    protected static boolean match( final String pattern,
                                    final String str,
                                    final boolean isCaseSensitive )
    {
        char[] patArr = pattern.toCharArray();
        char[] strArr = str.toCharArray();
        int patIdxStart = 0;
        int patIdxEnd = patArr.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strArr.length - 1;
        char ch;

        boolean containsStar = false;
        for( int i = 0; i < patArr.length; i++ )
        {
            if( patArr[ i ] == '*' )
            {
                containsStar = true;
                break;
            }
        }

        if( !containsStar )
        {
            // No '*'s, so we make a shortcut
            if( patIdxEnd != strIdxEnd )
            {
                return false;// Pattern and string do not have the same size
            }
            for( int i = 0; i <= patIdxEnd; i++ )
            {
                ch = patArr[ i ];
                if( ch != '?' )
                {
                    if( isCaseSensitive && ch != strArr[ i ] )
                    {
                        return false;// Character mismatch
                    }
                    if( !isCaseSensitive && Character.toUpperCase( ch ) !=
                        Character.toUpperCase( strArr[ i ] ) )
                    {
                        return false;// Character mismatch
                    }
                }
            }
            return true;// String matches against pattern
        }

        if( patIdxEnd == 0 )
        {
            return true;// Pattern contains only '*', which matches anything
        }

        // Process characters before first star
        while( ( ch = patArr[ patIdxStart ] ) != '*' && strIdxStart <= strIdxEnd )
        {
            if( ch != '?' )
            {
                if( isCaseSensitive && ch != strArr[ strIdxStart ] )
                {
                    return false;// Character mismatch
                }
                if( !isCaseSensitive && Character.toUpperCase( ch ) !=
                    Character.toUpperCase( strArr[ strIdxStart ] ) )
                {
                    return false;// Character mismatch
                }
            }
            patIdxStart++;
            strIdxStart++;
        }
        if( strIdxStart > strIdxEnd )
        {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for( int i = patIdxStart; i <= patIdxEnd; i++ )
            {
                if( patArr[ i ] != '*' )
                {
                    return false;
                }
            }
            return true;
        }

        // Process characters after last star
        while( ( ch = patArr[ patIdxEnd ] ) != '*' && strIdxStart <= strIdxEnd )
        {
            if( ch != '?' )
            {
                if( isCaseSensitive && ch != strArr[ strIdxEnd ] )
                {
                    return false;// Character mismatch
                }
                if( !isCaseSensitive && Character.toUpperCase( ch ) !=
                    Character.toUpperCase( strArr[ strIdxEnd ] ) )
                {
                    return false;// Character mismatch
                }
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if( strIdxStart > strIdxEnd )
        {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for( int i = patIdxStart; i <= patIdxEnd; i++ )
            {
                if( patArr[ i ] != '*' )
                {
                    return false;
                }
            }
            return true;
        }

        // process pattern between stars. padIdxStart and patIdxEnd point
        // always to a '*'.
        while( patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd )
        {
            int patIdxTmp = -1;
            for( int i = patIdxStart + 1; i <= patIdxEnd; i++ )
            {
                if( patArr[ i ] == '*' )
                {
                    patIdxTmp = i;
                    break;
                }
            }
            if( patIdxTmp == patIdxStart + 1 )
            {
                // Two stars next to each other, skip the first one.
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = ( patIdxTmp - patIdxStart - 1 );
            int strLength = ( strIdxEnd - strIdxStart + 1 );
            int foundIdx = -1;
            strLoop :
            for( int i = 0; i <= strLength - patLength; i++ )
            {
                for( int j = 0; j < patLength; j++ )
                {
                    ch = patArr[ patIdxStart + j + 1 ];
                    if( ch != '?' )
                    {
                        if( isCaseSensitive && ch != strArr[ strIdxStart + i + j ] )
                        {
                            continue strLoop;
                        }
                        if( !isCaseSensitive && Character.toUpperCase( ch ) !=
                            Character.toUpperCase( strArr[ strIdxStart + i + j ] ) )
                        {
                            continue strLoop;
                        }
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if( foundIdx == -1 )
            {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        // All characters in the string are used. Check if only '*'s are left
        // in the pattern. If so, we succeeded. Otherwise failure.
        for( int i = patIdxStart; i <= patIdxEnd; i++ )
        {
            if( patArr[ i ] != '*' )
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Matches a path against a pattern.
     *
     * @param pattern the (non-null) pattern to match against
     * @param str the (non-null) string (path) to match
     * @return <code>true</code> when the pattern matches against the string.
     *      <code>false</code> otherwise.
     */
    protected static boolean matchPath( final String pattern, final String str )
    {
        return matchPath( pattern, str, true );
    }

    /**
     * Matches a path against a pattern.
     *
     * @param pattern the (non-null) pattern to match against
     * @param str the (non-null) string (path) to match
     * @param isCaseSensitive must a case sensitive match be done?
     * @return <code>true</code> when the pattern matches against the string.
     *      <code>false</code> otherwise.
     */
    protected static boolean matchPath( final String pattern,
                                        final String str,
                                        final boolean isCaseSensitive )
    {
        // When str starts with a File.separator, pattern has to start with a
        // File.separator.
        // When pattern starts with a File.separator, str has to start with a
        // File.separator.
        if( str.startsWith( File.separator ) !=
            pattern.startsWith( File.separator ) )
        {
            return false;
        }

        ArrayList patDirs = new ArrayList();
        StringTokenizer st = new StringTokenizer( pattern, File.separator );
        while( st.hasMoreTokens() )
        {
            patDirs.add( st.nextToken() );
        }

        ArrayList strDirs = new ArrayList();
        st = new StringTokenizer( str, File.separator );
        while( st.hasMoreTokens() )
        {
            strDirs.add( st.nextToken() );
        }

        int patIdxStart = 0;
        int patIdxEnd = patDirs.size() - 1;
        int strIdxStart = 0;
        int strIdxEnd = strDirs.size() - 1;

        // up to first '**'
        while( patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd )
        {
            String patDir = (String)patDirs.get( patIdxStart );
            if( patDir.equals( "**" ) )
            {
                break;
            }
            if( !match( patDir, (String)strDirs.get( strIdxStart ), isCaseSensitive ) )
            {
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }
        if( strIdxStart > strIdxEnd )
        {
            // String is exhausted
            for( int i = patIdxStart; i <= patIdxEnd; i++ )
            {
                if( !patDirs.get( i ).equals( "**" ) )
                {
                    return false;
                }
            }
            return true;
        }
        else
        {
            if( patIdxStart > patIdxEnd )
            {
                // String not exhausted, but pattern is. Failure.
                return false;
            }
        }

        // up to last '**'
        while( patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd )
        {
            String patDir = (String)patDirs.get( patIdxEnd );
            if( patDir.equals( "**" ) )
            {
                break;
            }
            if( !match( patDir, (String)strDirs.get( strIdxEnd ), isCaseSensitive ) )
            {
                return false;
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if( strIdxStart > strIdxEnd )
        {
            // String is exhausted
            for( int i = patIdxStart; i <= patIdxEnd; i++ )
            {
                if( !patDirs.get( i ).equals( "**" ) )
                {
                    return false;
                }
            }
            return true;
        }

        while( patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd )
        {
            int patIdxTmp = -1;
            for( int i = patIdxStart + 1; i <= patIdxEnd; i++ )
            {
                if( patDirs.get( i ).equals( "**" ) )
                {
                    patIdxTmp = i;
                    break;
                }
            }
            if( patIdxTmp == patIdxStart + 1 )
            {
                // '**/**' situation, so skip one
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = ( patIdxTmp - patIdxStart - 1 );
            int strLength = ( strIdxEnd - strIdxStart + 1 );
            int foundIdx = -1;
            strLoop :
            for( int i = 0; i <= strLength - patLength; i++ )
            {
                for( int j = 0; j < patLength; j++ )
                {
                    String subPat = (String)patDirs.get( patIdxStart + j + 1 );
                    String subStr = (String)strDirs.get( strIdxStart + i + j );
                    if( !match( subPat, subStr, isCaseSensitive ) )
                    {
                        continue strLoop;
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if( foundIdx == -1 )
            {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        for( int i = patIdxStart; i <= patIdxEnd; i++ )
        {
            if( !patDirs.get( i ).equals( "**" ) )
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Does the path match the start of this pattern up to the first "**". <p>
     *
     * This is not a general purpose test and should only be used if you can
     * live with false positives.</p> <p>
     *
     * <code>pattern=**\\a</code> and <code>str=b</code> will yield true.
     *
     * @param pattern the (non-null) pattern to match against
     * @param str the (non-null) string (path) to match
     * @return Description of the Returned Value
     */
    protected static boolean matchPatternStart( final String pattern, final String str )
    {
        return matchPatternStart( pattern, str, true );
    }

    /**
     * Does the path match the start of this pattern up to the first "**". <p>
     *
     * This is not a general purpose test and should only be used if you can
     * live with false positives.</p> <p>
     *
     * <code>pattern=**\\a</code> and <code>str=b</code> will yield true.
     *
     * @param pattern the (non-null) pattern to match against
     * @param str the (non-null) string (path) to match
     * @param isCaseSensitive must matches be case sensitive?
     * @return Description of the Returned Value
     */
    protected static boolean matchPatternStart( final String pattern,
                                                final String str,
                                                final boolean isCaseSensitive )
    {
        // When str starts with a File.separator, pattern has to start with a
        // File.separator.
        // When pattern starts with a File.separator, str has to start with a
        // File.separator.
        if( str.startsWith( File.separator ) !=
            pattern.startsWith( File.separator ) )
        {
            return false;
        }

        ArrayList patDirs = new ArrayList();
        StringTokenizer st = new StringTokenizer( pattern, File.separator );
        while( st.hasMoreTokens() )
        {
            patDirs.add( st.nextToken() );
        }

        ArrayList strDirs = new ArrayList();
        st = new StringTokenizer( str, File.separator );
        while( st.hasMoreTokens() )
        {
            strDirs.add( st.nextToken() );
        }

        int patIdxStart = 0;
        int patIdxEnd = patDirs.size() - 1;
        int strIdxStart = 0;
        int strIdxEnd = strDirs.size() - 1;

        // up to first '**'
        while( patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd )
        {
            String patDir = (String)patDirs.get( patIdxStart );
            if( patDir.equals( "**" ) )
            {
                break;
            }
            if( !match( patDir, (String)strDirs.get( strIdxStart ), isCaseSensitive ) )
            {
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }

        if( strIdxStart > strIdxEnd )
        {
            // String is exhausted
            return true;
        }
        else if( patIdxStart > patIdxEnd )
        {
            // String not exhausted, but pattern is. Failure.
            return false;
        }
        else
        {
            // pattern now holds ** while string is not exhausted
            // this will generate false positives but we can live with that.
            return true;
        }
    }

    public static void setupDirectoryScanner( final FileSet set,
                                              final FileScanner scanner,
                                              final TaskContext context )
        throws TaskException
    {
        if( null == scanner )
        {
            final String message = "ds cannot be null";
            throw new IllegalArgumentException( message );
        }

        scanner.setBasedir( set.getDir() );

        final String message = "FileSet: Setup file scanner in dir " +
            set.getDir() + " with " + set;
        //getLogger().debug( message );

        scanner.setIncludes( PatternUtil.getIncludePatterns( set, context ) );
        scanner.setExcludes( PatternUtil.getExcludePatterns( set, context ) );
        if( set.includeDefaultExcludes() )
        {
            scanner.addDefaultExcludes();
        }
        scanner.setCaseSensitive( set.isCaseSensitive() );
    }

    public static DirectoryScanner getDirectoryScanner( final FileSet set )
        throws TaskException
    {
        final File dir = set.getDir();
        if( null == dir )
        {
            final String message = "No directory specified for fileset.";
            throw new TaskException( message );
        }

        if( !dir.exists() )
        {
            final String message = dir.getAbsolutePath() + " not found.";
            throw new TaskException( message );
        }
        if( !dir.isDirectory() )
        {
            final String message = dir.getAbsolutePath() + " is not a directory.";
            throw new TaskException( message );
        }

        final DirectoryScanner scanner = new DirectoryScanner();
        setupDirectoryScanner( set, scanner, null );
        scanner.scan();
        return scanner;
    }

    public static DirectoryScanner getZipScanner( final ZipFileSet set )
        throws TaskException
    {
        final File dir = set.getDir();
        final File src = set.getSrc();

        if( null != dir && null != src )
        {
            throw new TaskException( "Cannot set both dir and src attributes" );
        }

        if( null != src )
        {
            final ZipScanner scanner = new ZipScanner();
            scanner.setSrc( src );
            set.setDir( null  );
            setupDirectoryScanner( set, scanner, null );
            scanner.init();
            return scanner;
        }
        else
        {
            return getDirectoryScanner( set );
        }
    }
}
