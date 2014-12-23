package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.UnsupportedEncodingException;
import java.util.Comparator;
import java.util.Random;
import java.util.zip.CRC32;

import junit.framework.JUnit4TestAdapter;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Sorter;

public class RandomComparator implements Comparator<Description> {
    @Override
    public int compare(Description o1, Description o2) {
        return checkSum(o1.getDisplayName()) - checkSum(o2.getDisplayName());
    }

    private final long seed;
    public RandomComparator(long seed){
        this.seed =  seed;
    }

    private int checkSum(String value) {
        CRC32 crc32 = new CRC32();
        try {
            crc32.update(value.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            //This should never happen
        }
        Random random = new Random(crc32.getValue() ^ seed);

        return random.nextInt();
    }

    public static void apply(JUnit4TestAdapter adapter, long seed){
        adapter.sort(new Sorter(new RandomComparator(seed)));
    }
}

