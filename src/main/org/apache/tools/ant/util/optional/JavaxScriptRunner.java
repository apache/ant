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

package org.apache.tools.ant.util.optional;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.ScriptRunnerBase;

/**
 * This class is used to run scripts using JSR 223.
 * @since Ant 1.7.0
 */
public class JavaxScriptRunner extends ScriptRunnerBase {
    private ScriptEngine keptEngine;
    private CompiledScript compiledScript;

    /**
     * Get the name of the manager prefix.
     * @return "javax"
     */
    @Override
    public String getManagerName() {
        return "javax";
    }

    /** {@inheritDoc}. */
    @Override
    public boolean supportsLanguage() {
        if (keptEngine != null) {
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
    @Override
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
            if (getCompiled()) {
                final String compiledScriptRefName =
                    String.format("%s.%s.%d.%d", MagicNames.SCRIPT_CACHE,
                        getLanguage(), Objects.hashCode(getScript()),
                        Objects.hashCode(getClass().getClassLoader()));

                if (null == compiledScript) {
                    compiledScript = getProject().getReference(compiledScriptRefName);
                }
                if (null == compiledScript) {
                    final ScriptEngine engine = createEngine();
                    if (engine == null) {
                        throw new BuildException(
                            "Unable to create javax script engine for %s",
                            getLanguage());
                    }
                    if (engine instanceof Compilable) {
                        getProject().log("compile script " + execName,
                            Project.MSG_VERBOSE);

                        compiledScript =
                            ((Compilable) engine).compile(getScript());
                    } else {
                        getProject().log(
                            "script compilation not available for " + execName,
                            Project.MSG_VERBOSE);
                        compiledScript = null;
                    }
                    getProject().addReference(compiledScriptRefName,
                        compiledScript);
                }
                if (null != compiledScript) {
                    final Bindings bindings = new SimpleBindings();

                    applyBindings(bindings::put);

                    getProject().log(
                        "run compiled script " + compiledScriptRefName,
                        Project.MSG_DEBUG);

                    return compiledScript.eval(bindings);
                }
            }

            ScriptEngine engine = createEngine();
            if (engine == null) {
                throw new BuildException(
                    "Unable to create javax script engine for "
                        + getLanguage());
            }

            applyBindings(engine::put);

            return engine.eval(getScript());

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
                if (te instanceof BuildException) {
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

    private void applyBindings(BiConsumer<String, Object> target) {
        Map<String, Object> source = getBeans();

        if ("FX".equalsIgnoreCase(getLanguage())) {
            source = source.entrySet().stream()
                .collect(Collectors.toMap(e -> String.format("%s:%s", e.getKey(),
                        e.getValue().getClass().getName()), Map.Entry::getValue));
        }
        source.forEach(target);
    }

    private ScriptEngine createEngine() {
        if (keptEngine != null) {
            return keptEngine;
        }
        ScriptEngine result =
            new ScriptEngineManager().getEngineByName(getLanguage());
        if (result != null && getKeepEngine()) {
            this.keptEngine = result;
        }
        return result;
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
