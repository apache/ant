/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

import java.beans.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import org.xml.sax.SAXException;
import org.w3c.dom.*;
    
/**
 * Configures a Project (complete with Targets and Tasks) based on
 * a XML build file.
 *
 * @author duncan@x180.com
 */

public class ProjectHelper {

    public static void configureProject(Project project, File buildFile)
	throws BuildException
    {

	// XXX
	// need to get rid of the DOM layer and use SAX
	
	Document doc;

	try {
	    doc=Parser.getParser(project).parse(buildFile);
	} catch (IOException ioe) {
	    String msg = "Can't open config file: " + buildFile +
		" due to: " + ioe;
	    throw new BuildException(msg);
	} catch (SAXException se) {
	    String msg = "Can't open config file: " + buildFile +
		" due to: " + se;
	    throw new BuildException(msg);
	}

	Element root = doc.getDocumentElement();

	// sanity check, make sure that we have the right element
	// as we aren't validating the input

	if (!root.getTagName().equals("project")) {
	    String msg = "Config file is not of expected XML type";
	    throw new BuildException(msg);
	}

	project.setName(root.getAttribute("name"));
	project.setDefaultTarget(root.getAttribute("default"));

	String baseDir = project.getProperty("basedir");
	if (baseDir == null) {
	    baseDir = root.getAttribute("basedir");
	    if (baseDir.equals("")) {
		// Using clunky JDK1.1 methods here
		baseDir = new File(buildFile.getAbsolutePath()).getParent();
	    }
	}
	project.setBasedir(baseDir);
	    
	// set up any properties that may be in the config file

	//	configureProperties(project, root);
	
	// set up any task defs that may be in the config file

	//	configureTaskDefs(project, root);

	// set up the targets into the project
	init(project, root );
	configureTargets(project, root);
    }

    /** Read and execute init - all other targets will be loaded after ( to
     * make sure all properties are set ).
     * 
     */
    private static void init(Project project, Element root)
	throws BuildException
    {
	// Hack - all tasks outside init target will be added to init
	// ( will be removed when / if build.xml will start using init )
	Target initTarget = new Target();
	initTarget.setProject(project);
	initTarget.setName( "init" );
	project.addTarget( "init", initTarget );
	configureTasks( project, initTarget, root );

	NodeList list = root.getElementsByTagName("target");
	for (int i = 0; i < list.getLength(); i++) {
	    Element element = (Element)list.item(i);
	    String targetName = element.getAttribute("name");

	    if( targetName.equals("init") ) 
		configureTasks(project, initTarget, element);
	}
	initTarget.execute();
    }

    private static void configureTargets(Project project, Element root)
	throws BuildException
    {
	// configure targets
	NodeList list = root.getElementsByTagName("target");
	for (int i = 0; i < list.getLength(); i++) {
	    Element element = (Element)list.item(i);
	    String targetName = element.getAttribute("name");
	    String targetDep = element.getAttribute("depends");

	    // all targets must have a name
	    if (targetName.equals("")) {
		String msg = "target element appears without a name attribute";
		throw new BuildException(msg);
	    }

	    // init is done already
	    if( targetName.equals("init") ) 
		continue;
	    
	    Target target = new Target();
	    target.setName(targetName);
	    project.addTarget(targetName, target);
	    
	    // take care of dependencies
	    
	    if (targetDep.length() > 0) {
		StringTokenizer tok =
		    new StringTokenizer(targetDep, ",", false);
		while (tok.hasMoreTokens()) {
		    target.addDependency(tok.nextToken().trim());
		}
	    }

	    // populate target with tasks

	    configureTasks(project, target, element);
	}
    }

