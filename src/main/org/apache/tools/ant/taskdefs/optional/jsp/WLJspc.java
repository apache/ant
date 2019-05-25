/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.taskdefs.optional.jsp;

//apache/ant imports
import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;

/**
 * Precompiles JSP's using WebLogic's JSP compiler (weblogic.jspc).
 *
 * Tested only on WebLogic 4.5.1 - NT4.0 and Solaris 5.7
 *
 * required attributes
 *      src : root of source tree for JSP, ie, the document root for your WebLogic server
 *      dest : root of destination directory, what you have set as
 *             WorkingDir in the WebLogic properties
 *      package : start package name under which your JSP's would be compiled
 *
 * other attributes
 *     classpath
 *
 * A classpath should be set which contains the WebLogic classes as well as all
 * application classes referenced by the JSP. The system classpath is also
 * appended when the jspc is called, so you may choose to put everything in
 * the classpath while calling Ant. However, since presumably the JSP's will
 * reference classes being build by Ant, it would be better to explicitly add
 * the classpath in the task
 *
 * The task checks timestamps on the JSP's and the generated classes, and compiles
 * only those files that have changed.
 *
 * It follows the WebLogic naming convention of putting classes in
 *  <b> _dirName/_fileName.class for dirname/fileName.jsp   </b>
 *
 * Limitation: It compiles the files through the Classic compiler only.
 * Limitation: Since it is my experience that WebLogic jspc throws out of
 *             memory error on being given too many files at one go, it is
 *             called multiple times with one jsp file each.
 *
 * <pre>
 * example
 * &lt;target name="jspcompile" depends="compile"&gt;
 *   &lt;wljspc src="c:\\weblogic\\myserver\\public_html"
 *           dest="c:\\weblogic\\myserver\\serverclasses" package="myapp.jsp"&gt;
 *   &lt;classpath&gt;
 *          &lt;pathelement location="${weblogic.classpath}" /&gt;
 *           &lt;pathelement path="${compile.dest}" /&gt;
 *      &lt;/classpath&gt;
 *
 *   &lt;/wljspc&gt;
 * &lt;/target&gt;
 * </pre>
 *
 */

public class WLJspc extends MatchingTask {
    //TODO Test on other versions of WebLogic
    //TODO add more attributes to the task, to take care of all jspc options
    //TODO Test on Unix

    /** root of compiled files tree */
    private File destinationDirectory;

    /** root of source files tree */
    private File sourceDirectory;

    /** package under which resultant classes will reside */
    private String destinationPackage;

    /** classpath used to compile the jsp files. */
    private Path compileClasspath;

    //private String compilerPath; //fully qualified name for the compiler executable

    private String pathToPackage = "";
    private List<String> filesToDo = new Vector<>();

    /**
     * Run the task.
     * @throws BuildException if there is an error.
     */
    @Override
    public void execute() throws BuildException {
        if (!destinationDirectory.isDirectory()) {
            throw new BuildException("destination directory %s is not valid",
                destinationDirectory.getPath());
        }

        if (!sourceDirectory.isDirectory()) {
            throw new BuildException("src directory %s is not valid",
                sourceDirectory.getPath());
        }

        if (destinationPackage == null) {
            throw new BuildException("package attribute must be present.",
                                     getLocation());
        }

        pathToPackage
            = this.destinationPackage.replace('.', File.separatorChar);
        // get all the files in the sourceDirectory
        DirectoryScanner ds = super.getDirectoryScanner(sourceDirectory);

        //use the systemclasspath as well, to include the ant jar
        if (compileClasspath == null) {
            compileClasspath = new Path(getProject());
        }

        compileClasspath = compileClasspath.concatSystemClasspath();

        // WebLogic jspc calls System.exit() ... have to fork
        // Therefore, takes loads of time
        // Can pass directories at a time (*.jsp) but easily runs out of
        // memory on hefty dirs (even on  a Sun)
        Java helperTask = new Java(this);
        helperTask.setFork(true);
        helperTask.setClassname("weblogic.jspc");
        helperTask.setTaskName(getTaskName());
        // CheckStyle:MagicNumber OFF
        String[] args = new String[12];
        // CheckStyle:MagicNumber ON

        int j = 0;
        //TODO  this array stuff is a remnant of prev trials.. gotta remove.
        args[j++] = "-d";
        args[j++] = destinationDirectory.getAbsolutePath().trim();
        args[j++] = "-docroot";
        args[j++] = sourceDirectory.getAbsolutePath().trim();
        args[j++] = "-keepgenerated";
        //Call compiler as class... dont want to fork again
        //Use classic compiler -- can be parameterised?
        args[j++] =  "-compilerclass";
        args[j++] = "sun.tools.javac.Main";
        // WebLogic jspc does not seem to work unless u explicitly set this...
        // Does not take the classpath from the env....
        // Am i missing something about the Java task??
        args[j++] = "-classpath";
        args[j++] = compileClasspath.toString();

        this.scanDir(ds.getIncludedFiles());
        log("Compiling " + filesToDo.size() + " JSP files");

        for (String filename : filesToDo) {
            //TODO
            // All this to get package according to WebLogic standards
            // Can be written better... this is too hacky!
            // Careful.. similar code in scanDir, but slightly different!!
            File jspFile = new File(filename);
            args[j] = "-package";
            String parents = jspFile.getParent();
            if (parents == null || parents.isEmpty()) {
                args[j + 1] = destinationPackage;
            } else {
                parents = this.replaceString(parents, File.separator, "_.");
                args[j + 1] = destinationPackage + "." + "_" + parents;
            }

            args[j + 2] =  sourceDirectory + File.separator + filename;
            helperTask.clearArgs();

            // CheckStyle:MagicNumber OFF
            for (int x = 0; x < j + 3; x++) {
                helperTask.createArg().setValue(args[x]);
            }
            // CheckStyle:MagicNumber ON

            helperTask.setClasspath(compileClasspath);
            if (helperTask.executeJava() != 0) {
                log(filename + " failed to compile", Project.MSG_WARN);
            }
        }
    }

