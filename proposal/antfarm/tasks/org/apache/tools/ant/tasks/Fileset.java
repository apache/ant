/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant.tasks;

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;

public class Fileset {
    private String src;

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public void getFiles(List results) throws BuildException {
        if (src == null) {
            throw new BuildException("Missing property \"src\"", null); //LOCATION
        }

        File dir = new File(src);
        if (!dir.exists()) {
            throw new BuildException(src + " does not exist", null); // LOCATION!!!
        }
        getFiles(dir, results);
    }

    private void getFiles(File file, List results) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                getFiles(files[i], results);
            }
        }
        else if (file.getPath().endsWith(".java")) {
            results.add(file.getPath());
        }
    }
}