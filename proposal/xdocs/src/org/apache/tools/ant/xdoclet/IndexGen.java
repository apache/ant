package org.apache.tools.ant.xdoclet;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class IndexGen extends Task {
    private File destDir;
    private File rootDir;

    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }


    public void setRootDir(File rootDir) {
        this.rootDir = rootDir;
    }



    public void execute() throws BuildException {
        String[] categories = rootDir.list();

        StringBuffer sb = new StringBuffer();
        sb.append("<html><head><title>xdocs index</title></head>");
        sb.append("<body>");

        for (int i=0; i < categories.length; i++) {
            String category = categories[i];
            File catDir = new File(rootDir, category);

            if (!catDir.isDirectory()) {
                continue;
            }

            sb.append("<h2>" + category + "</h2>");

            sb.append("<ul>");

            String[] tasks = catDir.list();

            for (int j=0; j < tasks.length; j++) {
                String task = tasks[j];
                sb.append("<li>");
                sb.append("<a href=\"" + category + "/" + task + "\">" + task + "</a>");
                sb.append("</li>");
            }

            sb.append("</ul>");

        }

        sb.append("</body></html>");

        FileWriter fw = null;
        try {
            fw = new FileWriter(new File(destDir,"index.html"));
            fw.write(sb.toString());
            fw.close();
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }
}
