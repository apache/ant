/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
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
import java.util.Properties;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;

/**
 * Replaces all occurrences of one or more string tokens with given values in
 * the indicated files. Each value can be either a string or the value of a
 * property available in a designated property file.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">
 *      stefano@apache.org</a>
 * @author <a href="mailto:erik@desknetinc.com">Erik Langenbach</a>
 */
public class Replace extends MatchingTask
{

    private File src = null;
    private NestedString token = null;
    private NestedString value = new NestedString();

    private File propertyFile = null;
    private Properties properties = null;
    private Vector replacefilters = new Vector();

    private File dir = null;
    private boolean summary = false;

    /**
     * The encoding used to read and write files - if null, uses default
     */
    private String encoding = null;

    private FileUtils fileUtils = FileUtils.newFileUtils();

    private int fileCount;
    private int replaceCount;


    /**
     * Set the source files path when using matching tasks.
     *
     * @param dir The new Dir value
     */
    public void setDir( File dir )
    {
        this.dir = dir;
    }

    /**
     * Set the file encoding to use on the files read and written by replace
     *
     * @param encoding the encoding to use on the files
     */
    public void setEncoding( String encoding )
    {
        this.encoding = encoding;
    }


    /**
     * Set the source file.
     *
     * @param file The new File value
     */
    public void setFile( File file )
    {
        this.src = file;
    }

    /**
     * Sets a file to be searched for property values.
     *
     * @param filename The new PropertyFile value
     */
    public void setPropertyFile( File filename )
    {
        propertyFile = filename;
    }

    /**
     * Request a summary
     *
     * @param summary true if you would like a summary logged of the replace
     *      operation
     */
    public void setSummary( boolean summary )
    {
        this.summary = summary;
    }

    /**
     * Set the string token to replace.
     *
     * @param token The new Token value
     */
    public void setToken( String token )
    {
        createReplaceToken().addText( token );
    }

    /**
     * Set the string value to use as token replacement.
     *
     * @param value The new Value value
     */
    public void setValue( String value )
    {
        createReplaceValue().addText( value );
    }

