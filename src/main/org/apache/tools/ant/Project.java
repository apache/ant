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

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Stack;
import java.text.StringCharacterIterator;
import java.text.CharacterIterator;

/**
 * Central representation of an Ant project. This class defines a
 * Ant project with all of it's targets and tasks. It also provides
 * the mechanism to kick off a build using a particular target name.
 * <p>
 * This class also encapsulates methods which allow Files to be refered
 * to using abstract path names which are translated to native system
 * file paths at runtime as well as defining various project properties.
 *
 * @author duncan@x180.com
 */

public class Project {

    public static final int MSG_ERR = 0;
    public static final int MSG_WARN = 1;
    public static final int MSG_INFO = 2;
    public static final int MSG_VERBOSE = 3;

    private static String javaVersion;
    // private set of constants to represent the state
    // of a DFS of the Target dependencies
    private static final String VISITING = "VISITING";
    private static final String VISITED = "VISITED";

    private String name;
    private PrintStream out = System.out;
    private int msgOutputLevel = MSG_INFO;

    private Hashtable properties = new Hashtable();
    private Hashtable userProperties = new Hashtable();
    private String defaultTarget;
    private Hashtable taskClassDefinitions = new Hashtable();
    private Hashtable targets = new Hashtable();
    private File baseDir;

    public Project() {
        detectJavaVersion();
	String defs = "/org/apache/tools/ant/taskdefs/defaults.properties";
	try {
	    Properties props = new Properties();
	    InputStream in = this.getClass()
		.getResourceAsStream(defs);
	    props.load(in);
	    in.close();
	    Enumeration enum = props.propertyNames();
	    while (enum.hasMoreElements()) {
		String key = (String)enum.nextElement();
		String value = props.getProperty(key);
		try {
		    Class taskClass = Class.forName(value);
		    addTaskDefinition(key, taskClass);
		} catch (ClassNotFoundException cnfe) {
		    // ignore...
		}
	    }

	    Properties systemP=System.getProperties();
	    Enumeration e=systemP.keys();
	    while( e.hasMoreElements() ) {
		String n=(String) e.nextElement();
		properties.put( n, systemP.get(n));
	    }
       	} catch (IOException ioe) {
	    String msg = "Can't load default task list";
	    System.out.println(msg);
	    System.exit(1);
	}
    }
    
    public void setOutput(PrintStream out) {
	this.out = out;
    }

    public void setOutputLevel(int msgOutputLevel) {
	this.msgOutputLevel = msgOutputLevel;
    }
    public int getOutputLevel() {
	return this.msgOutputLevel;
    }
    
    public void log(String msg) {
	log(msg, MSG_INFO);
    }

    public void log(String msg, int msgLevel) {
	if (msgLevel <= msgOutputLevel) {
	    out.println(msg);
	}
    }

    public void log(String msg, String tag, int msgLevel) {
	if (msgLevel <= msgOutputLevel) {
	    out.println("[" + tag + "]" + msg);
	}
    }

    public void setProperty(String name, String value) {
	// command line properties take precedence
	if( null!= userProperties.get(name))
	    return;
        log("Setting project property: " + name + " to " +
            value, MSG_VERBOSE);
	properties.put(name, value);
    }

    public void setUserProperty(String name, String value) {
        log("Setting project property: " + name + " to " +
            value, MSG_VERBOSE);
	userProperties.put(name, value);
	properties.put( name,value);
    }

    public String getProperty(String name) {
	String property = (String)properties.get(name);
	return property;
    }

    public Hashtable getProperties() {
	return properties;
    }
    
    public void setDefaultTarget(String defaultTarget) {
	this.defaultTarget = defaultTarget;
    }

    // deprecated, use setDefault
    public String getDefaultTarget() {
	return defaultTarget;
    }

    // match the attribute name
    public void setDefault(String defaultTarget) {
	this.defaultTarget = defaultTarget;
    }
    

    public void setName(String name) {
	this.name = name;
    }

    public String getName() {
	return name;
    }

