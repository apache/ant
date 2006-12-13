/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.util.optional;

import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.apache.bsf.BSFEngine;


import java.util.Iterator;
import java.util.Hashtable;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import org.apache.tools.ant.util.ReflectUtil;
import org.apache.tools.ant.util.ScriptRunnerBase;

/**
 * This class is used to run BSF scripts
 *
 */
public class ScriptRunner extends ScriptRunnerBase {
    // Register Groovy ourselves, since BSF did not
    // natively support it in versions previous to 1.2.4.
    static {
        BSFManager.registerScriptingEngine(
            "groovy",
            "org.codehaus.groovy.bsf.GroovyEngine",
            new String[] {"groovy", "gy"});
    }

    private BSFEngine  engine;
    private BSFManager manager;

    /**
     * Get the name of the manager prefix.
     * @return "bsf"
     */
    public String getManagerName() {
        return "bsf";
    }

    /**
     * Check if bsf supports the language.
     * @return true if bsf can create an engine for this language.
     */
    public boolean supportsLanguage() {
        Hashtable table = (Hashtable) ReflectUtil.getField(
            new BSFManager(), "registeredEngines");
        String engineClassName = (String) table.get(getLanguage());
        if (engineClassName == null) {
            getProject().log(
                "This is no BSF engine class for language '"
                + getLanguage() + "'",
                Project.MSG_VERBOSE);
            return false;
        }
        try {
            getScriptClassLoader().loadClass(engineClassName);
            return true;
        } catch (Throwable ex) {
            getProject().log(
                "unable to create BSF engine class for language '"
                + getLanguage() + "'",
                ex,
                Project.MSG_VERBOSE);
            return false;
        }
    }

    /**
     * Do the work.
     *
     * @param execName the name that will be passed to BSF for this script
     *        execution.
     *
     * @exception BuildException if someting goes wrong exectuing the script.
     */
    public void executeScript(String execName) throws BuildException {
        checkLanguage();
        ClassLoader origLoader = replaceContextLoader();
        try {
            BSFManager m = createManager();
            declareBeans(m);
            // execute the script
            if (engine == null) {
                m.exec(getLanguage(), execName, 0, 0, getScript());
            } else {
                engine.exec(execName, 0, 0, getScript());
            }
        } catch (BSFException be) {
            throwBuildException(be);
        } finally {
            restoreContextLoader(origLoader);
        }
    }

    /**
     * Do the work.
     *
     * @param execName the name that will be passed to BSF for this script
     *        execution.
     * @return the result of the evalulation
     * @exception BuildException if someting goes wrong exectuing the script.
     */
    public Object evaluateScript(String execName)
        throws BuildException {
        checkLanguage();
        ClassLoader origLoader = replaceContextLoader();
        try {
            BSFManager m = createManager();
            declareBeans(m);
            // execute the script
            if (engine == null) {
                return m.eval(getLanguage(), execName, 0, 0, getScript());
            } else {
                return engine.eval(execName, 0, 0, getScript());
            }
        } catch (BSFException be) {
            throwBuildException(be);
            // NotReached
            return null;
        } finally {
            restoreContextLoader(origLoader);
        }
    }

    /**
     * Throw a buildException in place of a BSFException.
     * @param be BSFException to convert.
     * @throws BuildException the conveted exception.
     */
    private void throwBuildException(BSFException be) {
        Throwable t = be;
        Throwable te = be.getTargetException();
        if (te != null) {
            if  (te instanceof BuildException) {
                throw (BuildException) te;
            } else {
                t = te;
            }
        }
        throw new BuildException(t);
    }

    private void declareBeans(BSFManager m) throws BSFException {
        for (Iterator i = getBeans().keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            Object value = getBeans().get(key);
            if (value != null) {
                m.declareBean(key, value, value.getClass());
            } else {
                // BSF uses a hashtable to store values
                // so cannot declareBean with a null value
                // So need to remove any bean of this name as
                // that bean should not be visible
                m.undeclareBean(key);
            }
        }
    }

    private BSFManager createManager() throws BSFException {
        if (manager != null) {
            return manager;
        }
        BSFManager m = new BSFManager();
        m.setClassLoader(getScriptClassLoader());
        if (getKeepEngine()) {
            BSFEngine e = manager.loadScriptingEngine(getLanguage());
            this.manager = m;
            this.engine  = e;
        }
        return m;
    }
}
