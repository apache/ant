/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.sitraka;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.optional.sitraka.bytecode.ClassFile;
import org.apache.tools.ant.taskdefs.optional.sitraka.bytecode.ClassPathLoader;
import org.apache.tools.ant.taskdefs.optional.sitraka.bytecode.MethodInfo;
import org.apache.tools.ant.taskdefs.optional.sitraka.bytecode.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Little hack to process XML report from JProbe. It will fix some reporting
 * errors from JProbe 3.0 and makes use of a reference classpath to add
 * classes/methods that were not reported by JProbe as being used (ie loaded)
 *
 * @author <a href="sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class XMLReport
{

    /**
     * mapping of class names to <code>ClassFile</code>s from the reference
     * classpath. It is used to filter the JProbe report.
     */
    protected Hashtable classFiles;

    /**
     * mapping classname / class node for faster access
     */
    protected Hashtable classMap;

    /**
     * the XML file to process just from CovReport
     */
    protected File file;

    /**
     * method filters
     */
    protected ReportFilters filters;

    /**
     * jprobe home path. It is used to get the DTD
     */
    protected File jprobeHome;

    /**
     * mapping package name / package node for faster access
     */
    protected Hashtable pkgMap;

    /**
     * parsed document
     */
    protected Document report;
    /**
     * task caller, can be null, used for logging purpose
     */
    protected Task task;

    /**
     * create a new XML report, logging will be on stdout
     *
     * @param file Description of Parameter
     */
    public XMLReport( File file )
    {
        this( null, file );
    }

    /**
     * create a new XML report, logging done on the task
     *
     * @param task Description of Parameter
     * @param file Description of Parameter
     */
    public XMLReport( Task task, File file )
    {
        this.file = file;
        this.task = task;
    }

    private static DocumentBuilder newBuilder()
    {
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments( true );
            factory.setValidating( false );
            return factory.newDocumentBuilder();
        }
        catch( Exception e )
        {
            throw new ExceptionInInitializerError( e );
        }
    }

    /**
     * set the JProbe home path. Used to get the DTD
     *
     * @param home The new JProbehome value
     */
    public void setJProbehome( File home )
    {
        jprobeHome = home;
    }

    /**
     * set the
     *
     * @param filters The new ReportFilters value
     */
    public void setReportFilters( ReportFilters filters )
    {
        this.filters = filters;
    }

    /**
     * create the whole new document
     *
     * @param classPath Description of Parameter
     * @return Description of the Returned Value
     * @exception Exception Description of Exception
     */
    public Document createDocument( String[] classPath )
        throws Exception
    {

        // Iterate over the classpath to identify reference classes
        classFiles = new Hashtable();
        ClassPathLoader cpl = new ClassPathLoader( classPath );
        Iterator enum = cpl.loaders();
        while( enum.hasNext() )
        {
            ClassPathLoader.FileLoader fl = (ClassPathLoader.FileLoader)enum.next();
            ClassFile[] classes = fl.getClasses();
            log( "Processing " + classes.length + " classes in " + fl.getFile() );
            // process all classes
            for( int i = 0; i < classes.length; i++ )
            {
                classFiles.put( classes[ i ].getFullName(), classes[ i ] );
            }
        }

        // Load the JProbe coverage XML report
        DocumentBuilder dbuilder = newBuilder();
        InputSource is = new InputSource( new FileInputStream( file ) );
        if( jprobeHome != null )
        {
            File dtdDir = new File( jprobeHome, "dtd" );
            is.setSystemId( "file:///" + dtdDir.getAbsolutePath() + "/" );
        }
        report = dbuilder.parse( is );
        report.normalize();

        // create maps for faster node access (also filters out unwanted nodes)
        createNodeMaps();

        // Make sure each class from the reference path ends up in the report
        Iterator classes = classFiles.iterator();
        while( classes.hasNext() )
        {
            ClassFile cf = (ClassFile)classes.next();
            serializeClass( cf );
        }
        // update the document with the stats
        update();
        return report;
    }

    public void log( String message )
    {
        if( task == null )
        {
            //System.out.println(message);
        }
        else
        {
            task.getLogger().debug( message );
        }
    }

    protected Element[] getClasses( Element pkg )
    {
        ArrayList v = new ArrayList();
        NodeList children = pkg.getChildNodes();
        int len = children.getLength();
        for( int i = 0; i < len; i++ )
        {
            Node child = children.item( i );
            if( child.getNodeType() == Node.ELEMENT_NODE )
            {
                Element elem = (Element)child;
                if( "class".equals( elem.getNodeName() ) )
                {
                    v.add( elem );
                }
            }
        }
        Element[] elems = new Element[ v.size() ];
        v.copyInto( elems );
        return elems;
    }

    protected Element getCovDataChild( Element parent )
    {
        NodeList children = parent.getChildNodes();
        int len = children.getLength();
        for( int i = 0; i < len; i++ )
        {
            Node child = children.item( i );
            if( child.getNodeType() == Node.ELEMENT_NODE )
            {
                Element elem = (Element)child;
                if( "cov.data".equals( elem.getNodeName() ) )
                {
                    return elem;
                }
            }
        }
        throw new NoSuchElementException( "Could not find 'cov.data' element in parent '" + parent.getNodeName() + "'" );
    }

    protected ArrayList getFilteredMethods( ClassFile classFile )
    {
        MethodInfo[] methodlist = classFile.getMethods();
        ArrayList methods = new ArrayList( methodlist.length );
        for( int i = 0; i < methodlist.length; i++ )
        {
            MethodInfo method = methodlist[ i ];
            String signature = getMethodSignature( classFile, method );
            if( filters.accept( signature ) )
            {
                methods.add( method );
                log( "keeping " + signature );
            }
            else
            {
                //              log("discarding " + signature);
            }
        }
        return methods;
    }

    /**
     * JProbe does not put the java.lang prefix for classes in this package, so
     * used this nice method so that I have the same signature for methods
     *
     * @param method Description of Parameter
     * @return The MethodSignature value
     */
    protected String getMethodSignature( MethodInfo method )
    {
        StringBuffer buf = new StringBuffer( method.getName() );
        buf.append( "(" );
        String[] params = method.getParametersType();
        for( int i = 0; i < params.length; i++ )
        {
            String type = params[ i ];
            int pos = type.lastIndexOf( '.' );
            if( pos != -1 )
            {
                String pkg = type.substring( 0, pos );
                if( "java.lang".equals( pkg ) )
                {
                    params[ i ] = type.substring( pos + 1 );
                }
            }
            buf.append( params[ i ] );
            if( i != params.length - 1 )
            {
                buf.append( ", " );
            }
        }
        buf.append( ")" );
        return buf.toString();
    }

    /**
     * Convert to a CovReport-like signature ie,
     * &lt;classname&gt;.&lt;method&gt;()
     *
     * @param clazz Description of Parameter
     * @param method Description of Parameter
     * @return The MethodSignature value
     */
    protected String getMethodSignature( ClassFile clazz, MethodInfo method )
    {
        StringBuffer buf = new StringBuffer( clazz.getFullName() );
        buf.append( "." );
        buf.append( method.getName() );
        buf.append( "()" );
        return buf.toString();
    }

    protected Hashtable getMethods( Element clazz )
    {
        Hashtable map = new Hashtable();
        NodeList children = clazz.getChildNodes();
        int len = children.getLength();
        for( int i = 0; i < len; i++ )
        {
            Node child = children.item( i );
            if( child.getNodeType() == Node.ELEMENT_NODE )
            {
                Element elem = (Element)child;
                if( "method".equals( elem.getNodeName() ) )
                {
                    String name = elem.getAttribute( "name" );
                    map.put( name, elem );
                }
            }
        }
        return map;
    }

    protected Element[] getPackages( Element snapshot )
    {
        ArrayList v = new ArrayList();
        NodeList children = snapshot.getChildNodes();
        int len = children.getLength();
        for( int i = 0; i < len; i++ )
        {
            Node child = children.item( i );
            if( child.getNodeType() == Node.ELEMENT_NODE )
            {
                Element elem = (Element)child;
                if( "package".equals( elem.getNodeName() ) )
                {
                    v.add( elem );
                }
            }
        }
        Element[] elems = new Element[ v.size() ];
        v.copyInto( elems );
        return elems;
    }

    /**
     * create an empty class element with its default cov.data (0)
     *
     * @param classFile Description of Parameter
     * @return Description of the Returned Value
     */
    protected Element createClassElement( ClassFile classFile )
    {
        // create the class element
        Element classElem = report.createElement( "class" );
        classElem.setAttribute( "name", classFile.getName() );
        // source file possibly does not exist in the bytecode
        if( null != classFile.getSourceFile() )
        {
            classElem.setAttribute( "source", classFile.getSourceFile() );
        }
        // create the cov.data elem
        Element classData = report.createElement( "cov.data" );
        classElem.appendChild( classData );
        // create the class cov.data element
        classData.setAttribute( "calls", "0" );
        classData.setAttribute( "hit_methods", "0" );
        classData.setAttribute( "total_methods", "0" );
        classData.setAttribute( "hit_lines", "0" );
        classData.setAttribute( "total_lines", "0" );
        return classElem;
    }

    /**
     * create an empty method element with its cov.data values
     *
     * @param method Description of Parameter
     * @return Description of the Returned Value
     */
    protected Element createMethodElement( MethodInfo method )
    {
        String methodsig = getMethodSignature( method );
        Element methodElem = report.createElement( "method" );
        methodElem.setAttribute( "name", methodsig );
        // create the method cov.data element
        Element methodData = report.createElement( "cov.data" );
        methodElem.appendChild( methodData );
        methodData.setAttribute( "calls", "0" );
        methodData.setAttribute( "hit_lines", "0" );
        methodData.setAttribute( "total_lines", String.valueOf( method.getNumberOfLines() ) );
        return methodElem;
    }

    /**
     * create node maps so that we can access node faster by their name
     */
    protected void createNodeMaps()
    {
        pkgMap = new Hashtable();
        classMap = new Hashtable();
        // create a map index of all packages by their name
        // @todo can be done faster by direct access.
        NodeList packages = report.getElementsByTagName( "package" );
        final int pkglen = packages.getLength();
        log( "Indexing " + pkglen + " packages" );
        for( int i = pkglen - 1; i > -1; i-- )
        {
            Element pkg = (Element)packages.item( i );
            String pkgname = pkg.getAttribute( "name" );

            int nbclasses = 0;
            // create a map index of all classes by their fully
            // qualified name.
            NodeList classes = pkg.getElementsByTagName( "class" );
            final int classlen = classes.getLength();
            log( "Indexing " + classlen + " classes in package " + pkgname );
            for( int j = classlen - 1; j > -1; j-- )
            {
                Element clazz = (Element)classes.item( j );
                String classname = clazz.getAttribute( "name" );
                if( pkgname != null && pkgname.length() != 0 )
                {
                    classname = pkgname + "." + classname;
                }

                int nbmethods = 0;
                NodeList methods = clazz.getElementsByTagName( "method" );
                final int methodlen = methods.getLength();
                for( int k = methodlen - 1; k > -1; k-- )
                {
                    Element meth = (Element)methods.item( k );
                    StringBuffer methodname = new StringBuffer( meth.getAttribute( "name" ) );
                    methodname.delete( methodname.toString().indexOf( "(" ), methodname.toString().length() );
                    String signature = classname + "." + methodname + "()";
                    if( filters.accept( signature ) )
                    {
                        log( "kept method:" + signature );
                        nbmethods++;
                    }
                    else
                    {
                        clazz.removeChild( meth );
                    }
                }
                // if we don't keep any method, we don't keep the class
                if( nbmethods != 0 && classFiles.containsKey( classname ) )
                {
                    log( "Adding class '" + classname + "'" );
                    classMap.put( classname, clazz );
                    nbclasses++;
                }
                else
                {
                    pkg.removeChild( clazz );
                }
            }
            if( nbclasses != 0 )
            {
                log( "Adding package '" + pkgname + "'" );
                pkgMap.put( pkgname, pkg );
            }
            else
            {
                pkg.getParentNode().removeChild( pkg );
            }
        }
        log( "Indexed " + classMap.size() + " classes in " + pkgMap.size() + " packages" );
    }

    /**
     * create an empty package element with its default cov.data (0)
     *
     * @param pkgname Description of Parameter
     * @return Description of the Returned Value
     */
    protected Element createPackageElement( String pkgname )
    {
        Element pkgElem = report.createElement( "package" );
        pkgElem.setAttribute( "name", pkgname );
        // create the package cov.data element / default
        // must be updated at the end of the whole process
        Element pkgData = report.createElement( "cov.data" );
        pkgElem.appendChild( pkgData );
        pkgData.setAttribute( "calls", "0" );
        pkgData.setAttribute( "hit_methods", "0" );
        pkgData.setAttribute( "total_methods", "0" );
        pkgData.setAttribute( "hit_lines", "0" );
        pkgData.setAttribute( "total_lines", "0" );
        return pkgElem;
    }

    /**
     * Do additional work on an element to remove abstract methods that are
     * reported by JProbe 3.0
     *
     * @param classFile Description of Parameter
     * @param classNode Description of Parameter
     */
    protected void removeAbstractMethods( ClassFile classFile, Element classNode )
    {
        MethodInfo[] methods = classFile.getMethods();
        Hashtable methodNodeList = getMethods( classNode );
        // assert xmlMethods.size() == methods.length()
        final int size = methods.length;
        for( int i = 0; i < size; i++ )
        {
            MethodInfo method = methods[ i ];
            String methodSig = getMethodSignature( method );
            Element methodNode = (Element)methodNodeList.get( methodSig );
            if( methodNode != null &&
                Utils.isAbstract( method.getAccessFlags() ) )
            {
                log( "\tRemoving abstract method " + methodSig );
                classNode.removeChild( methodNode );
            }
        }
    }

    /**
     * serialize a classfile into XML
     *
     * @param classFile Description of Parameter
     */
    protected void serializeClass( ClassFile classFile )
    {
        // the class already is reported so ignore it
        String fullclassname = classFile.getFullName();
        log( "Looking for '" + fullclassname + "'" );
        Element clazz = (Element)classMap.get( fullclassname );

        // ignore classes that are already reported, all the information is
        // already there.
        if( clazz != null )
        {
            log( "Ignoring " + fullclassname );
            removeAbstractMethods( classFile, clazz );
            return;
        }

        // ignore interfaces files, there is no code in there to cover.
        if( Utils.isInterface( classFile.getAccess() ) )
        {
            return;
        }

        ArrayList methods = getFilteredMethods( classFile );
        // no need to process, there are no methods to add for this class.
        if( methods.size() == 0 )
        {
            return;
        }

        String pkgname = classFile.getPackage();
        // System.out.println("Looking for package " + pkgname);
        Element pkgElem = (Element)pkgMap.get( pkgname );
        if( pkgElem == null )
        {
            pkgElem = createPackageElement( pkgname );
            report.getDocumentElement().appendChild( pkgElem );
            pkgMap.put( pkgname, pkgElem );// add the pkg to the map
        }
        // this is a brand new class, so we have to create a new node

        // create the class element
        Element classElem = createClassElement( classFile );
        pkgElem.appendChild( classElem );

        int total_lines = 0;
        int total_methods = 0;
        for( int i = 0; i < methods.size(); i++ )
        {
            // create the method element
            MethodInfo method = (MethodInfo)methods.get( i );
            if( Utils.isAbstract( method.getAccessFlags() ) )
            {
                continue;// no need to report abstract methods
            }
            Element methodElem = createMethodElement( method );
            classElem.appendChild( methodElem );
            total_lines += method.getNumberOfLines();
            total_methods++;
        }
        // create the class cov.data element
        Element classData = getCovDataChild( classElem );
        classData.setAttribute( "total_methods", String.valueOf( total_methods ) );
        classData.setAttribute( "total_lines", String.valueOf( total_lines ) );

        // add itself to the node map
        classMap.put( fullclassname, classElem );
    }

    /**
     * update the count of the XML, that is accumulate the stats on methods,
     * classes and package so that the numbers are valid according to the info
     * that was appended to the XML.
     */
    protected void update()
    {
        int calls = 0;
        int hit_methods = 0;
        int total_methods = 0;
        int hit_lines = 0;
        int total_lines = 0;

        // use the map for access, all nodes should be there
        Iterator enum = pkgMap.iterator();
        while( enum.hasNext() )
        {
            Element pkgElem = (Element)enum.next();
            String pkgname = pkgElem.getAttribute( "name" );
            Element[] classes = getClasses( pkgElem );
            int pkg_calls = 0;
            int pkg_hit_methods = 0;
            int pkg_total_methods = 0;
            int pkg_hit_lines = 0;
            int pkg_total_lines = 0;
            //System.out.println("Processing package '" + pkgname + "': " + classes.length + " classes");
            for( int j = 0; j < classes.length; j++ )
            {
                Element clazz = classes[ j ];
                String classname = clazz.getAttribute( "name" );
                if( pkgname != null && pkgname.length() != 0 )
                {
                    classname = pkgname + "." + classname;
                }
                // there's only cov.data as a child so bet on it
                Element covdata = getCovDataChild( clazz );
                try
                {
                    pkg_calls += Integer.parseInt( covdata.getAttribute( "calls" ) );
                    pkg_hit_methods += Integer.parseInt( covdata.getAttribute( "hit_methods" ) );
                    pkg_total_methods += Integer.parseInt( covdata.getAttribute( "total_methods" ) );
                    pkg_hit_lines += Integer.parseInt( covdata.getAttribute( "hit_lines" ) );
                    pkg_total_lines += Integer.parseInt( covdata.getAttribute( "total_lines" ) );
                }
                catch( NumberFormatException e )
                {
                    log( "Error parsing '" + classname + "' (" + j + "/" + classes.length + ") in package '" + pkgname + "'" );
                    throw e;
                }
            }
            Element covdata = getCovDataChild( pkgElem );
            covdata.setAttribute( "calls", String.valueOf( pkg_calls ) );
            covdata.setAttribute( "hit_methods", String.valueOf( pkg_hit_methods ) );
            covdata.setAttribute( "total_methods", String.valueOf( pkg_total_methods ) );
            covdata.setAttribute( "hit_lines", String.valueOf( pkg_hit_lines ) );
            covdata.setAttribute( "total_lines", String.valueOf( pkg_total_lines ) );
            calls += pkg_calls;
            hit_methods += pkg_hit_methods;
            total_methods += pkg_total_methods;
            hit_lines += pkg_hit_lines;
            total_lines += pkg_total_lines;
        }
        Element covdata = getCovDataChild( report.getDocumentElement() );
        covdata.setAttribute( "calls", String.valueOf( calls ) );
        covdata.setAttribute( "hit_methods", String.valueOf( hit_methods ) );
        covdata.setAttribute( "total_methods", String.valueOf( total_methods ) );
        covdata.setAttribute( "hit_lines", String.valueOf( hit_lines ) );
        covdata.setAttribute( "total_lines", String.valueOf( total_lines ) );
    }

}