    // match basedir attribute in xml
    public void setBasedir( String baseD ) throws BuildException {
	try {
	    setBaseDir(new File( new File(baseD).getCanonicalPath()));
	} catch (IOException ioe) {
	    String msg = "Can't set basedir " + baseDir + " due to " +
		ioe.getMessage();
	    throw new BuildException(msg);
	}
    }
    
    public void setBaseDir(File baseDir) {
	this.baseDir = baseDir;
	String msg = "Project base dir set to: " + baseDir;
	log(msg, MSG_INFO);
    }

    public File getBaseDir() {
	if(baseDir==null) {
	    try {
		setBasedir(".");
	    } catch(BuildException ex) {ex.printStackTrace();}
	}
	return baseDir;
    }
    

    public static String getJavaVersion() {
        return javaVersion;
    }

    private void detectJavaVersion() {

        // Determine the Java version by looking at available classes
        // java.lang.StrictMath was introduced in JDK 1.3
        // java.lang.ThreadLocal was introduced in JDK 1.2
        // java.lang.Void was introduced in JDK 1.1
        // Count up version until a NoClassDefFoundError ends the try

        try {
            javaVersion = "1.0";
            Class.forName("java.lang.Void");
            javaVersion = "1.1";
            Class.forName("java.lang.ThreadLocal");  
            javaVersion = "1.2";
            Class.forName("java.lang.StrictMath");
            javaVersion = "1.3";
	    setProperty("ant.java.version", javaVersion);
        }
        catch (ClassNotFoundException cnfe) {
            // swallow as we've hit the max class version that
            // we have
        }
        log("Detected Java Version: " + javaVersion);
    }

    public void addTaskDefinition(String taskName, Class taskClass) {
	String msg = " +User task: " + taskName + "     " + taskClass.getName();
	log(msg, MSG_VERBOSE);
	taskClassDefinitions.put(taskName, taskClass);
    }

    /**
     * This call expects to add a <em>new</em> Target.
     * @param target is the Target to be added to the current
     * Project.
     * @exception BuildException if the Target already exists
     * in the project.
     * @see Project#addOrReplaceTarget to replace existing Targets.
     */      
    public void addTarget(Target target) {
	String name = target.getName();
	if (targets.get(name) != null) {
	    throw new BuildException("Duplicate target: `"+name+"'");
	}
	addOrReplaceTarget(name, target);
    }

    /**
     * This call expects to add a <em>new</em> Target.
     * @param target is the Target to be added to the current
     * Project.
     * @param targetName is the name to use for the Target
     * @exception BuildException if the Target already exists
     * in the project.
     * @see Project#addOrReplaceTarget to replace existing Targets.
     */
     public void addTarget(String targetName, Target target)
         throws BuildException {
         if (targets.get(targetName) != null) {
             throw new BuildException("Duplicate target: `"+targetName+"'");
         }
         addOrReplaceTarget(targetName, target);
     }

    /**
     * @param target is the Target to be added or replaced in
     * the current Project.
     */
    public void addOrReplaceTarget(Target target) {
	addOrReplaceTarget(target.getName(), target);
    }
    
    /**
     * @param target is the Target to be added/replaced in
     * the current Project.
     * @param targetName is the name to use for the Target
     */
    public void addOrReplaceTarget(String targetName, Target target) {
	String msg = " +Target: " + targetName;
	log(msg, MSG_VERBOSE);
	targets.put(targetName, target);
    }
    
    public Task createTask(String taskType) throws BuildException {
	Class c = (Class)taskClassDefinitions.get(taskType);

	// XXX
	// check for nulls, other sanity

	try {
	    Task task = (Task)c.newInstance();
	    task.setProject(this);
	    String msg = "   +Task: " + taskType;
	    log (msg, MSG_VERBOSE);
	    return task;
	} catch (Exception e) {
	    String msg = "Could not create task of type: "
		 + taskType + " due to " + e;
	    throw new BuildException(msg);
    }	
    }
    