    /**
     * Set the classpath to be used for this compilation.
     * @param classpath the classpath to use.
     */
    public void setClasspath(Path classpath) {
        if (compileClasspath == null) {
            compileClasspath = classpath;
        } else {
            compileClasspath.append(classpath);
        }
    }

    /**
     * Maybe creates a nested classpath element.
     * @return a path to be configured.
     */
    public Path createClasspath() {
        if (compileClasspath == null) {
            compileClasspath = new Path(getProject());
        }
        return compileClasspath;
    }

    /**
     * Set the directory containing the source jsp's
     *
     *
     * @param dirName the directory containing the source jsp's
     */
    public void setSrc(File dirName) {
        sourceDirectory = dirName;
    }

     /**
     * Set the directory containing the source jsp's
     *
     *
     * @param dirName the directory containing the source jsp's
     */
    public void setDest(File dirName) {
        destinationDirectory = dirName;
    }

    /**
     * Set the package under which the compiled classes go
     *
     * @param packageName the package name for the classes
     */
    public void setPackage(String packageName) {
        destinationPackage = packageName;
    }

    /**
     * Scan the array of files and add the jsp
     * files that need to be compiled to the filesToDo field.
     * @param files the files to scan.
     */
    protected void scanDir(String[] files) {
        long now = Instant.now().toEpochMilli();
        for (String file : files) {
            File srcFile = new File(this.sourceDirectory, file);
            //TODO
            // All this to convert source to destination directory according
            // to WebLogic standards - can be written better... this is too hacky!
            File jspFile = new File(file);
            String parents = jspFile.getParent();

            String pack;
            if (parents == null || parents.isEmpty()) {
                pack = pathToPackage;
            } else {
                parents =  this.replaceString(parents, File.separator, "_/");
                pack = pathToPackage + File.separator + "_" + parents;
            }

            String filePath = pack + File.separator + "_";
            int startingIndex = file.lastIndexOf(File.separator) != -1
                    ? file.lastIndexOf(File.separator) + 1 : 0;
            int endingIndex = file.indexOf(".jsp");
            if (endingIndex == -1) {
                log("Skipping " + file + ". Not a JSP",
                    Project.MSG_VERBOSE);
                continue;
            }

            filePath += file.substring(startingIndex, endingIndex);
            filePath += ".class";
            File classFile = new File(this.destinationDirectory, filePath);

            if (srcFile.lastModified() > now) {
                log("Warning: file modified in the future: "
                    + file, Project.MSG_WARN);
            }
            if (srcFile.lastModified() > classFile.lastModified()) {
                filesToDo.add(file);
                log("Recompiling File " + file, Project.MSG_VERBOSE);
            }
        }
    }

    /**
     * Replace occurrences of a string with a replacement string.
     * @param inpString the string to convert.
     * @param escapeChars the string to replace.
     * @param replaceChars the string to place.
     * @return the converted string.
     */
    protected String replaceString(String inpString, String escapeChars,
                                   String replaceChars) {
        StringBuilder localString = new StringBuilder();
        StringTokenizer st = new StringTokenizer(inpString, escapeChars, true);
        int numTokens = st.countTokens();
        for (int i = 0; i < numTokens; i++) {
            String test = st.nextToken();
            test = test.equals(escapeChars) ? replaceChars : test;
            localString.append(test);
        }
        return localString.toString();
    }
}
