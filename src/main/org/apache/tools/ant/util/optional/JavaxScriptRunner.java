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

import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.ReflectWrapper;
import org.apache.tools.ant.util.ScriptRunnerBase;

/**
 * This class is used to run scripts using JSR 223.
 * @since Ant 1.7.0
 */
public class JavaxScriptRunner extends ScriptRunnerBase {
    private ReflectWrapper engine;

    /** Debug constant */
    private static final boolean DEBUG = Boolean.getBoolean("JavaxScriptRunner.DEBUG");

    private String compiledScriptRefName; 

    /**
     * Get the name of the manager prefix.
     * @return "javax"
     */
    public String getManagerName() {
        return "javax";
    }

    /** {@inheritDoc}. */
    public boolean supportsLanguage() {
        if (engine != null) {
            return true;
        }
        checkLanguage();
        ClassLoader origLoader = replaceContextLoader();
        try {
            return createEngine() != null;
        } catch (Exception ex) {
            return false;
        } finally {
            restoreContextLoader(origLoader);
        }
    }

    /**
     * Do the work to run the script.
     *
     * @param execName the name that will be passed to the
     *                 scripting engine for this script execution.
     *
     * @exception BuildException if something goes wrong executing the script.
     */
    public void executeScript(String execName) throws BuildException {
        evaluateScript(execName);
    }

    /**
     * Do the work to eval the script.
     *
     * @param execName the name that will be passed to the
     *                 scripting engine for this script execution.
     * @return the result of the evaluation
     * @exception BuildException if something goes wrong executing the script.
     */
    public Object evaluateScript(String execName) throws BuildException {
        checkLanguage();
        ClassLoader origLoader = replaceContextLoader();
        try {

            if(DEBUG) System.out.println("-- JavaxScriptRunner.evaluateScript : compile enabled [" + getCompiled() + "]");

            if (getCompiled()) {

            	if (null == compiledScriptRefName) {
            		compiledScriptRefName = execName + ".compiledScript.0123456789";
            	}
                ReflectWrapper scriptRefObj = getProject().getReference(compiledScriptRefName);

                if (null == scriptRefObj) {

                    ReflectWrapper engine = createEngine();
                    if (engine == null) {
                        throw new BuildException(
                            "Unable to create javax script engine for "
                            + getLanguage());
                    }

                    final Class engineClass = Class.forName("javax.script.ScriptEngine");
                    final Class compilableClass = Class.forName("javax.script.Compilable");
                    final Object wrappedObject = engine.getObject();

                    if (DEBUG) System.out.println("-- JavaxScriptRunner.evaluateScript : wrappedObject [" + wrappedObject.getClass().getName() + "]");
                    if (engineClass.isAssignableFrom(wrappedObject.getClass()) && compilableClass.isAssignableFrom(wrappedObject.getClass())) {

                        if(DEBUG) System.out.println("-- JavaxScriptRunner.evaluateScript : compilable [" + wrappedObject.getClass().getName() + "]");

                        {
                            getProject().log("compile script" + compiledScriptRefName, Project.MSG_VERBOSE);

                            // compilable engine
                            final Object compiledScript = engine.invoke("compile", String.class, getScript());
                            scriptRefObj = new ReflectWrapper(compiledScript);
                        }

                        getProject().log("store compiled script, ref " + compiledScriptRefName, Project.MSG_DEBUG);

                    } else {
                        getProject().log("script compilation not available", Project.MSG_DEBUG);
                        scriptRefObj = new ReflectWrapper(null);
                    }

                    getProject().addReference(compiledScriptRefName, scriptRefObj);
                }

                if (null != scriptRefObj.getObject()) {

                    if (DEBUG) System.out.println("-- JavaxScriptRunner.evaluateScript : execute compiled script");

                    final Object simpleBindings;
                    {
                        final Class simpleBindingsClass  = Class.forName("javax.script.SimpleBindings");
                        simpleBindings = simpleBindingsClass.newInstance();
                    }

                    applyBindings(new ReflectWrapper(simpleBindings));
                    if (DEBUG) System.out.println("-- JavaxScriptRunner.evaluateScript : bindings applied");

                    getProject().log("run compiled script, ref " + compiledScriptRefName, Project.MSG_DEBUG);

                    final Class bindingsClass  = Class.forName("javax.script.Bindings");

                    return scriptRefObj.invoke("eval", bindingsClass, simpleBindings);
                }
            }

            ReflectWrapper engine = createEngine();
            if (engine == null) {
                throw new BuildException(
                    "Unable to create javax script engine for "
                    + getLanguage());
            }

            applyBindings(engine);

            // execute the script
            return engine.invoke("eval", String.class, getScript());

        } catch (BuildException be) {
            //catch and rethrow build exceptions

            // this may be a BuildException wrapping a ScriptException
            // deeply wrapping yet another BuildException - for
            // example because of self.fail() - see
            // https://issues.apache.org/bugzilla/show_bug.cgi?id=47509
            throw unwrap(be);
        } catch (Exception be) {
            //any other exception? Get its cause
            Throwable t = be;
            Throwable te = be.getCause();
            if (te != null) {
                if  (te instanceof BuildException) {
                    throw (BuildException) te;
                } else {
                    t = te;
                }
            }
            throw new BuildException(t);
        } finally {
            restoreContextLoader(origLoader);
        }
    }

    private void applyBindings(ReflectWrapper engine) {
        for (Iterator i = getBeans().keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            Object value = getBeans().get(key);
            if ("FX".equalsIgnoreCase(getLanguage())) {
                engine.invoke(
                    "put", String.class, key
                    + ":" + value.getClass().getName(),
                    Object.class, value);
            } else {
                engine.invoke(
                    "put", String.class, key,
                    Object.class, value);
            }
        }
    }

    private ReflectWrapper createEngine() {
        if (engine != null) {
            return engine;
        }
        ReflectWrapper manager = new ReflectWrapper(
            getClass().getClassLoader(), "javax.script.ScriptEngineManager");
        Object e = manager.invoke(
            "getEngineByName", String.class, getLanguage());
        if (e == null) {
            return null;
        }
        ReflectWrapper ret = new ReflectWrapper(e);
        if (getKeepEngine()) {
            this.engine = ret;
        }
        return ret;
    }

    /**
     * Traverse a Throwable's cause(s) and return the BuildException
     * most deeply nested into it - if any.
     */
    private static BuildException unwrap(Throwable t) {
        BuildException deepest =
            t instanceof BuildException ? (BuildException) t : null;
        Throwable current = t;
        while (current.getCause() != null) {
            current = current.getCause();
            if (current instanceof BuildException) {
                deepest = (BuildException) current;
            }
        }
        return deepest;
    }
}
