/* Copyright (c) 2000 The Apache Software Foundation */

package org.apache.tools.ant;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 *  Stores the information for a single project file. Each project
 *  has its own namespace for variable names and target names.
 *
 *  @author <a href="mailto:mpfoemme@thoughtworks.com">Matthew Foemmel</a>
 */
public class Project {
    private Workspace workspace;
    private String name;
    private URL base;
    private String location;
    private List imports;
    private Map targets;
    private Map variables;

    /**
     *  Constructs a new project. Should only be called by the Workspace class.
     */
    Project(Workspace workspace, String name) {
        this.workspace = workspace;
        this.name = name;
        this.location = null;
        this.imports = new ArrayList();
        this.targets = new HashMap();
        this.variables = new HashMap();
    }

    /**
     *  Returns the workspace that this project belongs to.
     */
    public Workspace getWorkspace() {
        return workspace;
    }

    /**
     *  Returns the name of this project.
     */
    public String getName() {
        return name;
    }

    /**
     *  The directory or jar file where this project file was located.
     */
    public URL getBase() {
        return base;
    }

    /**
     *  The directory where this project file was located.
     *
     *  @throws AntException if this project was loaded from a jar and not a directory.
     */
    public File getBaseDir() {
        if (base.getProtocol().equals("file")) {
            return new File(base.getFile());
        }
        else {
            throw new AntException(base.toString() + " is not a directory");
        }
    }

    public void setBase(URL base) {
        this.base = base;

        if (base.getProtocol().equals("file")) {
            variables.put("ant.base.dir", base.getFile());
        }
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    /**
     *  Creates an empty target with the specied name.
     */
    public Target createTarget(String name) throws BuildException {
        Target target = new Target(this, name);
        Target prevTarget = (Target) targets.put(name, target);

        if (prevTarget != null) {
            String msg = "Target with name \"" + name + "\" already exists";
            if (prevTarget.getLocation() != null) {
                msg = msg + " at " + prevTarget.getLocation();
            }
            throw new BuildException(msg);
        }

        return target;
    }

    /**
     *  Returns the target with the specified name.
     *
     *  @throws AntException if the target doesn't exist.
     */
    public Target getTarget(String name) throws BuildException {
        Target target = (Target) targets.get(name);
        if (target == null) {
            throw new BuildException("Target \"" + name + "\" not found");
        }
        return target;
    }

    public Collection getTargets() {
        return targets.values();
    }

    /**
     *  Indicates the this project relies on variables or targets in another project.
     */
    public Import createImport(String name) {
        Import imp = new Import(this, name);
        imports.add(imp);
        return imp;
    }

    /**
     *  Returns the list of projects that this project imports.
     */
    public List getImports() {
        return imports;
    }

    /**
     *  Returns the value of the variable. Variables from other
     *  projects may be referenced by using the ':' operator.
     */
    public String getVariable(String name) throws BuildException {
        int pos = name.indexOf(Workspace.SCOPE_SEPARATOR);
        if (pos == -1) {
            String value = (String) variables.get(name);
            if (value == null) {
                throw new BuildException("Variable \"" + name + "\" not defined");
            }
            return value;
        }
        else {
            String projectName = name.substring(0, pos);
            String variableName = name.substring(pos + 1);
            Project project = workspace.getProject(projectName);
            return project.getVariable(variableName);
        }
    }

    /**
     *  Sets the value of the variable. Variables from other
     *  projects may be referenced by using the ':' operator.
     */
    public void setVariable(String name, String value) throws BuildException {
        int pos = name.indexOf(Workspace.SCOPE_SEPARATOR);
        if (pos == -1) {
            variables.put(name, value);
        }
        else {
            String projectName = name.substring(0, pos);
            String variableName = name.substring(pos + 1);
            Project project = workspace.getProject(projectName);
            project.setVariable(variableName, value);
        }
    }

    public char getPathSeparator() {
        return ':';
    }
}
