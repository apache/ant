// -------------------------------------------------------------------------------
// Copyright (c)2000 Apache Software Foundation
// -------------------------------------------------------------------------------

package org.apache.ant;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;

/**
 * Helper class to build Project object trees.
 *
 * XXX right now this class only deals with the primary levels (project/target/task)
 * and nothing else. Also, it only supports attributes....
 *
 * @author James Duncan Davidson (duncan@apache.org)
 */
public class ProjectBuilder {
        
    // -----------------------------------------------------------------
    // PRIVATE MEMBERS
    // -----------------------------------------------------------------
    
    /**
     *
     */
    private AntFrontEnd frontEnd;
    
    /**
     *
     */
    private SAXParserFactory parserFactory;
    
    /**
     *
     */
    private TaskManager taskManager;
        
    // -----------------------------------------------------------------
    // CONSTRUCTORS
    // -----------------------------------------------------------------
   
   
    /**
     * Creates a new project builder that will build projects for the given
     * Ant.
     */
    public ProjectBuilder(AntFrontEnd frontEnd) {
        this.frontEnd = frontEnd;
        taskManager = new TaskManager(frontEnd);
        parserFactory = SAXParserFactory.newInstance();
        parserFactory.setValidating(false);  
    }
    
    // -----------------------------------------------------------------
    // PUBLIC METHODS
    // -----------------------------------------------------------------
    
    /**
     * Builds a project from the given file.
     */
    public Project buildFromFile(File file) throws AntException {
        try {
            SAXParser parser = parserFactory.newSAXParser();
            BuilderHandlerBase bhb = new BuilderHandlerBase();
            bhb.setProjectFileLocation(file);
            parser.parse(file, bhb);
            Project project = bhb.getProject();
            project.setFrontEnd(frontEnd);
            return project;
        } catch (ParserConfigurationException pce) {
            throw new AntException(pce);
        } catch (SAXException se) {
            Exception e = se.getException();
            if (e != null && e instanceof AntException) {
                // it's one of our own thrown from inside the parser to stop it
                throw (AntException)e;
            }
            throw new AntException(se);
        } catch (IOException ioe) {
            throw new AntException(ioe);
        }
    }
    
    /**
     * Returns the TaskManager associated with this ProjectBuilder and
     * the projects that it builds
     */
    public TaskManager getTaskManager() {
        return taskManager;
    }
    
    // -----------------------------------------------------------------
    // INNER CLASSES
    // -----------------------------------------------------------------    
    
    /**
     * Inner class that implements the needed SAX methods to get all the
     * data needed out of a build file.
     */
    class BuilderHandlerBase extends HandlerBase {
    
        private static final int STATE_START = 0;
        private static final int STATE_PROJECT = 1;
        private static final int STATE_TARGET = 2;
        private static final int STATE_TASK = 3;
        private static final int STATE_DESCRIPTION = 4;
        private static final int STATE_PROPERTY = 5;
        private static final int STATE_FINISHED = 99;
    
        private int state = STATE_START;
        
        private Vector tagCharDataStack = new Vector();
        
        private Target currentTarget;
        private Task currentTask;
    
        Project project = new Project(frontEnd, taskManager);
    
        Project getProject() {
            return project;
        }
        
        void setProjectFileLocation(File file) {
            project.setBaseDir(file.getParentFile());
        }
        
