/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Vector;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.AttributeList;
import org.xml.sax.DocumentHandler;
import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Configures a Project (complete with Targets and Tasks) based on a XML build
 * file.
 *
 * @author duncan@x180.com
 */

public class ProjectHelper
{

    private static SAXParserFactory parserFactory = null;
    private File buildFile;
    private File buildFileParent;
    private Locator locator;

    private org.xml.sax.Parser parser;
    private Project project;

    /**
     * Constructs a new Ant parser for the specified XML file.
     *
     * @param project Description of Parameter
     * @param buildFile Description of Parameter
     */
    private ProjectHelper( Project project, File buildFile )
    {
        this.project = project;
        this.buildFile = new File( buildFile.getAbsolutePath() );
        buildFileParent = new File( this.buildFile.getParent() );
    }

    /**
     * Adds the content of #PCDATA sections to an element.
     *
     * @param project The feature to be added to the Text attribute
     * @param target The feature to be added to the Text attribute
     * @param buf The feature to be added to the Text attribute
     * @param start The feature to be added to the Text attribute
     * @param end The feature to be added to the Text attribute
     * @exception BuildException Description of Exception
     */
    public static void addText( Project project, Object target, char[] buf, int start, int end )
        throws BuildException
    {
        addText( project, target, new String( buf, start, end ) );
    }

    /**
     * Adds the content of #PCDATA sections to an element.
     *
     * @param project The feature to be added to the Text attribute
     * @param target The feature to be added to the Text attribute
     * @param text The feature to be added to the Text attribute
     * @exception BuildException Description of Exception
     */
    public static void addText( Project project, Object target, String text )
        throws BuildException
    {

        if( text == null || text.trim().length() == 0 )
        {
            return;
        }

        if( target instanceof TaskAdapter )
            target = ( ( TaskAdapter )target ).getProxy();

        IntrospectionHelper.getHelper( target.getClass() ).addText( project, target, text );
    }

    public static void configure( Object target, AttributeList attrs,
                                  Project project )
        throws BuildException
    {
        if( target instanceof TaskAdapter )
            target = ( ( TaskAdapter )target ).getProxy();

        IntrospectionHelper ih =
            IntrospectionHelper.getHelper( target.getClass() );

        project.addBuildListener( ih );

        for( int i = 0; i < attrs.getLength(); i++ )
        {
            // reflect these into the target
            String value = replaceProperties( project, attrs.getValue( i ),
                project.getProperties() );
            try
            {
                ih.setAttribute( project, target,
                    attrs.getName( i ).toLowerCase( Locale.US ), value );

            }
            catch( BuildException be )
            {
                // id attribute must be set externally
                if( !attrs.getName( i ).equals( "id" ) )
                {
                    throw be;
                }
            }
        }
    }

    /**
     * Configures the Project with the contents of the specified XML file.
     *
     * @param project Description of Parameter
     * @param buildFile Description of Parameter
     * @exception BuildException Description of Exception
     */
    public static void configureProject( Project project, File buildFile )
        throws BuildException
    {
        new ProjectHelper( project, buildFile ).parse();
    }

    /**
     * This method will parse a string containing ${value} style property values
     * into two lists. The first list is a collection of text fragments, while
     * the other is a set of string property names null entries in the first
     * list indicate a property reference from the second list.
     *
     * @param value Description of Parameter
     * @param fragments Description of Parameter
     * @param propertyRefs Description of Parameter
     * @exception BuildException Description of Exception
     */
    public static void parsePropertyString( String value, Vector fragments, Vector propertyRefs )
        throws BuildException
    {
        int prev = 0;
        int pos;
        while( ( pos = value.indexOf( "$", prev ) ) >= 0 )
        {
            if( pos > 0 )
            {
                fragments.addElement( value.substring( prev, pos ) );
            }

            if( pos == ( value.length() - 1 ) )
            {
                fragments.addElement( "$" );
                prev = pos + 1;
            }
            else if( value.charAt( pos + 1 ) != '{' )
            {
                fragments.addElement( value.substring( pos + 1, pos + 2 ) );
                prev = pos + 2;
            }
            else
            {
                int endName = value.indexOf( '}', pos );
                if( endName < 0 )
                {
                    throw new BuildException( "Syntax error in property: "
                         + value );
                }
                String propertyName = value.substring( pos + 2, endName );
                fragments.addElement( null );
                propertyRefs.addElement( propertyName );
                prev = endName + 1;
            }
        }

        if( prev < value.length() )
        {
            fragments.addElement( value.substring( prev ) );
        }
    }

