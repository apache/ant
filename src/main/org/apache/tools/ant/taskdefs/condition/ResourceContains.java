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
package org.apache.tools.ant.taskdefs.condition;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileUtils;

/**
 * &lt;resourcecontains&gt;
 * Is a string contained in a resource (file currently)?
 * @since Ant 1.7.1
 */
public class ResourceContains implements Condition {

    private String substring;
    private Resource resource;
    private boolean casesensitive = true;
    
    /**
     * Sets the resource to search
     * @param r
     */
    public void setResource(String r) {
        this.resource = new FileResource(new File(r));
    }

    /**
     * Sets the substring to look for
     * @param substring
     */
    public void setSubstring(String substring) {
        this.substring = substring;
    }

    /**
     * Sets case sensitivity
     * @param casesensitive
     */
    public void setCasesensitive(boolean casesensitive) {
        this.casesensitive = casesensitive;
    }
    
    /**
     * Evaluates
     * Returns true if the substring is contained in the resource
     */
    public boolean eval() throws BuildException {
        if (resource == null || substring == null) {
            throw new BuildException("both resource and substring are required "
                                     + "in <resourcecontains>");
        }
        
        if (resource.getSize() == 0) {
            return false;
        }
        
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String contents = FileUtils.readFully(reader);
            if(casesensitive) {
                if(contents.indexOf(substring) > -1) {
                    return true;
                }
            } else {
                if(contents.toLowerCase().indexOf(substring) > -1) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            throw new BuildException("There was a problem accessing resource : "+resource);
        } finally {
            FileUtils.close(reader);
        }
    }
}