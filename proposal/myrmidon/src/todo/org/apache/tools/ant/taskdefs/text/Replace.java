/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Properties;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.avalon.excalibur.util.StringUtil;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.DirectoryScanner;

/**
 * Replaces all occurrences of one or more string tokens with given values in
 * the indicated files. Each value can be either a string or the value of a
 * property available in a designated property file.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">
 *      stefano@apache.org</a>
 * @author <a href="mailto:erik@desknetinc.com">Erik Langenbach</a>
 */
public class Replace
    extends MatchingTask
{
    private File m_src;
    private NestedString m_token;
    private NestedString m_value = new NestedString();

    private File m_propertyFile;
    private Properties m_properties;
    private ArrayList m_replacefilters = new ArrayList();

    private File m_dir;
    private boolean m_summary;

    /**
     * The encoding used to read and write files - if null, uses default
     */
    private String m_encoding;

    private int m_fileCount;
    private int m_replaceCount;

    /**
     * Set the source files path when using matching tasks.
     *
     * @param dir The new Dir value
     */
    public void setDir( File dir )
    {
        m_dir = dir;
    }

    /**
     * Set the file encoding to use on the files read and written by replace
     *
     * @param encoding the encoding to use on the files
     */
    public void setEncoding( String encoding )
    {
        m_encoding = encoding;
    }

    /**
     * Set the source file.
     *
     * @param file The new File value
     */
    public void setFile( File file )
    {
        m_src = file;
    }

    /**
     * Sets a file to be searched for property values.
     *
     * @param filename The new PropertyFile value
     */
    public void setPropertyFile( File filename )
    {
        m_propertyFile = filename;
    }

    /**
     * Request a summary
     *
     * @param summary true if you would like a summary logged of the replace
     *      operation
     */
    public void setSummary( boolean summary )
    {
        m_summary = summary;
    }

    /**
     * Set the string token to replace.
     *
     * @param token The new Token value
     */
    public void setToken( String token )
    {
        createReplaceToken().addContent( token );
    }

    /**
     * Set the string value to use as token replacement.
     *
     * @param value The new Value value
     */
    public void setValue( String value )
    {
        createReplaceValue().addContent( value );
    }

    public Properties getProperties( File propertyFile )
        throws TaskException
    {
        Properties properties = new Properties();

        try
        {
            properties.load( new FileInputStream( propertyFile ) );
        }
        catch( FileNotFoundException e )
        {
            String message = "Property file (" + propertyFile.getPath() + ") not found.";
            throw new TaskException( message );
        }
        catch( IOException e )
        {
            String message = "Property file (" + propertyFile.getPath() + ") cannot be loaded.";
            throw new TaskException( message );
        }

        return properties;
    }

    /**
     * Nested &lt;replacetoken&gt; element.
     *
     * @return Description of the Returned Value
     */
    public NestedString createReplaceToken()
    {
        if( m_token == null )
        {
            m_token = new NestedString();
        }
        return m_token;
    }

    /**
     * Nested &lt;replacevalue&gt; element.
     *
     * @return Description of the Returned Value
     */
    public NestedString createReplaceValue()
    {
        return m_value;
    }

    /**
     * Add nested &lt;replacefilter&gt; element.
     *
     * @return Description of the Returned Value
     */
    public Replacefilter createReplacefilter()
    {
        Replacefilter filter = new Replacefilter( this );
        m_replacefilters.add( filter );
        return filter;
    }

    /**
     * Do the execution.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        validateAttributes();

        if( m_propertyFile != null )
        {
            m_properties = getProperties( m_propertyFile );
        }

        validateReplacefilters();
        m_fileCount = 0;
        m_replaceCount = 0;

        if( m_src != null )
        {
            processFile( m_src );
        }

        if( m_dir != null )
        {
            DirectoryScanner ds = super.getDirectoryScanner( m_dir );
            String[] srcs = ds.getIncludedFiles();

            for( int i = 0; i < srcs.length; i++ )
            {
                File file = new File( m_dir, srcs[ i ] );
                processFile( file );
            }
        }

        if( m_summary )
        {
            getLogger().info( "Replaced " + m_replaceCount + " occurrences in " + m_fileCount + " files." );
        }
    }

    /**
     * Validate attributes provided for this task in .xml build file.
     *
     * @exception TaskException if any supplied attribute is invalid or any
     *      mandatory attribute is missing
     */
    public void validateAttributes()
        throws TaskException
    {
        if( m_src == null && m_dir == null )
        {
            String message = "Either the file or the dir attribute " + "must be specified";
            throw new TaskException( message );
        }
        if( m_propertyFile != null && !m_propertyFile.exists() )
        {
            String message = "Property file " + m_propertyFile.getPath() + " does not exist.";
            throw new TaskException( message );
        }
        if( m_token == null && m_replacefilters.size() == 0 )
        {
            String message = "Either token or a nested replacefilter "
                + "must be specified";
            throw new TaskException( message );
        }
        if( m_token != null && "".equals( m_token.getText() ) )
        {
            String message = "The token attribute must not be an empty string.";
            throw new TaskException( message );
        }
    }

    /**
     * Validate nested elements.
     *
     * @exception TaskException if any supplied attribute is invalid or any
     *      mandatory attribute is missing
     */
    public void validateReplacefilters()
        throws TaskException
    {
        for( int i = 0; i < m_replacefilters.size(); i++ )
        {
            Replacefilter element = (Replacefilter)m_replacefilters.get( i );
            element.validate();
        }
    }

    /**
     * Perform the replacement on the given file. The replacement is performed
     * on a temporary file which then replaces the original file.
     *
     * @param src the source file
     * @exception TaskException Description of Exception
     */
    private void processFile( File src )
        throws TaskException
    {
        if( !src.exists() )
        {
            throw new TaskException( "Replace: source file " + src.getPath() + " doesn't exist" );
        }

        File temp = null;
        try
        {
            temp = File.createTempFile( "rep", ".tmp", src.getParentFile() );
        }
        catch( IOException ioe )
        {
            throw new TaskException( ioe.toString(), ioe );
        }

        Reader reader = null;
        Writer writer = null;
        try
        {
            reader = m_encoding == null ? new FileReader( src )
                : new InputStreamReader( new FileInputStream( src ), m_encoding );
            writer = m_encoding == null ? new FileWriter( temp )
                : new OutputStreamWriter( new FileOutputStream( temp ), m_encoding );

            BufferedReader br = new BufferedReader( reader );
            BufferedWriter bw = new BufferedWriter( writer );

            // read the entire file into a StringBuffer
            //   size of work buffer may be bigger than needed
            //   when multibyte characters exist in the source file
            //   but then again, it might be smaller than needed on
            //   platforms like Windows where length can't be trusted
            int fileLengthInBytes = (int)( src.length() );
            StringBuffer tmpBuf = new StringBuffer( fileLengthInBytes );
            int readChar = 0;
            int totread = 0;
            while( true )
            {
                readChar = br.read();
                if( readChar < 0 )
                {
                    break;
                }
                tmpBuf.append( (char)readChar );
                totread++;
            }

            // create a String so we can use indexOf
            String buf = tmpBuf.toString();

            //Preserve original string (buf) so we can compare the result
            String newString = new String( buf );

            if( m_token != null )
            {
                // line separators in values and tokens are "\n"
                // in order to compare with the file contents, replace them
                // as needed
                final String val = stringReplace( m_value.getText(), "\n", StringUtil.LINE_SEPARATOR );
                final String tok = stringReplace( m_token.getText(), "\n", StringUtil.LINE_SEPARATOR );

                // for each found token, replace with value
                getLogger().debug( "Replacing in " + src.getPath() + ": " + m_token.getText() + " --> " + m_value.getText() );
                newString = stringReplace( newString, tok, val );
            }

            if( m_replacefilters.size() > 0 )
            {
                newString = processReplacefilters( newString, src.getPath() );
            }

            boolean changes = !newString.equals( buf );
            if( changes )
            {
                bw.write( newString, 0, newString.length() );
                bw.flush();
            }

            // cleanup
            bw.close();
            writer = null;
            br.close();
            reader = null;

            // If there were changes, move the new one to the old one;
            // otherwise, delete the new one
            if( changes )
            {
                ++m_fileCount;
                src.delete();
                temp.renameTo( src );
                temp = null;
            }
        }
        catch( IOException ioe )
        {
            throw new TaskException( "IOException in " + src + " - " +
                                     ioe.getClass().getName() + ":" + ioe.getMessage(), ioe );
        }
        finally
        {
            IOUtil.shutdownReader( reader );
            IOUtil.shutdownWriter( writer );
            if( temp != null )
            {
                temp.delete();
            }
        }

    }

    private String processReplacefilters( String buffer, String filename )
    {
        String newString = new String( buffer );

        for( int i = 0; i < m_replacefilters.size(); i++ )
        {
            Replacefilter filter = (Replacefilter)m_replacefilters.get( i );

            //for each found token, replace with value
            getLogger().debug( "Replacing in " + filename + ": " + filter.getToken() + " --> " + filter.getReplaceValue() );
            newString = stringReplace( newString, filter.getToken(), filter.getReplaceValue() );
        }

        return newString;
    }

    /**
     * Replace occurrences of str1 in string str with str2
     */
    private String stringReplace( String str, String str1, String str2 )
    {
        StringBuffer ret = new StringBuffer();
        int start = 0;
        int found = str.indexOf( str1 );
        while( found >= 0 )
        {
            // write everything up to the found str1
            if( found > start )
            {
                ret.append( str.substring( start, found ) );
            }

            // write the replacement str2
            if( str2 != null )
            {
                ret.append( str2 );
            }

            // search again
            start = found + str1.length();
            found = str.indexOf( str1, start );
            ++m_replaceCount;
        }

        // write the remaining characters
        if( str.length() > start )
        {
            ret.append( str.substring( start, str.length() ) );
        }

        return ret.toString();
    }

    public NestedString getValue()
    {
        return m_value;
    }

    public File getPropertyFile()
    {
        return m_propertyFile;
    }

    public Properties getProperties()
    {
        return m_properties;
    }
}