    /**
     * Replace ${} style constructions in the given value with the string value
     * of the corresponding data types.
     *
     * @param value the string to be scanned for property references.
     * @param project Description of Parameter
     * @return Description of the Returned Value
     * @exception BuildException Description of Exception
     * @since 1.5
     */
    public static String replaceProperties( Project project, String value )
        throws BuildException
    {
        return replaceProperties( project, value, project.getProperties() );
    }

    /**
     * Replace ${} style constructions in the given value with the string value
     * of the corresponding data types.
     *
     * @param value the string to be scanned for property references.
     * @param project Description of Parameter
     * @param keys Description of Parameter
     * @return Description of the Returned Value
     * @exception BuildException Description of Exception
     */
    public static String replaceProperties( Project project, String value, Hashtable keys )
        throws BuildException
    {
        if( value == null )
        {
            return null;
        }

        Vector fragments = new Vector();
        Vector propertyRefs = new Vector();
        parsePropertyString( value, fragments, propertyRefs );

        StringBuffer sb = new StringBuffer();
        Enumeration i = fragments.elements();
        Enumeration j = propertyRefs.elements();
        while( i.hasMoreElements() )
        {
            String fragment = ( String )i.nextElement();
            if( fragment == null )
            {
                String propertyName = ( String )j.nextElement();
                if( !keys.containsKey( propertyName ) )
                {
                    project.log( "Property ${" + propertyName + "} has not been set", Project.MSG_VERBOSE );
                }
                fragment = ( keys.containsKey( propertyName ) ) ? ( String )keys.get( propertyName )
                     : "${" + propertyName + "}";
            }
            sb.append( fragment );
        }

        return sb.toString();
    }

    /**
     * Stores a configured child element into its parent object
     *
     * @param project Description of Parameter
     * @param parent Description of Parameter
     * @param child Description of Parameter
     * @param tag Description of Parameter
     */
    public static void storeChild( Project project, Object parent, Object child, String tag )
    {
        IntrospectionHelper ih = IntrospectionHelper.getHelper( parent.getClass() );
        ih.storeElement( project, parent, child, tag );
    }

    private static SAXParserFactory getParserFactory()
    {
        if( parserFactory == null )
        {
            parserFactory = SAXParserFactory.newInstance();
        }

        return parserFactory;
    }

    /**
     * Scan AttributeList for the id attribute and maybe add a reference to
     * project. <p>
     *
     * Moved out of {@link #configure configure} to make it happen at parser
     * time.</p>
     *
     * @param target Description of Parameter
     * @param attr Description of Parameter
     */
    private void configureId( Object target, AttributeList attr )
    {
        String id = attr.getValue( "id" );
        if( id != null )
        {
            project.addReference( id, target );
        }
    }

