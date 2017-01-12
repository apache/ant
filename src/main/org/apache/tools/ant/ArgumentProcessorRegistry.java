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
package org.apache.tools.ant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.LoaderUtils;

/**
 * The global registry for {@link ArgumentProcessor}s.
 * <p>
 * An {@link ArgumentProcessor} implementation can be registered via the system
 * property <code>org.apache.tools.ant.ArgumentProcessor</code>, or via a JDK1.3
 * 'service', by putting the fully qualified name of the implementation into the
 * file <code>META-INF/services/org.apache.tools.ant.ArgumentProcessor</code>
 * <p>
 * Use the system property <code>ant.argument-processor.debug</code> to enable
 * the print of debug log.
 *
 * @since 1.9
 */
public class ArgumentProcessorRegistry {

    private static final String DEBUG_ARGUMENT_PROCESSOR_REPOSITORY = "ant.argument-processor-repo.debug";

    // The message log level is not accessible here because everything
    // is instantiated statically
    private static final boolean DEBUG = "true".equals(System.getProperty(DEBUG_ARGUMENT_PROCESSOR_REPOSITORY));

    private static final String SERVICE_ID = "META-INF/services/org.apache.tools.ant.ArgumentProcessor";

    private static ArgumentProcessorRegistry instance = new ArgumentProcessorRegistry();

    private List<ArgumentProcessor> processors = new ArrayList<ArgumentProcessor>();

    public static ArgumentProcessorRegistry getInstance() {
        return instance;
    }

    private ArgumentProcessorRegistry() {
        collectArgumentProcessors();
    }

    public List<ArgumentProcessor> getProcessors() {
        return processors;
    }

    private void collectArgumentProcessors() {
        try {
            ClassLoader classLoader = LoaderUtils.getContextClassLoader();
            if (classLoader != null) {
                Enumeration<URL> resources = classLoader.getResources(SERVICE_ID);
                while (resources.hasMoreElements()) {
                    URL resource = resources.nextElement();
                    URLConnection conn = resource.openConnection();
                    conn.setUseCaches(false);
                    ArgumentProcessor processor = getProcessorByService(conn.getInputStream());
                    registerArgumentProcessor(processor);
                }
            }

            InputStream systemResource = ClassLoader.getSystemResourceAsStream(SERVICE_ID);
            if (systemResource != null) { //NOSONAR
                ArgumentProcessor processor = getProcessorByService(systemResource);
                registerArgumentProcessor(processor);
            }
        } catch (Exception e) {
            System.err.println("Unable to load ArgumentProcessor from service "
                    + SERVICE_ID + " (" + e.getClass().getName() + ": "
                    + e.getMessage() + ")");
            if (DEBUG) {
                e.printStackTrace(System.err); //NOSONAR
            }
        }
    }

    public void registerArgumentProcessor(String helperClassName)
            throws BuildException {
        registerArgumentProcessor(getProcessor(helperClassName));
    }

    public void registerArgumentProcessor(
            Class< ? extends ArgumentProcessor> helperClass)
            throws BuildException {
        registerArgumentProcessor(getProcessor(helperClass));
    }

    private ArgumentProcessor getProcessor(String helperClassName) {
        try {
            @SuppressWarnings("unchecked")
            Class< ? extends ArgumentProcessor> cl = (Class< ? extends ArgumentProcessor>) Class.forName(helperClassName);
            return getProcessor(cl);
        } catch (ClassNotFoundException e) {
            throw new BuildException("Argument processor class "
                    + helperClassName + " was not found", e);
        }
    }

    private ArgumentProcessor getProcessor(
            Class< ? extends ArgumentProcessor> processorClass) {
        ArgumentProcessor processor;
        try {
            processor = processorClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new BuildException("The argument processor class"
                    + processorClass.getClass().getName()
                    + " could not be instantiated with a default constructor",
                    e);
        }
        return processor;
    }

    public void registerArgumentProcessor(ArgumentProcessor processor) {
        if (processor == null) {
            return;
        }
        processors.add(processor);
        if (DEBUG) {
            System.out.println("Argument processor "
                    + processor.getClass().getName() + " registered.");
        }
    }

    private ArgumentProcessor getProcessorByService(InputStream is)
            throws IOException {
        InputStreamReader isr = null;
        try {
            try {
                isr = new InputStreamReader(is, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                isr = new InputStreamReader(is);
            }
            BufferedReader rd = new BufferedReader(isr);
            String processorClassName = rd.readLine();
            if (processorClassName != null && !"".equals(processorClassName)) {
                return getProcessor(processorClassName);
            }
        } finally {
            FileUtils.close(isr);
        }
        return null;
    }

}
