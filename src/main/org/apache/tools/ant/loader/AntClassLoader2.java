/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.loader;

import java.io.File;
import java.io.IOException;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.Project;
import java.util.jar.Manifest;
import java.util.jar.JarFile;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.net.URL;
import java.net.MalformedURLException;

public class AntClassLoader2 extends AntClassLoader {
    protected Class defineClassFromData(File container, byte[] classData,
                                        String className) throws IOException {

        definePackage(container, className);                                            
        return defineClass(className, classData, 0, classData.length,
                           Project.class.getProtectionDomain());                                             
                                            
    }
    
    protected void definePackage(File container, String className) 
        throws IOException {
        int classIndex = className.lastIndexOf('.');
        if (classIndex == -1) {
            return;
        }
        
        String packageName = className.substring(0, classIndex);
        if (getPackage(packageName) != null) {
            // already defined 
            return;
        }
        
        // define the package now 
        Manifest manifest = null;
        if (!container.isDirectory()) {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(container);
                manifest = jarFile.getManifest();
            } finally {
                if (jarFile != null) {
                    jarFile.close();
                }
            }
        }
        
        if (manifest == null) {
            definePackage(packageName, null, null, null, null, null, null, null);
        } else {
            definePackage(container, packageName, manifest);
        }
    }
    
    protected void definePackage(File container, String packageName, Manifest manifest) {
        String sectionName = packageName.replace('.', '/') + "/";

        String specificationTitle = null;
        String specificationVendor = null;
        String specificationVersion = null;
        String implementationTitle = null;
        String implementationVendor = null;
        String implementationVersion = null;
        String sealedString = null;
        URL sealBase = null;
        
        Attributes sectionAttributes = manifest.getAttributes(sectionName);
        if (sectionAttributes != null) {
            specificationTitle   
                = sectionAttributes.getValue(Name.SPECIFICATION_TITLE);
            specificationVendor   
                = sectionAttributes.getValue(Name.SPECIFICATION_VENDOR);
            specificationVersion   
                = sectionAttributes.getValue(Name.SPECIFICATION_VERSION);
            implementationTitle   
                = sectionAttributes.getValue(Name.IMPLEMENTATION_TITLE);
            implementationVendor   
                = sectionAttributes.getValue(Name.IMPLEMENTATION_VENDOR);
            implementationVersion   
                = sectionAttributes.getValue(Name.IMPLEMENTATION_VERSION);
            sealedString   
                = sectionAttributes.getValue(Name.SEALED);
        }
        
        Attributes mainAttributes = manifest.getMainAttributes();
        if (mainAttributes != null) {
            if (specificationTitle == null) {
                specificationTitle   
                    = mainAttributes.getValue(Name.SPECIFICATION_TITLE);
            }
            if (specificationVendor == null) {
                specificationVendor   
                    = mainAttributes.getValue(Name.SPECIFICATION_VENDOR);
            }
            if (specificationVersion == null) {
                specificationVersion   
                    = mainAttributes.getValue(Name.SPECIFICATION_VERSION);
            }
            if (implementationTitle == null) {
                implementationTitle   
                    = mainAttributes.getValue(Name.IMPLEMENTATION_TITLE);
            }
            if (implementationVendor == null) {
                implementationVendor   
                    = mainAttributes.getValue(Name.IMPLEMENTATION_VENDOR);
            }
            if (implementationVersion == null) {
                implementationVersion   
                    = mainAttributes.getValue(Name.IMPLEMENTATION_VERSION);
            }
            if (sealedString == null) {
                sealedString   
                    = mainAttributes.getValue(Name.SEALED);
            }
        }
        
        if (sealedString != null && sealedString.equalsIgnoreCase("true")) {
            try {
                sealBase = new URL("file:" + container.getPath());
            } catch (MalformedURLException e) {
                // ignore
            }
        }
        
        definePackage(packageName, specificationTitle, specificationVersion, 
                      specificationVendor, implementationTitle, 
                      implementationVersion, implementationVendor, sealBase);        
    }
}