    /**
     * Parses the project file.
     *
     * @exception BuildException Description of Exception
     */
    private void parse()
        throws BuildException
    {
        FileInputStream inputStream = null;
        InputSource inputSource = null;

        try
        {
            SAXParser saxParser = getParserFactory().newSAXParser();
            parser = saxParser.getParser();

            String uri = "file:" + buildFile.getAbsolutePath().replace( '\\', '/' );
            for( int index = uri.indexOf( '#' ); index != -1; index = uri.indexOf( '#' ) )
            {
                uri = uri.substring( 0, index ) + "%23" + uri.substring( index + 1 );
            }

            inputStream = new FileInputStream( buildFile );
            inputSource = new InputSource( inputStream );
            inputSource.setSystemId( uri );
            project.log( "parsing buildfile " + buildFile + " with URI = " + uri, Project.MSG_VERBOSE );
            saxParser.parse( inputSource, new RootHandler() );
        }
        catch( ParserConfigurationException exc )
        {
            throw new BuildException( "Parser has not been configured correctly", exc );
        }
        catch( SAXParseException exc )
        {
            Location location =
                new Location( buildFile.toString(), exc.getLineNumber(), exc.getColumnNumber() );

            Throwable t = exc.getException();
            if( t instanceof BuildException )
            {
                BuildException be = ( BuildException )t;
                if( be.getLocation() == Location.UNKNOWN_LOCATION )
                {
                    be.setLocation( location );
                }
                throw be;
            }

            throw new BuildException( exc.getMessage(), t, location );
        }
        catch( SAXException exc )
        {
            Throwable t = exc.getException();
            if( t instanceof BuildException )
            {
                throw ( BuildException )t;
            }
            throw new BuildException( exc.getMessage(), t );
        }
        catch( FileNotFoundException exc )
        {
            throw new BuildException( exc );
        }
        catch( IOException exc )
        {
            throw new BuildException( "Error reading project file", exc );
        }
        finally
        {
            if( inputStream != null )
            {
                try
                {
                    inputStream.close();
                }
                catch( IOException ioe )
                {
                    // ignore this
                }
            }
        }
    }

    /**
     * The common superclass for all sax event handlers in Ant. Basically throws
     * an exception in each method, so subclasses should override what they can
     * handle. Each type of xml element (task, target, etc) in ant will have its
     * own subclass of AbstractHandler. In the constructor, this class takes
     * over the handling of sax events from the parent handler, and returns
     * control back to the parent in the endElement method.
     *
     * @author RT
     */
    private class AbstractHandler extends HandlerBase
    {
        protected DocumentHandler parentHandler;

        public AbstractHandler( DocumentHandler parentHandler )
        {
            this.parentHandler = parentHandler;

            // Start handling SAX events
            parser.setDocumentHandler( this );
        }

        public void characters( char[] buf, int start, int end )
            throws SAXParseException
        {
            String s = new String( buf, start, end ).trim();

            if( s.length() > 0 )
            {
                throw new SAXParseException( "Unexpected text \"" + s + "\"", locator );
            }
        }

        public void endElement( String name )
            throws SAXException
        {

            finished();
            // Let parent resume handling SAX events
            parser.setDocumentHandler( parentHandler );
        }

        public void startElement( String tag, AttributeList attrs )
            throws SAXParseException
        {
            throw new SAXParseException( "Unexpected element \"" + tag + "\"", locator );
        }

        /**
         * Called when this element and all elements nested into it have been
         * handled.
         */
        protected void finished() { }
    }

    /**
     * Handler for all data types at global level.
     *
     * @author RT
     */
    private class DataTypeHandler extends AbstractHandler
    {
        private RuntimeConfigurable wrapper = null;
        private Object element;
        private Target target;

        public DataTypeHandler( DocumentHandler parentHandler )
        {
            this( parentHandler, null );
        }

        public DataTypeHandler( DocumentHandler parentHandler, Target target )
        {
            super( parentHandler );
            this.target = target;
        }

        public void characters( char[] buf, int start, int end )
            throws SAXParseException
        {
            try
            {
                addText( project, element, buf, start, end );
            }
            catch( BuildException exc )
            {
                throw new SAXParseException( exc.getMessage(), locator, exc );
            }
        }

        public void init( String propType, AttributeList attrs )
            throws SAXParseException
        {
            try
            {
                element = project.createDataType( propType );
                if( element == null )
                {
                    throw new BuildException( "Unknown data type " + propType );
                }

                if( target != null )
                {
                    wrapper = new RuntimeConfigurable( element, propType );
                    wrapper.setAttributes( attrs );
                    target.addDataType( wrapper );
                }
                else
                {
                    configure( element, attrs, project );
                    configureId( element, attrs );
                }
            }
            catch( BuildException exc )
            {
                throw new SAXParseException( exc.getMessage(), locator, exc );
            }
        }

        public void startElement( String name, AttributeList attrs )
            throws SAXParseException
        {
            new NestedElementHandler( this, element, wrapper, target ).init( name, attrs );
        }
    }

