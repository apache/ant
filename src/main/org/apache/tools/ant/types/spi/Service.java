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
package org.apache.tools.ant.types.spi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;

/**
 * ANT Jar-Task SPI extension
 *
 * @see <a href="https://issues.apache.org/bugzilla/show_bug.cgi?id=31520">
 * https://issues.apache.org/bugzilla/show_bug.cgi?id=31520</a>
 */
public class Service extends ProjectComponent {
    private List<Provider> providerList = new ArrayList<>();
    private String type;

    /**
     * Set the provider classname.
     * @param className the classname of a provider of this service.
     */
    public void setProvider(String className) {
        Provider provider = new Provider();
        provider.setClassName(className);
        providerList.add(provider);
    }

    /**
     * Add a nested provider element.
     * @param provider a provider element.
     */
    public void addConfiguredProvider(Provider provider) {
        provider.check();
        providerList.add(provider);
    }

    /**
     * @return the service type.
     */
    public String getType() {
        return type;
    }

    /**
     * Set the service type.
     * @param type the service type, a classname of
     *             an interface or a class (normally
     *             abstract).
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Return the implementations of this
     * services as an inputstream.
     * @return an inputstream of the classname names
     *         encoded as UTF-8.
     * @throws IOException if there is an error.
     */
    public InputStream getAsStream() throws IOException {
        return new ByteArrayInputStream(
            providerList.stream().map(Provider::getClassName)
                .collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Check if this object is configured correctly as a nested
     * element.
     */
    public void check() {
        if (type == null) {
            throw new BuildException(
                "type attribute must be set for service element",
                getLocation());
        }
        if (type.isEmpty()) {
            throw new BuildException(
                "Invalid empty type classname", getLocation());
        }
        if (providerList.isEmpty()) {
            throw new BuildException(
                "provider attribute or nested provider element must be set!",
                getLocation());
        }
    }
}
