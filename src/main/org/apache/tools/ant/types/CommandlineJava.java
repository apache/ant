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
 * 4. The names "Ant" and "Apache Software
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

package org.apache.tools.ant.types;

import java.util.Properties;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * A representation of a Java command line that is nothing more
 * than a composite of 2 <tt>Commandline</tt>. One is used for the 
 * vm/options and one for the classname/arguments. It provides 
 * specific methods for a java command line.
 *
 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class CommandlineJava implements Cloneable {

    /**
     * commands to the JVM
     */
    private Commandline vmCommand = new Commandline();
    /**
     * actual java commands
     */
    private Commandline javaCommand = new Commandline();
    /**
     * properties to add using -D
     */
    private SysProperties sysProperties = new SysProperties();
    private Path classpath = null;
    private String vmVersion;
    private String maxMemory = null;

    /**
     * Indicate whether it will execute a jar file or not, in this case
     * the first vm option must be a -jar and the 'executable' is a jar file.
     */
     private boolean executeJar  = false;

    /**
     * Specialized Environment class for System properties
     */
    public static class SysProperties extends Environment implements Cloneable {
        Properties sys = null;

        public String[] getVariables() throws BuildException {
            String[] props = super.getVariables();
      
            if (props == null) {
              return null;
            }

            for (int i = 0; i < props.length; i++) {
                props[i] = "-D" + props[i];
            }
            return props;
        }

        public int size() {
            return variables.size();
        }

        public void setSystem() throws BuildException {
            try {
                sys = System.getProperties();
                Properties p = new Properties();
                for (Enumeration e = sys.keys(); e.hasMoreElements();) {
                    Object o = e.nextElement();
                    p.put(o, sys.get(o));
                }
                for (Enumeration e = variables.elements(); e.hasMoreElements();) {
                    Environment.Variable v = (Environment.Variable) e.nextElement();
                    p.put(v.getKey(), v.getValue());
                }
                System.setProperties(p);
            } catch (SecurityException e) {
                throw new BuildException("Cannot modify system properties", e);
            }
        }

        public void restoreSystem() throws BuildException {
            if (sys == null) {
                throw new BuildException("Unbalanced nesting of SysProperties");
            }

            try {
                System.setProperties(sys);
                sys = null;
            } catch (SecurityException e) {
                throw new BuildException("Cannot modify system properties", e);
            }
        }

        public Object clone() {
            try {
                SysProperties c = (SysProperties) super.clone();
                c.variables = (Vector) variables.clone();
                return c;
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }

    }

    /**
     * constructor uses the VM we are running on now.
     */
    public CommandlineJava() {
        setVm(JavaEnvUtils.getJreExecutable("java"));
        setVmversion(JavaEnvUtils.getJavaVersion());
    }

    public Commandline.Argument createArgument() {
        return javaCommand.createArgument();
    }

    public Commandline.Argument createVmArgument() {
        return vmCommand.createArgument();
    }

    public void addSysproperty(Environment.Variable sysp) {
        sysProperties.addVariable(sysp);
    }

    public void setVm(String vm) {
        vmCommand.setExecutable(vm);
    }

    public void setVmversion(String value) {
        vmVersion = value;
    }

    /**
     * set a jar file to execute via the -jar option.
     * @param jarpathname the pathname of the jar to execute
     */
    public void setJar(String jarpathname){
        javaCommand.setExecutable(jarpathname);
        executeJar = true;
    }

    /**
     * @return the pathname of the jar file to run via -jar option
     * or <tt>null</tt> if there is no jar to run.
     * @see #getClassname()
     */
    public String getJar(){
        if (executeJar){
            return javaCommand.getExecutable();
        }
        return null;
    }

    /**
     * set the classname to execute
     * @param classname the fully qualified classname.
     */
    public void setClassname(String classname) {
        javaCommand.setExecutable(classname);
        executeJar = false;
    }

    /**
     * @return the name of the class to run or <tt>null</tt> if there is no class.
     * @see #getJar()
     */
    public String getClassname() {
        if (!executeJar) {
            return javaCommand.getExecutable();
        }
        return null;
    }

    public Path createClasspath(Project p) {
        if (classpath == null) {
            classpath = new Path(p);
        }
        return classpath;
    }

    public String getVmversion() {
        return vmVersion;
    }

    /**
     * get the command line to run a java vm.
     * @return the list of all arguments necessary to run the vm.
     */
    public String[] getCommandline() {
        String[] result = new String[size()];
        int pos = 0;
        String[] vmArgs = getActualVMCommand().getCommandline();
        // first argument is the java.exe path...
        result[pos++] = vmArgs[0];
        
        // next follows the vm options
        System.arraycopy(vmArgs, 1, result, pos, vmArgs.length - 1);
        pos += vmArgs.length - 1;
        // properties are part of the vm options...
        if (sysProperties.size() > 0) {
            System.arraycopy(sysProperties.getVariables(), 0,
                             result, pos, sysProperties.size());
            pos += sysProperties.size();
        }
        // classpath is a vm option too..
        Path fullClasspath = classpath != null ? classpath.concatSystemClasspath("ignore") : null;
        if (fullClasspath != null && fullClasspath.toString().trim().length() > 0) {
            result[pos++] = "-classpath";
            result[pos++] = fullClasspath.toString();
        }

        // JDK usage command line says that -jar must be the first option, as there is
        // a bug in JDK < 1.4 that forces the jvm type to be specified as the first
        // option, it is appended here as specified in the docs even though there is
        // in fact no order.
        if (executeJar){
            result[pos++] = "-jar";
        }

        // this is the classname to run as well as its arguments.
        // in case of 'executeJar', the executable is a jar file.
        System.arraycopy(javaCommand.getCommandline(), 0, 
                         result, pos, javaCommand.size());

        return result;
    }

    /**
     * Specify max memory of the JVM
     * -mx or -Xmx depending on VM version
     */
    public void setMaxmemory(String max){
        this.maxMemory = max;
    }


    /**
     * get a string description
     * @return the command line as a string
     */
    public String toString() {
        return Commandline.toString(getCommandline());
    }

    /**
     * Returns a String that describes the command and arguments
     * suitable for verbose output before a call to
     * <code>Runtime.exec(String[])<code>
     *
     * @since Ant 1.5
     */
    public String describeCommand() {
        return Commandline.describeCommand(getCommandline());
    }

    /**
     * Returns a String that describes the java command and arguments
     * for in VM executions.
     *
     * <p>The class name is the executable in this context.</p>
     *
     * @since Ant 1.5
     */
    public String describeJavaCommand() {
        return Commandline.describeCommand(getJavaCommand());
    }

    private Commandline getActualVMCommand() {
        Commandline actualVMCommand = (Commandline) vmCommand.clone();
        if (maxMemory != null) {
            if (vmVersion.startsWith("1.1")) {
                actualVMCommand.createArgument().setValue("-mx" + maxMemory);
            } else {
                actualVMCommand.createArgument().setValue("-Xmx" + maxMemory);
            }
        }
        return actualVMCommand;
    }        

    /**
     * The size of the java command line.
     * @return the total number of arguments in the java command line.
     * @see #getCommandline()
     */
    public int size() {
        int size = getActualVMCommand().size() + javaCommand.size() + sysProperties.size();
        // classpath is "-classpath <classpath>" -> 2 args
        Path fullClasspath = classpath != null ? classpath.concatSystemClasspath("ignore") : null;
        if (fullClasspath != null && fullClasspath.toString().trim().length() > 0) {
            size += 2;
        }
        // jar execution requires an additional -jar option
        if (executeJar){
            size++ ;
        }
        return size;
    }

    public Commandline getJavaCommand() {
        return javaCommand;
    }

    public Commandline getVmCommand() {
        return getActualVMCommand();
    }

    public Path getClasspath() {
        return classpath;
    }

    public void setSystemProperties() throws BuildException {
        sysProperties.setSystem();
    }

    public void restoreSystemProperties() throws BuildException {
        sysProperties.restoreSystem();
    }

    public SysProperties getSystemProperties() {
        return sysProperties;
    }

    /**
     * clone the object; do a deep clone of all fields in the class
     * @return a CommandlineJava object
     */
    public Object clone() {
        CommandlineJava c = new CommandlineJava();
        c.vmCommand = (Commandline) vmCommand.clone();
        c.javaCommand = (Commandline) javaCommand.clone();
        c.sysProperties = (SysProperties) sysProperties.clone();
        c.maxMemory = maxMemory;
        if (classpath != null) {
            c.classpath = (Path) classpath.clone();
        }
        c.vmVersion = vmVersion;
        c.executeJar = executeJar;
        return c;
    }

    /**
     * Clear out the java arguments.
     */
    public void clearJavaArgs() {
        javaCommand.clearArgs();
    }

}