    /**
     * Handler for all nested properties.
     *
     * @author RT
     */
    private class NestedElementHandler extends AbstractHandler
    {
        private RuntimeConfigurable childWrapper = null;
        private Object child;
        private Object parent;
        private RuntimeConfigurable parentWrapper;
        private Target target;

        public NestedElementHandler( DocumentHandler parentHandler,
                                     Object parent,
                                     RuntimeConfigurable parentWrapper,
                                     Target target )
        {
            super( parentHandler );

            if( parent instanceof TaskAdapter )
            {
                this.parent = ( ( TaskAdapter )parent ).getProxy();
            }
            else
            {
                this.parent = parent;
            }
            this.parentWrapper = parentWrapper;
            this.target = target;
        }

        public void characters( char[] buf, int start, int end )
            throws SAXParseException
        {
            if( parentWrapper == null )
            {
                try
                {
                    addText( project, child, buf, start, end );
                }
                catch( BuildException exc )
                {
                    throw new SAXParseException( exc.getMessage(), locator, exc );
                }
            }
            else
            {
                childWrapper.addText( buf, start, end );
            }
        }

        public void init( String propType, AttributeList attrs )
            throws SAXParseException
        {
            Class parentClass = parent.getClass();
            IntrospectionHelper ih =
                IntrospectionHelper.getHelper( parentClass );

            try
            {
                String elementName = propType.toLowerCase( Locale.US );
                if( parent instanceof UnknownElement )
                {
                    UnknownElement uc = new UnknownElement( elementName );
                    uc.setProject( project );
                    ( ( UnknownElement )parent ).addChild( uc );
                    child = uc;
                }
                else
                {
                    child = ih.createElement( project, parent, elementName );
                }

                configureId( child, attrs );

                if( parentWrapper != null )
                {
                    childWrapper = new RuntimeConfigurable( child, propType );
                    childWrapper.setAttributes( attrs );
                    parentWrapper.addChild( childWrapper );
                }
                else
                {
                    configure( child, attrs, project );
                    ih.storeElement( project, parent, child, elementName );
                }
            }
            catch( BuildException exc )
            {
                throw new SAXParseException( exc.getMessage(), locator, exc );
            }
        }

        public void startElement( String name, AttributeList attrs )
            throws SAXParseException
        {
            if( child instanceof TaskContainer )
            {
                // taskcontainer nested element can contain other tasks - no other
                // nested elements possible
                new TaskHandler( this, ( TaskContainer )child, childWrapper, target ).init( name, attrs );
            }
            else
            {
                new NestedElementHandler( this, child, childWrapper, target ).init( name, attrs );
            }
        }
    }

    /**
     * Handler for the top level "project" element.
     *
     * @author RT
     */
    private class ProjectHandler extends AbstractHandler
    {
        public ProjectHandler( DocumentHandler parentHandler )
        {
            super( parentHandler );
        }

        public void init( String tag, AttributeList attrs )
            throws SAXParseException
        {
            String def = null;
            String name = null;
            String id = null;
            String baseDir = null;

            for( int i = 0; i < attrs.getLength(); i++ )
            {
                String key = attrs.getName( i );
                String value = attrs.getValue( i );

                if( key.equals( "default" ) )
                {
                    def = value;
                }
                else if( key.equals( "name" ) )
                {
                    name = value;
                }
                else if( key.equals( "id" ) )
                {
                    id = value;
                }
                else if( key.equals( "basedir" ) )
                {
                    baseDir = value;
                }
                else
                {
                    throw new SAXParseException( "Unexpected attribute \"" + attrs.getName( i ) + "\"", locator );
                }
            }

            if( def == null )
            {
                throw new SAXParseException( "The default attribute of project is required",
                    locator );
            }

            project.setDefaultTarget( def );

            if( name != null )
            {
                project.setName( name );
                project.addReference( name, project );
            }

            if( id != null )
                project.addReference( id, project );

            if( project.getProperty( "basedir" ) != null )
            {
                project.setBasedir( project.getProperty( "basedir" ) );
            }
            else
            {
                if( baseDir == null )
                {
                    project.setBasedir( buildFileParent.getAbsolutePath() );
                }
                else
                {
                    // check whether the user has specified an absolute path
                    if( ( new File( baseDir ) ).isAbsolute() )
                    {
                        project.setBasedir( baseDir );
                    }
                    else
                    {
                        project.setBaseDir( project.resolveFile( baseDir, buildFileParent ) );
                    }
                }
            }

        }

