/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.sound;

import java.io.File;

/**
 * A class to be extended by any BuildAlert's that require the output of
 * sound.
 */
public class BuildAlert
{
    private File m_source;
    private int m_loops;
    private Long m_duration;

    /**
     * Sets the duration in milliseconds the file should be played.
     *
     * @param duration The new Duration value
     */
    public void setDuration( Long duration )
    {
        m_duration = duration;
    }

    /**
     * Sets the number of times the source file should be played.
     *
     * @param loops the number of loops to play the source file
     */
    public void setLoops( int loops )
    {
        m_loops = loops;
    }

    /**
     * Sets the location of the file to get the audio.
     *
     * @param source the name of a sound-file directory or of the audio file
     */
    public void setSource( final File source )
    {
        m_source = source;
    }

    /**
     * Gets the duration in milliseconds the file should be played.
     *
     * @return The Duration value
     */
    public Long getDuration()
    {
        return m_duration;
    }

    /**
     * Sets the number of times the source file should be played.
     *
     * @return the number of loops to play the source file
     */
    public int getLoops()
    {
        return m_loops;
    }

    /**
     * Gets the location of the file to get the audio.
     *
     * @return The Source value
     */
    public File getSource()
    {
        return m_source;
    }
}