    public void executeTarget(String targetName) throws BuildException {

        // sanity check ourselves, if we've been asked to build nothing
        // then we should complain
        
        if (targetName == null) {
            String msg = "No target specified";
            throw new BuildException(msg);
        }

        // Sort the dependency tree, and run everything from the
        // beginning until we hit our targetName.
        // Sorting checks if all the targets (and dependencies)
        // exist, and if there is any cycle in the dependency
        // graph.
        Vector sortedTargets = topoSort(targetName, targets);

        int curidx = 0;
        String curtarget;
	
        do {
            curtarget = (String) sortedTargets.elementAt(curidx++);
            runTarget(curtarget, targets);
        } while (!curtarget.equals(targetName));
    }

    public File resolveFile(String fileName) {
	// deal with absolute files
	if (fileName.startsWith("/")) return new File( fileName );

        // Eliminate consecutive slashes after the drive spec
        if (fileName.length() >= 2 &&
            Character.isLetter(fileName.charAt(0)) &&
            fileName.charAt(1) == ':') {
            char[] ca = fileName.replace('/', '\\').toCharArray();
            char c;
            StringBuffer sb = new StringBuffer();

            for (int i = 0; i < ca.length; i++) {
                if ((ca[i] != '\\') ||
                    (ca[i] == '\\' &&
                        i > 0 &&
                        ca[i - 1] != '\\')) {
                    if (i == 0 &&
                        Character.isLetter(ca[i]) &&
                        i < ca.length - 1 &&
                        ca[i + 1] == ':') {
                        c = Character.toUpperCase(ca[i]);
                    } else {
                        c = ca[i];
                    }

                    sb.append(c);
                }
            }

            return new File(sb.toString());
        }

	File file = new File(baseDir.getAbsolutePath());
	StringTokenizer tok = new StringTokenizer(fileName, "/", false);
	while (tok.hasMoreTokens()) {
	    String part = tok.nextToken();
	    if (part.equals("..")) {
		file = new File(file.getParent());
	    } else if (part.equals(".")) {
		// Do nothing here
	    } else {
		file = new File(file, part);
	    }
	}

	try {
	    return new File(file.getCanonicalPath());
	}
	catch (IOException e) {
	    log("IOException getting canonical path for " + file + ": " +
                e.getMessage(), MSG_ERR);
	    return new File(file.getAbsolutePath());
	}
    }
    
    /**
        Translate a path into its native (platform specific)
        path. This should be extremely fast, code is 
        borrowed from ECS project.
        <p>
        All it does is translate the : into ; and / into \ 
        if needed. In other words, it isn't perfect.
        
        @returns translated string or empty string if to_process is null or empty
        @author Jon S. Stevens <a href="mailto:jon@clearink.com">jon@clearink.com</a>
    */
    public static String translatePath(String to_process) {
        if ( to_process == null || to_process.length() == 0 )
            return "";
    
        StringBuffer bs = new StringBuffer(to_process.length() + 50);
        StringCharacterIterator sci = new StringCharacterIterator(to_process);
        String path = System.getProperty("path.separator");
        String file = System.getProperty("file.separator");
        String tmp = null;
        for (char c = sci.first(); c != CharacterIterator.DONE; c = sci.next()) {
            tmp = String.valueOf(c);
            
            if (tmp.equals(":")) {
		// could be a DOS drive or a Unix path separator...
		// if followed by a backslash, assume it is a drive
		c = sci.next();
		tmp = String.valueOf(c);
		bs.append( tmp.equals("\\") ? ":" : path );
		if (c == CharacterIterator.DONE) break;
	    }

            if (tmp.equals(":") || tmp.equals(";"))
                tmp = path;
            else if (tmp.equals("/") || tmp.equals ("\\"))
                tmp = file;
            bs.append(tmp);
        }
        return(bs.toString());
    }

    // Given a string defining a target name, and a Hashtable
    // containing the "name to Target" mapping, pick out the
    // Target and execute it.
    private final void runTarget(String target, Hashtable targets)
        throws BuildException {
        Target t = (Target)targets.get(target);
        if (t == null) {
            throw new RuntimeException("Unexpected missing target `"+target+
                                       "' in this project.");
        }
        log("Executing Target: "+target, MSG_INFO);
        t.execute();
    }


