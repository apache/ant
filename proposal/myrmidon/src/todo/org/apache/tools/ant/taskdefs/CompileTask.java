/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;
import java.util.Enumeration;
import java.util.Vector;
import org.apache.tools.ant.types.PatternSet;

/**
 * This task will compile and load a new taskdef all in one step. At times, this
 * is useful for eliminating ordering dependencies which otherwise would require
 * multiple executions of Ant.
 *
 * @author Sam Ruby <a href="mailto:rubys@us.ibm.com">rubys@us.ibm.com</a>
 * @deprecated use &lt;taskdef&gt; elements nested into &lt;target&gt;s instead
 */

public class CompileTask extends Javac
{

    protected Vector taskList = new Vector();

    /**
     * add a new task entry on the task list
     *
     * @return Description of the Returned Value
     */
    public Taskdef createTaskdef()
    {
        Taskdef task = new Taskdef();
        taskList.addElement( task );
        return task;
    }

    /**
     * have execute do nothing
     */
    public void execute() { }

    /**
     * do all the real work in init
     */
    public void init()
    {
        log( "!! CompileTask is deprecated. !!" );
        log( "Use <taskdef> elements nested into <target>s instead" );

        // create all the include entries from the task defs
        for( Enumeration e = taskList.elements(); e.hasMoreElements();  )
        {
            Taskdef task = ( Taskdef )e.nextElement();
            String source = task.getClassname().replace( '.', '/' ) + ".java";
            PatternSet.NameEntry include = super.createInclude();
            include.setName( "**/" + source );
        }

        // execute Javac
        super.init();
        super.execute();

        // now define all the new tasks
        for( Enumeration e = taskList.elements(); e.hasMoreElements();  )
        {
            Taskdef task = ( Taskdef )e.nextElement();
            task.init();
        }

    }
}
