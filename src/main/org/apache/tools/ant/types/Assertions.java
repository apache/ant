/*
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2003 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution, if
 *  any, must include the following acknowlegement:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowlegement may appear in the software itself,
 *  if and wherever such third-party acknowlegements normally appear.
 *
 *  4. The names "The Jakarta Project", "Ant", and "Apache Software
 *  Foundation" must not be used to endorse or promote products derived
 *  from this software without prior written permission. For written
 *  permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache"
 *  nor may "Apache" appear in their names without prior written
 *  permission of the Apache Group.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * implement the assertion datatype. This type describes
 * assertion settings for the &lt;java&gt; task and derivatives.
 * One can set the system assertions, and enable/disable those in
 * packages & classes.
 * Assertions can only be enabled or disabled when forking Java.
 *
 * Example: set system assertions and all org.apache packages except
 * for ant, and the class org.apache.tools.ant.Main.
 * <pre>
 * &lt;assertions enableSystemAssertions="true" &gt;
 *   &lt;enable package="org.apache" /&gt;
 *   &lt;disable package="org.apache.ant" /&gt;
 *   &lt;enable class="org.apache.tools.ant.Main"/&gt;
 * &lt;/assertions&gt;
 *</pre>
 * Disable system assertions; enable those in the anonymous package
 * <pre>
 * &lt;assertions enableSystemAssertions="false" &gt;
 *   &lt;enable package="..." /&gt;
 * &lt;/assertions&gt;
 * </pre>
 * enable assertions in a class called Test 
 * <pre>
 * &lt;assertions &gt;
 *   &lt;enable class="Test" /&gt;
 * &lt;/assertions&gt;
 * </pre>
 * This type is a datatype, so you can declare assertions and use them later
 *
 * <pre>
 * &lt;assertions id="project.assertions" &gt;
 *   &lt;enable project="org.apache.test" /&gt;
 * &lt;/assertions&gt;
 *
 * &lt;assertions refid="project.assertions" /&gt;
 *
 * </pre>
 * @since Ant 1.6
 * @author steve loughran
 */
public class Assertions extends DataType {

    /**
     * enable/disable sys assertions; null means undefined
     */
    private Boolean enableSystemAssertions;

    /**
     * list of type BaseAssertion
     */
    private List assertionList=new ArrayList();


    /**
     * enable assertions
     * @param assertion
     */
    public void addEnable(EnabledAssertion assertion) {
        checkChildrenAllowed();
        assertionList.add(assertion);
    }

    /**
     * disable assertions
     * @param assertion
     */
    public void addDisable(EnabledAssertion assertion) {
        checkChildrenAllowed();
        assertionList.add(assertion);
    }

    /**
     * enable or disable system assertions
     * @param enableSystemAssertions
     */
    public void setEnableSystemAssertions(Boolean enableSystemAssertions) {
        checkAttributesAllowed();
        this.enableSystemAssertions = enableSystemAssertions;
    }

    /**
     * Set the value of the refid attribute.
     *
     * <p>Subclasses may need to check whether any other attributes
     * have been set as well or child elements have been created and
     * thus override this method. if they do the must call
     * <code>super.setRefid</code>.</p>
     */
    public void setRefid(Reference ref) {
        if(assertionList.size()>0 || enableSystemAssertions!=null) {
            throw tooManyAttributes();
        }
        super.setRefid(ref);
    }

    /**
     * get whatever we are referencing to. This could be ourself.
     * @return the object that contains the assertion info
     */
    private Assertions getFinalReference() {
        if(getRefid()==null) {
            return this;
        } else {
            Object o = getRefid().getReferencedObject(getProject());
            if(!(o instanceof Assertions)) {
                throw new BuildException("reference is of wrong type");
            }
            return (Assertions)o;
        }
    }

    /**
     * apply all the assertions to the command.
     * @param command
     */
    public void applyAssertions(CommandlineJava command) {
        Assertions clause=getFinalReference();
        //do the system assertions
        if(Boolean.TRUE.equals(clause.enableSystemAssertions)) {
            addVmArgument(command,"-enablesystemassertions");
        } else if (Boolean.FALSE.equals(clause.enableSystemAssertions)) {
            addVmArgument(command, "-disablesystemassertions");
        }

        //now any inner assertions
        Iterator it= clause.assertionList.iterator();
        while (it.hasNext()) {
            BaseAssertion assertion = (BaseAssertion) it.next();
            String arg=assertion.toCommand();
            addVmArgument(command, arg);
        }
    }

    /**
     * helper method to add a string JVM argument to a command
     * @param command
     * @param arg
     */
    private static void addVmArgument(CommandlineJava command, String arg) {
        Commandline.Argument argument;
        argument = command.createVmArgument();
        argument.setValue(arg);
    }

    /**
     * base class for our assertion elements.
     */

    public static abstract class BaseAssertion {
        private String packageName;
        private String className;

        /**
         * name a class
         * @param className
         */
        public void setClass(String className) {
            this.className = className;
        }

        /**
         * name a package
         * @param packageName
         */
        public void setPackage(String packageName) {
            this.packageName = packageName;
        }

        /**
         * what is the class name?
         * @return classname or null
         * @see #setClass
         */
        protected String getClassName() {
            return className;
        }

        /**
         * what is the package name?
         * @return package name or null
         * @see #setPackage
         */
        protected String getPackageName() {
            return packageName;
        }

        /**
         * get the prefix used to begin the command; -ea or -da.
         * @return prefix
         */
        public abstract String getCommandPrefix();

        /**
         * create a full command string from this class
         * @throws BuildException in case of trouble
         * @return
         */
        public String toCommand() {
            //catch invalidness
            if(getPackageName()!=null && getClassName()!=null) {
                throw new BuildException("Both package and class have been set");
            }
            StringBuffer command=new StringBuffer(getCommandPrefix());
            //see if it is a package or a class
            if(getPackageName() != null) {
                //packages get a ... prefix
                command.append(':');
                command.append(getPackageName());
                if(!command.toString().endsWith("...")) {
                    //append the ... suffix if not there already
                    command.append("...");
                }
            } else if(getClassName()!=null) {
                //classes just get the classname
                command.append(':');
                command.append(getClassName());
            }
            return command.toString();
        }
    }


    /**
     * an enabled assertion enables things
     */
    public static class EnabledAssertion extends BaseAssertion {
        /**
         * get the prefix used to begin the command; -ea or -da.
         * @return prefix
         */
        public String getCommandPrefix() {
            return "-ea";
        }

    }

    /**
     * A disabled assertion disables things
     */
    public static class DisabledAssertion extends BaseAssertion {
        /**
         * get the prefix used to begin the command; -ea or -da.
         * @return prefix
         */
        public String getCommandPrefix() {
            return "-da";
        }

    }
}
