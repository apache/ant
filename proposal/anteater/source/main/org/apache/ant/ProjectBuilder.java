// -------------------------------------------------------------------------------
// Copyright (c)2000 Apache Software Foundation
// -------------------------------------------------------------------------------

package org.apache.ant;

import java.io.*;
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
class ProjectBuilder {

    private SAXParserFactory parserFactory;
        
    // -----------------------------------------------------------------
    // CONSTRUCTORS
    // -----------------------------------------------------------------
   
    ProjectBuilder() {
        parserFactory = SAXParserFactory.newInstance();
        parserFactory.setValidating(false);  
    }

    Project buildFromFile(File file) throws AntException {
        try {
            SAXParser parser = parserFactory.newSAXParser();
            BuilderHandlerBase bhb = new BuilderHandlerBase();
            parser.parse(file, bhb);
            return bhb.getProject();
        } catch (ParserConfigurationException pce) {
            throw new AntException(pce.getMessage());
        } catch (SAXException se) {
            System.out.println(se);
            System.out.println(se.getMessage());
            throw new AntException(se.getMessage());
        } catch (IOException ioe) {
            throw new AntException(ioe.getMessage());
        }
    }
    
    class BuilderHandlerBase extends HandlerBase {
    
        private static final int STATE_START = 0;
        private static final int STATE_PROJECT = 1;
        private static final int STATE_TARGET = 2;
        private static final int STATE_TASK = 3;
        private static final int STATE_FINISHED = 99;
    
        private int state = STATE_START;
        
        private Target currentTarget;
        private Task currentTask;
    
        Project project = new Project();
    
        Project getProject() {
            return project;
        }
        
        public void startElement(String name, AttributeList atts) throws SAXException {
            //System.out.println("element: " + name);
            
            switch (state) {
              case STATE_START:
                if (name.equals("project")) {
                    state = STATE_PROJECT;
                    String projectName = atts.getValue("name");
                    if (projectName == null) {
                        System.out.println("Projects *must* have names");
                        // XXX exception out
                    }
                    project.setName(projectName);
                } else {
                    System.out.println("Expecting project, got: " + name);
                    // XXX exception out
                }
                break;
              case STATE_PROJECT:
                if (name.equals("target")) {
                    state = STATE_TARGET;
                    String targetName = atts.getValue("name");
                    if (targetName == null) {
                        System.out.println("Targets *must* have names");
                        // XXX exception out
                    }
                    currentTarget = new Target(targetName);
                    project.addTarget(currentTarget);
                    
                    // XXX add dependency checks
                } else {
                    System.out.println("Expecting target, got: " + name);
                    // XXX exception out
                }
                break;
              case STATE_TARGET:
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
              default:
                System.out.println("I'm not sure, but we're off base here: " + name);
                // XXX exception out
            }
        }
        
        public void characters(char ch[], int start, int length) throws SAXException {
        }
        
        public void endElement(String name) throws SAXException {
            // System.out.println("end: " + name);
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