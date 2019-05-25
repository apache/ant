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
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;

import java.util.ArrayList;
import java.util.List;

public class Find extends Task {

    // =====  internal attributes  =====

    private List<String> foundFiles = new ArrayList<>();

    // =====  attribute support  =====

    private String file;
    private String location;
    private List<Path> paths = new ArrayList<>();
    private String delimiter = null;

    public void setFile(String file) {
        this.file = file;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void addPath(Path path) {
        paths.add(path);
    }

    public void setDelimiter(String delim) {
        delimiter = delim;
    }

    // =====  the tasks work  =====

    protected void validate() {
        if (file == null) throw new BuildException("file not set");
        if (location == null) throw new BuildException("location not set");
        if (paths.isEmpty()) throw new BuildException("path not set");
    }

    public void execute() {
        validate();
        for (Path path : paths) {
            for (String includedFile : path.list()) {
                String filename = includedFile.replace('\\','/');
                filename = filename.substring(filename.lastIndexOf("/") + 1);
                if (file.equals(filename) && !foundFiles.contains(includedFile)) {
                    foundFiles.add(includedFile);
                }
            }
        }
        String rv = null;
        if (!foundFiles.isEmpty()) {
            if (delimiter == null) {
                // only the first
                rv = foundFiles.get(0);
            } else {
                // create list
                StringBuilder list = new StringBuilder();
                for (String file : foundFiles) {
                    if (list.length() > 0) {
                        list.append(delimiter);
                    }
                    list.append(file);
                }
                rv = list.toString();
            }
        }


        if (rv != null)
            getProject().setNewProperty(location, rv);
    }

}