    public Properties getProperties( File propertyFile )
        throws BuildException
    {
        Properties properties = new Properties();

        try
        {
            properties.load( new FileInputStream( propertyFile ) );
        }
        catch( FileNotFoundException e )
        {
            String message = "Property file (" + propertyFile.getPath() + ") not found.";
            throw new BuildException( message );
        }
        catch( IOException e )
        {
            String message = "Property file (" + propertyFile.getPath() + ") cannot be loaded.";
            throw new BuildException( message );
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
        if( token == null )
        {
            token = new NestedString();
        }
        return token;
    }

    /**
     * Nested &lt;replacevalue&gt; element.
     *
     * @return Description of the Returned Value
     */
    public NestedString createReplaceValue()
    {
        return value;
    }

    /**
     * Add nested &lt;replacefilter&gt; element.
     *
     * @return Description of the Returned Value
     */
    public Replacefilter createReplacefilter()
    {
        Replacefilter filter = new Replacefilter();
        replacefilters.addElement( filter );
        return filter;
    }

    /**
     * Do the execution.
     *
     * @exception BuildException Description of Exception
     */
    public void execute()
        throws BuildException
    {
        validateAttributes();

        if( propertyFile != null )
        {
            properties = getProperties( propertyFile );
        }

        validateReplacefilters();
        fileCount = 0;
        replaceCount = 0;

        if( src != null )
        {
            processFile( src );
        }

        if( dir != null )
        {
            DirectoryScanner ds = super.getDirectoryScanner( dir );
            String[] srcs = ds.getIncludedFiles();

            for( int i = 0; i < srcs.length; i++ )
            {
                File file = new File( dir, srcs[i] );
                processFile( file );
            }
        }

        if( summary )
        {
            log( "Replaced " + replaceCount + " occurrences in " + fileCount + " files.", Project.MSG_INFO );
        }
    }

    /**
     * Validate attributes provided for this task in .xml build file.
     *
     * @exception BuildException if any supplied attribute is invalid or any
     *      mandatory attribute is missing
     */
    public void validateAttributes()
        throws BuildException
    {
        if( src == null && dir == null )
        {
            String message = "Either the file or the dir attribute " + "must be specified";
            throw new BuildException( message );
        }
        if( propertyFile != null && !propertyFile.exists() )
        {
            String message = "Property file " + propertyFile.getPath() + " does not exist.";
            throw new BuildException( message );
        }
        if( token == null && replacefilters.size() == 0 )
        {
            String message = "Either token or a nested replacefilter "
                 + "must be specified";
            throw new BuildException( message);
        }
        if( token != null && "".equals( token.getText() ) )
        {
            String message = "The token attribute must not be an empty string.";
            throw new BuildException( message );
        }
    }

    /**
     * Validate nested elements.
     *
     * @exception BuildException if any supplied attribute is invalid or any
     *      mandatory attribute is missing
     */
    public void validateReplacefilters()
        throws BuildException
    {
        for( int i = 0; i < replacefilters.size(); i++ )
        {
            Replacefilter element = ( Replacefilter )replacefilters.elementAt( i );
            element.validate();
        }
    }

    /**
     * Perform the replacement on the given file. The replacement is performed
     * on a temporary file which then replaces the original file.
     *
     * @param src the source file
     * @exception BuildException Description of Exception
     */
    private void processFile( File src )
        throws BuildException
    {
        if( !src.exists() )
        {
            throw new BuildException( "Replace: source file " + src.getPath() + " doesn't exist" );
        }

        File temp = fileUtils.createTempFile( "rep", ".tmp",
            fileUtils.getParentFile( src ) );

        Reader reader = null;
        Writer writer = null;
        try
        {
            reader = encoding == null ? new FileReader( src )
                 : new InputStreamReader( new FileInputStream( src ), encoding );
            writer = encoding == null ? new FileWriter( temp )
                 : new OutputStreamWriter( new FileOutputStream( temp ), encoding );

            BufferedReader br = new BufferedReader( reader );
            BufferedWriter bw = new BufferedWriter( writer );

            // read the entire file into a StringBuffer
            //   size of work buffer may be bigger than needed
            //   when multibyte characters exist in the source file
            //   but then again, it might be smaller than needed on
            //   platforms like Windows where length can't be trusted
            int fileLengthInBytes = ( int )( src.length() );
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
                tmpBuf.append( ( char )readChar );
                totread++;
            }

            // create a String so we can use indexOf
            String buf = tmpBuf.toString();

            //Preserve original string (buf) so we can compare the result
            String newString = new String( buf );

            if( token != null )
            {
                // line separators in values and tokens are "\n"
                // in order to compare with the file contents, replace them
                // as needed
                String linesep = System.getProperty( "line.separator" );
                String val = stringReplace( value.getText(), "\n", linesep );
                String tok = stringReplace( token.getText(), "\n", linesep );

                // for each found token, replace with value
                log( "Replacing in " + src.getPath() + ": " + token.getText() + " --> " + value.getText(), Project.MSG_VERBOSE );
                newString = stringReplace( newString, tok, val );
            }

            if( replacefilters.size() > 0 )
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
                ++fileCount;
                src.delete();
                temp.renameTo( src );
                temp = null;
            }
        }
        catch( IOException ioe )
        {
            throw new BuildException( "IOException in " + src + " - " +
                ioe.getClass().getName() + ":" + ioe.getMessage(), ioe );
        }
        finally
        {
            if( reader != null )
            {
                try
                {
                    reader.close();
                }
                catch( IOException e )
                {}
            }
            if( writer != null )
            {
                try
                {
                    writer.close();
                }
                catch( IOException e )
                {}
            }
            if( temp != null )
            {
                temp.delete();
            }
        }

    }

    private String processReplacefilters( String buffer, String filename )
    {
        String newString = new String( buffer );

        for( int i = 0; i < replacefilters.size(); i++ )
        {
            Replacefilter filter = ( Replacefilter )replacefilters.elementAt( i );

            //for each found token, replace with value
            log( "Replacing in " + filename + ": " + filter.getToken() + " --> " + filter.getReplaceValue(), Project.MSG_VERBOSE );
            newString = stringReplace( newString, filter.getToken(), filter.getReplaceValue() );
        }

        return newString;
    }

    /**
     * Replace occurrences of str1 in string str with str2
     *
     * @param str Description of Parameter
     * @param str1 Description of Parameter
     * @param str2 Description of Parameter
     * @return Description of the Returned Value
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
            ++replaceCount;
        }

        // write the remaining characters
        if( str.length() > start )
        {
            ret.append( str.substring( start, str.length() ) );
        }

        return ret.toString();
    }

    //Inner class
    public class NestedString
    {

        private StringBuffer buf = new StringBuffer();

        public String getText()
        {
            return buf.toString();
        }

        public void addText( String val )
        {
            buf.append( val );
        }
    }

    //Inner class
    public class Replacefilter
    {
        private String property;
        private String token;
        private String value;

        public void setProperty( String property )
        {
            this.property = property;
        }

        public void setToken( String token )
        {
            this.token = token;
        }

        public void setValue( String value )
        {
            this.value = value;
        }

        public String getProperty()
        {
            return property;
        }

        public String getReplaceValue()
        {
            if( property != null )
            {
                return ( String )properties.getProperty( property );
            }
            else if( value != null )
            {
                return value;
            }
            else if( Replace.this.value != null )
            {
                return Replace.this.value.getText();
            }
            else
            {
                //Default is empty string
                return new String( "" );
            }
        }

        public String getToken()
        {
            return token;
        }

        public String getValue()
        {
            return value;
        }

        public void validate()
            throws BuildException
        {
            //Validate mandatory attributes
            if( token == null )
            {
                String message = "token is a mandatory attribute " + "of replacefilter.";
                throw new BuildException( message );
            }

            if( "".equals( token ) )
            {
                String message = "The token attribute must not be an empty string.";
                throw new BuildException( message );
            }

            //value and property are mutually exclusive attributes
            if( ( value != null ) && ( property != null ) )
            {
                String message = "Either value or property " + "can be specified, but a replacefilter " + "element cannot have both.";
                throw new BuildException( message );
            }

            if( ( property != null ) )
            {
                //the property attribute must have access to a property file
                if( propertyFile == null )
                {
                    String message = "The replacefilter's property attribute " + "can only be used with the replacetask's " + "propertyFile attribute.";
                    throw new BuildException( message );
                }

                //Make sure property exists in property file
                if( properties == null ||
                    properties.getProperty( property ) == null )
                {
                    String message = "property \"" + property + "\" was not found in " + propertyFile.getPath();
                    throw new BuildException( message );
                }
            }
        }
    }

}
