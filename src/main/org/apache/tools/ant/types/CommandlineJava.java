/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.types;

import java.util.*;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;

/*
 *
 * @author thomas.haas@softwired-inc.com
 */
public class CommandlineJava implements Cloneable {

    private Commandline vmCommand = new Commandline();
    private Commandline javaCommand = new Commandline();
    private SysProperties sysProperties = new SysProperties();
    private Path classpath = null;
    private String vmVersion;

    /**
     * Specialized Environment class for System properties
     */
    public static class SysProperties extends Environment implements Cloneable {
        Properties sys = null;

        public String[] getVariables() throws BuildException {
            String props[] = super.getVariables();
      
            if (props == null) return null;

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
                Properties p = new Properties(sys = System.getProperties());
        
                for (Enumeration e = variables.elements(); e.hasMoreElements(); ) {
                    Environment.Variable v = (Environment.Variable) e.nextElement();
                    p.put(v.getKey(), v.getValue());
                }
                System.setProperties(p);
            } catch (SecurityException e) {
                throw new BuildException("Cannot modify system properties", e);
            }
        }

        public void restoreSystem() throws BuildException {
            if (sys == null)
                throw new BuildException("Unbalanced nesting of SysProperties");

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
            } catch(CloneNotSupportedException e){return null;}
        }

    }

    public CommandlineJava() {
        setVm("java");
        setVmversion(org.apache.tools.ant.Project.getJavaVersion());
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

    public void setClassname(String classname) {
        javaCommand.setExecutable(classname);
    }

    public String getClassname() {
        return javaCommand.getExecutable();
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

    public String[] getCommandline() {
        Path fullClasspath = classpath != null ? classpath.concatSystemClasspath("ignore") : null;
        int size = 
            vmCommand.size() + javaCommand.size() + sysProperties.size();
        if (fullClasspath != null && fullClasspath.size() > 0) {
            size += 2;
        }
        
        String[] result = new String[size];
        System.arraycopy(vmCommand.getCommandline(), 0, 
                         result, 0, vmCommand.size());

        int pos = vmCommand.size();
        if (sysProperties.size() > 0) {
            System.arraycopy(sysProperties.getVariables(), 0,
                             result, pos, sysProperties.size());
            pos += sysProperties.size();
        }
        if (fullClasspath != null && fullClasspath.toString().trim().length() > 0) {
            result[pos++] = "-classpath";
            result[pos++] = fullClasspath.toString();
        }
        System.arraycopy(javaCommand.getCommandline(), 0, 
                         result, pos, javaCommand.size());
        return result;
    }


    public String toString() {
        return Commandline.toString(getCommandline());
    }

    public int size() {
        int size = vmCommand.size() + javaCommand.size();
        if (classpath != null && classpath.size() > 0) {
            size += 2;
        }
        return size;
    }

    public Commandline getJavaCommand() {
        return javaCommand;
    }

    public Commandline getVmCommand() {
        return vmCommand;
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

    public Object clone() {
        CommandlineJava c = new CommandlineJava();
        c.vmCommand = (Commandline) vmCommand.clone();
        c.javaCommand = (Commandline) javaCommand.clone();
        c.sysProperties = (SysProperties) sysProperties.clone();
        if (classpath != null) {
            c.classpath = (Path) classpath.clone();
        }
        c.vmVersion = vmVersion;
        return c;
    }

    /**
     * Clear out the java arguments.
     */
    public void clearJavaArgs() {
        javaCommand.clearArgs();
    }

}
