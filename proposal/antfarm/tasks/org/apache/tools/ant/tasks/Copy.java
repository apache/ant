/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant.tasks;

import java.io.*;
import org.apache.tools.ant.*;

public class Copy extends Task {
    private String src;
    private String dest;

    public void execute() throws BuildException {
        try {
            FileInputStream in = new FileInputStream(src);
            FileOutputStream out = new FileOutputStream(dest);

            byte[] buf = new byte[4096];
            int len = 0;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }
        catch(FileNotFoundException exc) {
            throw new BuildException("File not found");
        }
        catch(IOException exc) {
            throw new AntException("Error copying files", exc);
        }
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getDest() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }
}