package org.apache.ant.echo;

import java.io.*;
import java.util.*;

import org.apache.ant.*;

/**
 * Basic echo task that just spits out whatever it is supposed to...
 *
 * @author James Duncan Davidson (duncan@apache.org)
 */
public class EchoTask extends AbstractTask {
    
    // -----------------------------------------------------------------
    // PRIVATE DATA MEMBERS
    // -----------------------------------------------------------------
    
    /**
     * Data to echo
     */
    private String data;
    
    // -----------------------------------------------------------------
    // PUBLIC METHODS
    // -----------------------------------------------------------------    
    
    /**
     *
     */
    public boolean execute() throws AntException {
    
        PrintStream out = project.getOutput();
        out.println("ECHOING: " + data);
        return true;
    } 
    
    /**
     *
     */
    public void setData(String data) {
        this.data = data;
    }
}