        public void startElement(String name, AttributeList atts) throws SAXException {

            StringBuffer tagCharData = new StringBuffer();
            tagCharDataStack.insertElementAt(tagCharData, 0);
            
            switch (state) {
            
              case STATE_START:
                if (name.equals("project")) {
                    state = STATE_PROJECT;
                    String projectName = atts.getValue("name");
                    if (projectName != null) {
                        project.setName(projectName);
                    } else {
                        String msg = "Project element doesn't contain a name attribute";
                        AntException ae = new AntException(msg);
                        throw new SAXException(ae);
                    }
                    String defaultTarget = atts.getValue("default");
                    if (defaultTarget != null) {
                        project.setDefaultTargetName(defaultTarget);
                    }
                    String baseDirName = atts.getValue("basedir");
                    if (baseDirName != null) {
                        // XXX need to check to see if base dir exists
                        project.setBaseDir(new File(baseDirName));
                    }
                } else {
                    String msg = "Project file doesn't contain a project element as " +
                                 "its root node";
                    AntException ae = new AntException(msg);
                    throw new SAXException(ae);
                }
                break;
                
              case STATE_PROJECT:
              
                // valid tags in a project object are: description, property, and target
              
                if (name.equals("description")) {
                    state = STATE_DESCRIPTION;
                } else if (name.equals("property")) {
                    state = STATE_PROPERTY;
                    String propertyName = atts.getValue("name");
                    String propertyValue = atts.getValue("value");
                    if (propertyName == null) {
                        String msg = "Name attribute must be present on property";
                        AntException ae = new AntException(msg);
                        throw new SAXException(ae);
                    } else if (propertyValue == null) {
                        String msg = "Value attribute must be present on property";
                        AntException ae = new AntException(msg);
                        throw new SAXException(ae);
                    } else {
                        project.setProperty(propertyName, propertyValue);
                    }
                } else if (name.equals("target")) {
                    state = STATE_TARGET;
                    String targetName = atts.getValue("name");
                    if (targetName != null) {
                        currentTarget = new Target(targetName);
                        project.addTarget(currentTarget);
                    } else {
                        // XXX figure out which target we're talking about! 
                        // Like a location
                        String msg = "Target element doesn't contain a name attribute";
                        AntException ae = new AntException(msg);
                        throw new SAXException(ae);
                    }
                    String depends = atts.getValue("depends");
                    if (depends != null) {
                        StringTokenizer tok = new StringTokenizer(depends, ",", false);
                        while(tok.hasMoreTokens()) {
                            currentTarget.addDependancy(tok.nextToken().trim());
                        }
                    }
                                            
                    // XXX add dependency checks
                } else {
                    System.out.println("Expecting target, got: " + name);
                    // XXX exception out
                }
                break;
                
              case STATE_TARGET:
              
                // Valid tags inside target: task
              
                state = STATE_TASK;
                //System.out.println("Getting task: " + name + " for target " + 
                //                   currentTarget);
                // XXX need to validate that task type (name) exists in system
                // else exception out.
                currentTask = new Task(name);
                currentTarget.addTask(currentTask);
                for (int i = 0; i < atts.getLength(); i++) {
                    String atName = atts.getName(i);
                    String atValue = atts.getValue(i);
                    currentTask.addAttribute(atName, atValue);
                }
                break;
                
              case STATE_TASK:
              
                // data in here needs to be reflected into tasks
                
                System.out.println("Not yet supporting tags inside of tasks!");
                System.out.println("The project build will probably bust right here");
                
                break;
                
              default:
                System.out.println("I'm not sure, but we're off base here: " + name);
                // XXX exception out
            }
        }
        
        public void characters(char ch[], int start, int length) throws SAXException {
            StringBuffer buf = (StringBuffer)tagCharDataStack.elementAt(0);
            buf.append(ch, start, length);
        }
        
        public void endElement(String name) throws SAXException {
            
            StringBuffer elementData = (StringBuffer)tagCharDataStack.elementAt(0);
            tagCharDataStack.removeElementAt(0);
            
            switch (state) {
            
              case STATE_TASK:
                state = STATE_TARGET;
                break;
            
              case STATE_TARGET:
                if (name.equals("target")) {
                    state = STATE_PROJECT;
                } else {
                    System.out.println("Expecting to get an end of target, got: " + name);
                    // XXX exception out.
                }
                break;
            
              case STATE_DESCRIPTION:
                if (name.equals("description")) {
                    state = STATE_PROJECT;
                    project.setDescription(elementData.toString().trim());
                } else {
                    System.out.println("Expecting to get an end of description, got: " +
                        name);
                    // XXX exception out.
                }
                break;
            
              case STATE_PROPERTY:
                if (name.equals("property")) {
                    state = STATE_PROJECT;
                } else {
                    System.out.println("Expecting to get end of property, got: " + name);
                    // XXX exception out
                }
                break;
            
              case STATE_PROJECT:
                if (name.equals("project")) {
                    state = STATE_FINISHED;
                } else {
                    System.out.println("Expecting to get end of project, got: " + name);
                    // XXX exception out;
                }
                break;
            
              default:
                System.out.println("I'm not sure what we are ending here: " + name);
                // XXX exception out;
            }
        }
    }
}