        public void startElement( String name, AttributeList attrs )
            throws SAXParseException
        {
            if( name.equals( "taskdef" ) )
            {
                handleTaskdef( name, attrs );
            }
            else if( name.equals( "typedef" ) )
            {
                handleTypedef( name, attrs );
            }
            else if( name.equals( "property" ) )
            {
                handleProperty( name, attrs );
            }
            else if( name.equals( "target" ) )
            {
                handleTarget( name, attrs );
            }
            else if( project.getDataTypeDefinitions().get( name ) != null )
            {
                handleDataType( name, attrs );
            }
            else
            {
                throw new SAXParseException( "Unexpected element \"" + name + "\"", locator );
            }
        }

        private void handleDataType( String name, AttributeList attrs )
            throws SAXParseException
        {
            new DataTypeHandler( this ).init( name, attrs );
        }

        private void handleProperty( String name, AttributeList attrs )
            throws SAXParseException
        {
            ( new TaskHandler( this, null, null, null ) ).init( name, attrs );
        }

        private void handleTarget( String tag, AttributeList attrs )
            throws SAXParseException
        {
            new TargetHandler( this ).init( tag, attrs );
        }

        private void handleTaskdef( String name, AttributeList attrs )
            throws SAXParseException
        {
            ( new TaskHandler( this, null, null, null ) ).init( name, attrs );
        }

        private void handleTypedef( String name, AttributeList attrs )
            throws SAXParseException
        {
            ( new TaskHandler( this, null, null, null ) ).init( name, attrs );
        }

    }

    /**
     * Handler for the root element. It's only child must be the "project"
     * element.
     *
     * @author RT
     */
    private class RootHandler extends HandlerBase
    {

        public void setDocumentLocator( Locator locator )
        {
            ProjectHelper.this.locator = locator;
        }

        /**
         * resolve file: URIs as relative to the build file.
         *
         * @param publicId Description of Parameter
         * @param systemId Description of Parameter
         * @return Description of the Returned Value
         */
        public InputSource resolveEntity( String publicId,
                                          String systemId )
        {

            project.log( "resolving systemId: " + systemId, Project.MSG_VERBOSE );

            if( systemId.startsWith( "file:" ) )
            {
                String path = systemId.substring( 5 );
                int index = path.indexOf( "file:" );

                // we only have to handle these for backward compatibility
                // since they are in the FAQ.
                while( index != -1 )
                {
                    path = path.substring( 0, index ) + path.substring( index + 5 );
                    index = path.indexOf( "file:" );
                }

                String entitySystemId = path;
                index = path.indexOf( "%23" );
                // convert these to #
                while( index != -1 )
                {
                    path = path.substring( 0, index ) + "#" + path.substring( index + 3 );
                    index = path.indexOf( "%23" );
                }

                File file = new File( path );
                if( !file.isAbsolute() )
                {
                    file = new File( buildFileParent, path );
                }

                try
                {
                    InputSource inputSource = new InputSource( new FileInputStream( file ) );
                    inputSource.setSystemId( "file:" + entitySystemId );
                    return inputSource;
                }
                catch( FileNotFoundException fne )
                {
                    project.log( file.getAbsolutePath() + " could not be found",
                        Project.MSG_WARN );
                }
            }
            // use default if not file or file not found
            return null;
        }

        public void startElement( String tag, AttributeList attrs )
            throws SAXParseException
        {
            if( tag.equals( "project" ) )
            {
                new ProjectHandler( this ).init( tag, attrs );
            }
            else
            {
                throw new SAXParseException( "Config file is not of expected XML type", locator );
            }
        }
    }

    /**
     * Handler for "target" elements.
     *
     * @author RT
     */
    private class TargetHandler extends AbstractHandler
    {
        private Target target;

        public TargetHandler( DocumentHandler parentHandler )
        {
            super( parentHandler );
        }

