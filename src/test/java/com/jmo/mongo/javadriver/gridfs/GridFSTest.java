package com.jmo.mongo.javadriver.gridfs;

import static org.junit.Assert.*;

import com.google.common.io.CountingInputStream;
import com.jmo.mongo.javadriver.gridfs.GridFS.StatusEntry;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class GridFSTest {

  @Test
  public void getUUIDBuckets() throws Exception {
    final List<String> uuidBuckets = GridFS.getUUIDBuckets(4);
    uuidBuckets.forEach(System.out::println);

    assertEquals(4, uuidBuckets.size());
    assertEquals("00000000-0000-0000-0000-000000000000", uuidBuckets.get(0));
    assertEquals("40000000-0000-0000-0000-000000000000", uuidBuckets.get(1));
    assertEquals("80000000-0000-0000-0000-000000000000", uuidBuckets.get(2));
    assertEquals("c0000000-0000-0000-0000-000000000000", uuidBuckets.get(3));
  }
  
  @Test
  public void format() throws Exception {
    assertEquals("0", String.format("%,.0f", 0.0));
    assertEquals("2", String.format("%,.0f", 2.0));
    assertEquals("2,002", String.format("%,.0f", 2002.1));
    assertEquals("12,342,002", String.format("%,.0f", 12342002.1231));
  }

  @Test
  public void statusCompare() throws Exception {
    final StatusEntry first = new StatusEntry("aa", "filename", 2L,
        new CountingInputStream(new ByteArrayInputStream(new byte[]{1})));
    Thread.sleep(1000);
    final StatusEntry second = new StatusEntry("bb", "filename", 3L,
        new CountingInputStream(new ByteArrayInputStream(new byte[]{1})));

    final List<StatusEntry> list = new ArrayList<>(Arrays.asList(second, first));
    Collections.sort(list);

    assertEquals(first, list.get(0));
    assertEquals(second, list.get(1));
  }
}
