package de.urbanpulse.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ConcatIteratorTest {

    private Iterator<String> emptyIterator;
    private Iterator<String> abcIterator;
    private Iterator<String> dIterator;
    private Iterator<String> efIterator;

    @Before
    public void setUp() {
        emptyIterator = new LinkedList<String>().iterator();

        LinkedList<String> abc = new LinkedList<>();
        abc.add("a");
        abc.add("b");
        abc.add("c");
        abcIterator = abc.iterator();

        LinkedList<String> d = new LinkedList<>();
        d.add("d");
        dIterator = d.iterator();

        LinkedList<String> ef = new LinkedList<>();
        ef.add("e");
        ef.add("f");
        efIterator = ef.iterator();
    }

    @Test
    public void testNoInnerIterators() {
        Iterator<String> fromNone = new ConcatIterator<>();
        assertFalse(fromNone.hasNext());
        try {
            fromNone.next();
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException e) {

        }
    }

    @Test
    public void testEmptyInnerIterator() {
        Iterator<String> fromNone = new ConcatIterator<>(emptyIterator);
        assertFalse(fromNone.hasNext());
        try {
            fromNone.next();
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException e) {

        }
    }

    @Test
    public void testAbcEmptyDEf() {
        Iterator<String> abcdef = new ConcatIterator<>(abcIterator, emptyIterator, dIterator, efIterator);
        assertTrue(abcdef.hasNext());
        assertEquals("a", abcdef.next());

        assertTrue(abcdef.hasNext());
        assertEquals("b", abcdef.next());

        assertTrue(abcdef.hasNext());
        assertEquals("c", abcdef.next());

        assertTrue(abcdef.hasNext());
        assertEquals("d", abcdef.next());

        assertTrue(abcdef.hasNext());
        assertEquals("e", abcdef.next());

        assertTrue(abcdef.hasNext());
        assertEquals("f", abcdef.next());

        assertFalse(abcdef.hasNext());
        try {
            abcdef.next();
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException e) {

        }
    }

}
