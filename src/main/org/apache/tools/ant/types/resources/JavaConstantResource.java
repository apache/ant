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
package org.apache.tools.ant.types.resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

/**
 * A resource that is a java constant.
 * This lets you extract values off the classpath and use them elsewhere
 * @since Ant 1.7
 */
public class JavaConstantResource extends AbstractClasspathResource {

    /**
     * open the input stream from a specific classloader
     *
     * @param cl the classloader to use. Will be null if the system classloader is used
     * @return an open input stream for the resource
     * @throws IOException if an error occurs.
     */
    @Override
    protected InputStream openInputStream(ClassLoader cl) throws IOException {
        String constant = getName();
        if (constant == null) {
            throw new IOException("Attribute 'name' must be set.");
        }
        int index = constant.lastIndexOf('.');
        if (index < 0) {
            throw new IOException("No class name in " + constant);
        }
        String classname = constant.substring(0, index);
        String fieldname = constant.substring(index + 1);
        try {
            Class<?> clazz =
                cl != null
                ? Class.forName(classname, true, cl)
                : Class.forName(classname);
            Field field = clazz.getField(fieldname);
            String value = field.get(null).toString();
            return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
        } catch (ClassNotFoundException e) {
            throw new IOException("Class not found:" + classname);
        } catch (NoSuchFieldException e) {
            throw new IOException(
                "Field not found:" + fieldname + " in " + classname);
        } catch (IllegalAccessException e) {
            throw new IOException("Illegal access to :" + fieldname + " in " + classname);
        } catch (NullPointerException npe) {
            throw new IOException("Not a static field: " + fieldname + " in " + classname);
        }
    }

}
