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
package org.apache.tools.ant.taskdefs.optional.ide;

import com.ibm.ivj.util.base.IvjException;
import com.ibm.ivj.util.base.Package;
import com.ibm.ivj.util.base.Project;
import java.io.File;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.tools.ant.DirectoryScanner;

/**
 * Class for scanning a Visual Age for Java workspace for packages matching
 * a certain criteria.
 * <p>
 * These criteria consist of a set of include and exclude patterns. With these
 * patterns, you can select which packages you want to have included, and which
 * packages you want to have excluded.  You can add patterns to be excluded by
 * default with the addDefaultExcludes method.  The patters that are excluded
 * by default include
 * <ul>
 *   <li>IBM*\**</li>
 *   <li>Java class libraries\**</li>
 *   <li>Sun class libraries*\**</li>
 *   <li>JSP Page Compile Generated Code\**</li>
 *   <li>VisualAge*\**</li>
 * </ul>
 * <p>
 * This class works like DirectoryScanner. 
 *
 * @see org.apache.tools.ant.DirectoryScanner
 *
 * @author Wolf Siberski, TUI Infotec (based on Arnout J. Kuipers DirectoryScanner)
 */
public class VAJWorkspaceScanner extends DirectoryScanner {

    /**
     * Patterns that should be excluded by default.
     *
     * @see #addDefaultExcludes()
     */
    private final static String[] DEFAULTEXCLUDES = 
    {
        "IBM*/**", 
        "Java class libraries/**", 
        "Sun class libraries*/**", 
        "JSP Page Compile Generated Code/**", 
        "VisualAge*/**", 
    }; 

    /**
     * The packages that where found and matched at least one includes, and
     * matched no excludes.
     */
    private Vector packagesIncluded = new Vector();
   
    /**
     * Adds the array with default exclusions to the current exclusions set.
     */
    public void addDefaultExcludes() {
        int excludesLength = excludes == null ? 0 : excludes.length;
        String[] newExcludes;
        newExcludes = new String[excludesLength + DEFAULTEXCLUDES.length];
        if (excludesLength > 0) {
            System.arraycopy(excludes, 0, newExcludes, 0, excludesLength);
        }
        for (int i = 0; i < DEFAULTEXCLUDES.length; i++) {
            newExcludes[i + excludesLength] = DEFAULTEXCLUDES[i].
                replace( '/', File.separatorChar ).replace( '\\', File.separatorChar ); 
        }
        excludes = newExcludes;
    }
        
    /**
     * Finds all Projects specified in include patterns.
     *
     * @return the projects
     */
    public Vector findMatchingProjects() {
        Project[] projects = VAJUtil.getWorkspace().getProjects();

        Vector matchingProjects = new Vector();

        boolean allProjectsMatch = false;
        for (int i = 0; i < projects.length; i++) {
            Project project = projects[i];
            for (int j = 0; j < includes.length && !allProjectsMatch; j++) {
                StringTokenizer tok = new StringTokenizer(includes[j], File.separator);
                String projectNamePattern = tok.nextToken();
                if (projectNamePattern.equals("**")) {
                    // if an include pattern starts with '**', 
                    // all projects match
                    allProjectsMatch = true;
                } else
                    if (match(projectNamePattern, project.getName())) {
                        matchingProjects.addElement(project);
                        break;
                    }
            }
        }

        if (allProjectsMatch) {
            matchingProjects = new Vector();
            for (int i = 0; i < projects.length; i++) {
                matchingProjects.addElement(projects[i]);
            }
        }

        return matchingProjects;
    }
        
    /**
     * Get the names of the packages that matched at least one of the include
     * patterns, and didn't match one of the exclude patterns.
     *
     * @return the matching packages
     */
    public Package[] getIncludedPackages() {
        int count = packagesIncluded.size();
        Package[] packages = new Package[count];
        for (int i = 0; i < count; i++) {
            packages[i] = (Package) packagesIncluded.elementAt(i);
        }
        return packages;
    }
        
    /**
     * Matches a string against a pattern. The pattern contains two special
     * characters:
     * '*' which means zero or more characters,
     * '?' which means one and only one character.
     *
     * @param pattern the (non-null) pattern to match against
     * @param str     the (non-null) string that must be matched against the
     *                pattern
     *
     * @return <code>true</code> when the string matches against the pattern,
     *         <code>false</code> otherwise.
     */
    protected static boolean match(String pattern, String str) {
        return DirectoryScanner.match(pattern, str);
    }
    /**
     * Scans the workspace for packages that match at least one include
     * pattern, and don't match any exclude patterns.
     *
     */
    public void scan() {
        if (includes == null) {
            // No includes supplied, so set it to 'matches all'
            includes = new String[1];
            includes[0] = "**";
        }
        if (excludes == null) {
            excludes = new String[0];
        }

        // only scan projects which are included in at least one include pattern
        Vector matchingProjects = findMatchingProjects();
        for (Enumeration e = matchingProjects.elements(); e.hasMoreElements();) {
            Project project = (Project) e.nextElement();
            scanProject(project);
        }
    }
        
    /**
     * Scans a project for packages that match at least one include
     * pattern, and don't match any exclude patterns.
     *
     */
    public void scanProject(Project project) {
        try {
            Package[] packages = project.getPackages();
            if (packages != null) {
                for (int i = 0; i < packages.length; i++) {
                    Package item = packages[i];
                    // replace '.' by file seperator because the patterns are
                    // using file seperator syntax (and we can use the match
                    // methods this way).
                    String name = 
                        project.getName()
                        + File.separator
                        + item.getName().replace('.', File.separatorChar); 
                    if (isIncluded(name) && !isExcluded(name)) {
                        packagesIncluded.addElement(item);
                    }
                }
            }
        } catch (IvjException e) {
            throw VAJUtil.createBuildException("VA Exception occured: ", e);
        }
    }
}