        public void init( String tag, AttributeList attrs )
            throws SAXParseException
        {
            String name = null;
            String depends = "";
            String ifCond = null;
            String unlessCond = null;
            String id = null;
            String description = null;

            for( int i = 0; i < attrs.getLength(); i++ )
            {
                String key = attrs.getName( i );
                String value = attrs.getValue( i );

                if( key.equals( "name" ) )
                {
                    name = value;
                }
                else if( key.equals( "depends" ) )
                {
                    depends = value;
                }
                else if( key.equals( "if" ) )
                {
                    ifCond = value;
                }
                else if( key.equals( "unless" ) )
                {
                    unlessCond = value;
                }
                else if( key.equals( "id" ) )
                {
                    id = value;
                }
                else if( key.equals( "description" ) )
                {
                    description = value;
                }
                else
                {
                    throw new SAXParseException( "Unexpected attribute \"" + key + "\"", locator );
                }
            }

            if( name == null )
            {
                throw new SAXParseException( "target element appears without a name attribute", locator );
            }

            target = new Target();
            target.setName( name );
            target.setIf( ifCond );
            target.setUnless( unlessCond );
            target.setDescription( description );
            project.addTarget( name, target );

            if( id != null && !id.equals( "" ) )
                project.addReference( id, target );

            // take care of dependencies

            if( depends.length() > 0 )
            {
                target.setDepends( depends );
            }
        }

        public void startElement( String name, AttributeList attrs )
            throws SAXParseException
        {
            if( project.getDataTypeDefinitions().get( name ) != null )
            {
                new DataTypeHandler( this, target ).init( name, attrs );
            }
            else
            {
                new TaskHandler( this, target, null, target ).init( name, attrs );
            }
        }
    }

    /**
     * Handler for all task elements.
     *
     * @author RT
     */
    private class TaskHandler extends AbstractHandler
    {
        private RuntimeConfigurable wrapper = null;
        private TaskContainer container;
        private RuntimeConfigurable parentWrapper;
        private Target target;
        private Task task;

        public TaskHandler( DocumentHandler parentHandler, TaskContainer container, RuntimeConfigurable parentWrapper, Target target )
        {
            super( parentHandler );
            this.container = container;
            this.parentWrapper = parentWrapper;
            this.target = target;
        }

        public void characters( char[] buf, int start, int end )
            throws SAXParseException
        {
            if( wrapper == null )
            {
                try
                {
                    addText( project, task, buf, start, end );
                }
                catch( BuildException exc )
                {
                    throw new SAXParseException( exc.getMessage(), locator, exc );
                }
            }
            else
            {
                wrapper.addText( buf, start, end );
            }
        }

        public void init( String tag, AttributeList attrs )
            throws SAXParseException
        {
            try
            {
                task = project.createTask( tag );
            }
            catch( BuildException e )
            {
                // swallow here, will be thrown again in
                // UnknownElement.maybeConfigure if the problem persists.
            }

            if( task == null )
            {
                task = new UnknownElement( tag );
                task.setProject( project );
                task.setTaskType( tag );
                task.setTaskName( tag );
            }

            task.setLocation( new Location( buildFile.toString(), locator.getLineNumber(), locator.getColumnNumber() ) );
            configureId( task, attrs );

            // Top level tasks don't have associated targets
            if( target != null )
            {
                task.setOwningTarget( target );
                container.addTask( task );
                task.init();
                wrapper = task.getRuntimeConfigurableWrapper();
                wrapper.setAttributes( attrs );
                if( parentWrapper != null )
                {
                    parentWrapper.addChild( wrapper );
                }
            }
            else
            {
                task.init();
                configure( task, attrs, project );
            }
        }

        public void startElement( String name, AttributeList attrs )
            throws SAXParseException
        {
            if( task instanceof TaskContainer )
            {
                // task can contain other tasks - no other nested elements possible
                new TaskHandler( this, ( TaskContainer )task, wrapper, target ).init( name, attrs );
            }
            else
            {
                new NestedElementHandler( this, task, wrapper, target ).init( name, attrs );
            }
        }

        protected void finished()
        {
            if( task != null && target == null )
            {
                task.execute();
            }
        }
    }

}
