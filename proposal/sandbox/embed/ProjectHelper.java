/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Locale;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.xml.sax.AttributeList;


/**
 * Configures a Project (complete with Targets and Tasks) based on
 * a XML build file. It'll rely on a plugin to do the actual processing
 * of the xml file.
 *
 * This class also provide static wrappers for common introspection.
 *
 * All helper plugins must provide backward compatiblity with the
 * original ant patterns, unless a different behavior is explicitely
 * specified. For example, if namespace is used on the <project> tag
 * the helper can expect the entire build file to be namespace-enabled.
 * Namespaces or helper-specific tags can provide meta-information to
 * the helper, allowing it to use new ( or different policies ).
 *
 * However, if no namespace is used the behavior should be exactly
 * identical with the default helper.
 *
 * @author duncan@x180.com
 */
public /*abstract*/ class ProjectHelper {

    /**
     * Configures the Project with the contents of the specified XML file.
     * ( should it be deprecated ? Using getProjectHelper(), parse()
     * is cleaner )
     */
    public static void configureProject(Project project, File buildFile)
        throws BuildException
    {
        ProjectHelper helper=ProjectHelper.getProjectHelper();
        helper.parse(project, buildFile);
    }

    public ProjectHelper() {
    }
    
    /**
     * Constructs a new Ant parser for the specified XML file.
     * @deprecated Use the plugin mechanism instead.
     */
    private ProjectHelper(Project project, File buildFile) {
        //  this.project = project;
        //  this.buildFile = new File(buildFile.getAbsolutePath());
        //  buildFileParent = new File(this.buildFile.getParent());
    }

    public Project createProject(ClassLoader coreLoader) {
        return new Project();
    }
    
    /**
     * Process an input source for the project.
     *
     * All processors must support at least File sources. It is usefull to also support
     * InputSource - this allows the input to come from a non-filesystem source
     * (like input stream of a POST, or a soap body ).
     */
    public /*abstract*/ void parse(Project project, Object source)
        throws BuildException
    {
        throw new BuildException("You must use a real ProjectHelper implementation"); 
    }

    /* -------------------- Helper discovery -------------------- */
    public static final String HELPER_PROPERTY =
        "org.apache.tools.ant.ProjectHelper";
    
    public static final String SERVICE_ID =
        "/META-INF/services/org.apache.tools.ant.ProjectHelper";

    
    /** Discover a project helper instance.
     */
    public static ProjectHelper getProjectHelper()
        throws BuildException
    {
        // Identify the class loader we will be using. Ant may be
        // in a webapp or embeded in a different app
        ClassLoader classLoader = getContextClassLoader();
        ProjectHelper helper=null;
        
        // First, try the system property
        try {
            String helperClass = System.getProperty(HELPER_PROPERTY);
            if (helperClass != null) {
                helper = newHelper(helperClass, classLoader);
            }
        } catch (SecurityException e) {
            // It's ok, we'll try next option
            ;
        }

        // A JDK1.3 'service' ( like in JAXP ). That will plug a helper
        // automatically if in CLASSPATH, with the right META-INF/services.
        if( helper==null ) {
            try {
                InputStream is=null;
                if (classLoader == null) {
                    is=ClassLoader.getSystemResourceAsStream( SERVICE_ID );
                } else {
                    is=classLoader.getResourceAsStream( SERVICE_ID );
                }

                if( is != null ) {
                    // This code is needed by EBCDIC and other strange systems.
                    // It's a fix for bugs reported in xerces
                    BufferedReader rd;
                    try {
                        rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    } catch (java.io.UnsupportedEncodingException e) {
                        rd = new BufferedReader(new InputStreamReader(is));
                    }
                    
                    String helperClassName = rd.readLine();
                    rd.close();
                    
                    if (helperClassName != null &&
                        ! "".equals(helperClassName)) {
                        
                        helper= newHelper( helperClassName, classLoader );
                    }
                }
            } catch( Exception ex ) {
                ;
            }
        }

        // Default
        return new ProjectHelperImpl();
    }

    private static ProjectHelper newHelper(String helperClass,
                                           ClassLoader classLoader)
        throws BuildException
    {

        try {
            Class clazz = null;
            if (classLoader == null) {
                clazz = Class.forName(helperClass);
            } else {
                clazz = classLoader.loadClass(helperClass);
            }
            return ((ProjectHelper) clazz.newInstance());
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /** 
     */
    public static ClassLoader getContextClassLoader()
        throws BuildException
    {
        // Are we running on a JDK 1.2 or later system?
        Method method = null;
        try {
            method = Thread.class.getMethod("getContextClassLoader", null);
        } catch (NoSuchMethodException e) {
            // we are running on JDK 1.1
            return null; 
        }

        // Get the thread context class loader (if there is one)
        ClassLoader classLoader = null;
        try {
            classLoader = (ClassLoader)
                method.invoke(Thread.currentThread(), null);
        } catch (IllegalAccessException e) {
            throw new BuildException
                ("Unexpected IllegalAccessException", e);
        } catch (InvocationTargetException e) {
            throw new BuildException
                ("Unexpected InvocationTargetException", e);
        }

        // Return the selected class loader
        return (classLoader);
    }

    
    /* -------------------- Common utilities and wrappers -------------------- */

    /** Configure a java object using ant's rules.
     */
    public static void configure(Object target, AttributeList attrs, 
                                 Project project) throws BuildException {
        TaskAdapter adapter=null;
        if( target instanceof TaskAdapter ) {
            adapter=(TaskAdapter)target;
            target=adapter.getProxy();
        }

        IntrospectionHelper ih = 
            IntrospectionHelper.getHelper(target.getClass());
        if( adapter != null )
            adapter.setIntrospectionHelper( ih );

        // XXX What's that ?
        project.addBuildListener(ih);

        for (int i = 0; i < attrs.getLength(); i++) {
            // reflect these into the target
            String value=replaceProperties(project, attrs.getValue(i), 
                                           project.getProperties() );
            String name=attrs.getName(i).toLowerCase(Locale.US);
            try {
                if (adapter!=null ) {
                    adapter.setAttribute( name, value );
                } else {
                    ih.setAttribute(project, target, 
                                    name, value);
                }
            } catch (BuildException be) {
                // id attribute must be set externally
                // XXX Shuldn't it be 'name' ( i.e. lower-cased ) ?
                if (!attrs.getName(i).equals("id")) {
                    throw be;
                }
            }
        }
    }

    /**
     * Adds the content of #PCDATA sections to an element.
     */
    public static void addText(Project project, Object target, char[] buf, int start, int end)
        throws BuildException {
        addText(project, target, new String(buf, start, end));
    }

    /**
     * Adds the content of #PCDATA sections to an element.
     */
    public static void addText(Project project, Object target, String text)
        throws BuildException {

        if (text == null ) {
            return;
        }

        if(target instanceof TaskAdapter) {
            target = ((TaskAdapter) target).getProxy();
        }

        IntrospectionHelper.getHelper(target.getClass()).addText(project, target, text);
    }

    /**
     * Stores a configured child element into its parent object 
     */
    public static void storeChild(Project project, Object parent, Object child, String tag) {
        IntrospectionHelper ih = IntrospectionHelper.getHelper(parent.getClass());
        ih.storeElement(project, parent, child, tag);
    }

    /**
     * Replace ${} style constructions in the given value with the string value of
     * the corresponding data types.
     *
     * @param value the string to be scanned for property references.
     * @since 1.5
     */
     public static String replaceProperties(Project project, String value)
            throws BuildException {
         return project.replaceProperties(value);
     }

    /**
     * Replace ${} style constructions in the given value with the string value of
     * the corresponding data types.
     *
     * @param value the string to be scanned for property references.
     */
     public static String replaceProperties(Project project, String value, Hashtable keys)
            throws BuildException {
        if (value == null) {
            return null;
        }

        Vector fragments = new Vector();
        Vector propertyRefs = new Vector();
        parsePropertyString(value, fragments, propertyRefs);

        StringBuffer sb = new StringBuffer();
        Enumeration i = fragments.elements();
        Enumeration j = propertyRefs.elements();
        while (i.hasMoreElements()) {
            String fragment = (String)i.nextElement();
            if (fragment == null) {
                String propertyName = (String)j.nextElement();
                if (!keys.containsKey(propertyName)) {
                    project.log("Property ${" + propertyName + "} has not been set", Project.MSG_VERBOSE);
                }
                fragment = (keys.containsKey(propertyName)) ? (String) keys.get(propertyName) 
                                                            : "${" + propertyName + "}"; 
            }
            sb.append(fragment);
        }                        
        
        return sb.toString();
    }

    /**
     * This method will parse a string containing ${value} style 
     * property values into two lists. The first list is a collection
     * of text fragments, while the other is a set of string property names
     * null entries in the first list indicate a property reference from the
     * second list.
     */
    public static void parsePropertyString(String value, Vector fragments, Vector propertyRefs) 
        throws BuildException {
        int prev = 0;
        int pos;
        while ((pos = value.indexOf("$", prev)) >= 0) {
            if (pos > 0) {
                fragments.addElement(value.substring(prev, pos));
            }

            if( pos == (value.length() - 1)) {
                fragments.addElement("$");
                prev = pos + 1;
            }
            else if (value.charAt(pos + 1) != '{' ) {
                fragments.addElement(value.substring(pos + 1, pos + 2));
                prev = pos + 2;
            } else {
                int endName = value.indexOf('}', pos);
                if (endName < 0) {
                    throw new BuildException("Syntax error in property: " 
                                                 + value );
                }
                String propertyName = value.substring(pos + 2, endName);
                fragments.addElement(null);
                propertyRefs.addElement(propertyName);
                prev = endName + 1;
            }
        }

        if (prev < value.length()) {
            fragments.addElement(value.substring(prev));
        }
    }

}