    private static void configureTasks(Project project,
				       Target target,
				       Element targetElement)
	throws BuildException
    {
	NodeList list = targetElement.getChildNodes();
	for (int i = 0; i < list.getLength(); i++) {
	    Node node = list.item(i);

	    // right now, all we are interested in is element nodes
	    // not quite sure what to do with others except drop 'em

	    if (node.getNodeType() == Node.ELEMENT_NODE) {
		Element element = (Element)node;
		String taskType = element.getTagName();

		// special case - no target in a target.
		// hack to allow this method to set "init" target
		// using root element
		if( ! taskType.equals( "target" ) ) {
		    // XXX
		    // put in some sanity checking
		    
		    Task task = project.createTask(taskType);
		    
		    // get the attributes of this element and reflect them
		    // into the task
		    
		    NamedNodeMap nodeMap = element.getAttributes();
		    configureTask(project, task, nodeMap);
		    target.addTask(task);
		}
	    }
	}
    }

    private static void configureTask(Project project,
				      Task task,
				      NamedNodeMap nodeMap)
	throws BuildException
    {
	// XXX
	// instead of doing this introspection each time around, I
	// should have a helper class to keep this info around for
	// each kind of class
	
	Hashtable propertySetters = new Hashtable();
	BeanInfo beanInfo;
	try {
	    beanInfo = Introspector.getBeanInfo(task.getClass());
	} catch (IntrospectionException ie) {
	    String msg = "Can't introspect task class: " + task.getClass();
	    throw new BuildException(msg);
	}

	PropertyDescriptor[] pda = beanInfo.getPropertyDescriptors();
	for (int i = 0; i < pda.length; i++) {
	    PropertyDescriptor pd = pda[i];
	    String property = pd.getName();
	    Method setMethod = pd.getWriteMethod();
	    if (setMethod != null) {

		// make sure that there's only 1 param and that it
		// takes a String object, all other setMethods need
		// to get screened out

		Class[] ma =setMethod.getParameterTypes();
		if (ma.length == 1) {
		    Class c = ma[0];
		    if (c.getName().equals("java.lang.String")) {
			propertySetters.put(property, setMethod);
		    }
		}
	    }
	}

	for (int i = 0; i < nodeMap.getLength(); i++) {
	    Node node = nodeMap.item(i);

	    // these should only be attribs, we won't see anything
	    // else here.

	    if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
		Attr attr = (Attr)node;

		// reflect these into the task

		Method setMethod = (Method)propertySetters.get(attr.getName());
		if (setMethod == null) {
		    String msg = "Configuration property \"" + attr.getName() +
			"\" does not have a setMethod in " + task.getClass();
		    throw new BuildException(msg);
		}

		String value=replaceProperties(  attr.getValue(), project.getProperties() );
		try {
		    setMethod.invoke(task, new String[] {value});
		} catch (IllegalAccessException iae) {
		    String msg = "Error setting value for attrib: " +
			attr.getName();
		    iae.printStackTrace();
		    throw new BuildException(msg);
		} catch (InvocationTargetException ie) {
		    String msg = "Error setting value for attrib: " +
			attr.getName() + " in " + task.getClass().getName();
		    ie.printStackTrace();
		    ie.getTargetException().printStackTrace();
		    throw new BuildException(msg);		    
		}
	    }
	}
    }

    /** Replace ${NAME} with the property value
     */
    public static String replaceProperties( String value, Hashtable keys )
	throws BuildException
    {
	// XXX use Map instead of proj, it's too heavy

	// XXX need to replace this code with something better.
	StringBuffer sb=new StringBuffer();
	int i=0;
	int prev=0;
	// assert value!=nil
	int pos;
	while( (pos=value.indexOf( "$", prev )) >= 0 ) {
	    if(pos>0)
		sb.append( value.substring( prev, pos ) );
	    if( value.charAt( pos + 1 ) != '{' ) {
		sb.append( value.charAt( pos + 1 ) );
		prev=pos+2; // XXX 
	    } else {
		int endName=value.indexOf( '}', pos );
		if( endName < 0 ) {
		    throw new BuildException("Syntax error in prop: " +
					     value );
		}
		String n=value.substring( pos+2, endName );
		String v=(String) keys.get( n );
		//System.out.println("N: " + n + " " + " V:" + v); 
		sb.append( v );
		prev=endName+1;
	    }
	}
	if( prev < value.length() ) sb.append( value.substring( prev ) );
	//	System.out.println("After replace: " + sb.toString());
	// System.out.println("Before replace: " + value);
	return sb.toString();
    }
}









