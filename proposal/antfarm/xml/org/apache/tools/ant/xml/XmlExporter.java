package org.apache.tools.ant.xml;

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;

public class XmlExporter {
    public void exportProject(Project project, Writer out) throws IOException {
        out.write("<project name=\"" + project.getName() + "\">\n");

        Iterator itr = project.getTargets().iterator();
        while (itr.hasNext()) {
            Target target = (Target) itr.next();
            writeTarget(target, out);
        }

        out.write("</project>\n");
    }

    private void writeTarget(Target target, Writer out) throws IOException {
        out.write("\t<target name=\"" + target.getName() + "\" depends=\"" + concat(target.getDepends()) + ">\n");
        out.write("\t</target>\n");
    }

    public String concat(List depends) throws IOException {
        StringBuffer buf = new StringBuffer();
        Iterator itr = depends.iterator();
        while (itr.hasNext()) {
            String depend = (String) itr.next();
            buf.append(depend);
            if (itr.hasNext()) {
                buf.append(" ");
            }
        }
        return buf.toString();
    }

    public static void main(String[] args) throws Exception {
        Workspace workspace = new Workspace(new XmlImporter());
        Project project = workspace.importProject("ant");
        Writer out = new OutputStreamWriter(System.out);
        new XmlExporter().exportProject(project, out);
        out.flush();
    }
}