    /**
     * Topologically sort a set of Targets.
     * @param root is the (String) name of the root Target. The sort is
     * created in such a way that the sequence of Targets uptil the root
     * target is the minimum possible such sequence.
     * @param targets is a Hashtable representing a "name to Target" mapping
     * @return a Vector of Strings with the names of the targets in
     * sorted order.
     * @exception BuildException if there is a cyclic dependency among the
     * Targets, or if a Target does not exist.
     */
    private final Vector topoSort(String root, Hashtable targets)
        throws BuildException {
        Vector ret = new Vector();
        Hashtable state = new Hashtable();
        Stack visiting = new Stack();

        // We first run a DFS based sort using the root as the starting node.
        // This creates the minimum sequence of Targets to the root node.
        // We then do a sort on any remaining unVISITED targets.
        // This is unnecessary for doing our build, but it catches
        // circular dependencies or missing Targets on the entire
        // dependency tree, not just on the Targets that depend on the
        // build Target.

        tsort(root, targets, state, visiting, ret);
        log("Build sequence for target `"+root+"' is "+ret, MSG_VERBOSE);
        for (Enumeration en=targets.keys(); en.hasMoreElements();) {
            String curTarget = (String)(en.nextElement());
            String st = (String) state.get(curTarget);
            if (st == null) {
                tsort(curTarget, targets, state, visiting, ret);
            }
            else if (st == VISITING) {
                throw new RuntimeException("Unexpected node in visiting state: "+curTarget);
            }
        }
        log("Complete build sequence is "+ret, MSG_VERBOSE);
        return ret;
    }

    // one step in a recursive DFS traversal of the Target dependency tree.
    // - The Hashtable "state" contains the state (VISITED or VISITING or null)
    // of all the target names.
    // - The Stack "visiting" contains a stack of target names that are
    // currently on the DFS stack. (NB: the target names in "visiting" are
    // exactly the target names in "state" that are in the VISITING state.)
    // 1. Set the current target to the VISITING state, and push it onto
    // the "visiting" stack.
    // 2. Throw a BuildException if any child of the current node is
    // in the VISITING state (implies there is a cycle.) It uses the
    // "visiting" Stack to construct the cycle.
    // 3. If any children have not been VISITED, tsort() the child.
    // 4. Add the current target to the Vector "ret" after the children
    //   have been visited. Move the current target to the VISITED state.
    //   "ret" now contains the sorted sequence of Targets upto the current
    //   Target.

    private final void tsort(String root, Hashtable targets,
                             Hashtable state, Stack visiting,
                             Vector ret)
        throws BuildException {
        state.put(root, VISITING);
        visiting.push(root);

        Target target = (Target)(targets.get(root));

        // Make sure we exist
        if (target == null) {
            StringBuffer sb = new StringBuffer("Target `");
            sb.append(root);
            sb.append("' does not exist in this project. ");
            visiting.pop();
            if (!visiting.empty()) {
                String parent = (String)visiting.peek();
                sb.append("It is used from target `");
                sb.append(parent);
                sb.append("'.");
            }

            throw new BuildException(new String(sb));
        }

        for (Enumeration en=target.getDependencies(); en.hasMoreElements();) {
            String cur = (String) en.nextElement();
            String m=(String)state.get(cur);
            if (m == null) {
                // Not been visited
                tsort(cur, targets, state, visiting, ret);
            }
            else if (m == VISITING) {
                // Currently visiting this node, so have a cycle
                throw makeCircularException(cur, visiting);
            }
        }

        String p = (String) visiting.pop();
        if (root != p) {
            throw new RuntimeException("Unexpected internal error: expected to pop "+root+" but got "+p);
        }
        state.put(root, VISITED);
        ret.addElement(root);
    }

    private static BuildException makeCircularException(String end, Stack stk) {
        StringBuffer sb = new StringBuffer("Circular dependency: ");
        sb.append(end);
        String c;
        do {
            c = (String)stk.pop();
            sb.append(" <- ");
            sb.append(c);
        } while(!c.equals(end));
        return new BuildException(new String(sb));
    }
}
