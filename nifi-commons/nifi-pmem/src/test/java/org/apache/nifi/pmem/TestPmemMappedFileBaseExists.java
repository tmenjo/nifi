/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.pmem;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.nifi.pmem.PmemMappedFileTestUtils.DEFAULT_LENGTH;
import static org.apache.nifi.pmem.PmemMappedFileTestUtils.NO_SPACE_LENGTH;
import static org.apache.nifi.pmem.PmemMappedFileTestUtils.PMEM_FS_DIR;
import static org.apache.nifi.pmem.PmemMappedFileTestUtils.READ_WRITE_MODE;
import static org.apache.nifi.pmem.PmemMappedFileTestUtils.allocateFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TestPmemMappedFileBaseExists {
    @Rule(order = 0)
    public final AssumePmemFsDirExists assumption = new AssumePmemFsDirExists();

    @Rule(order = 1)
    public final TemporaryFolder tempDir = TemporaryFolder.builder()
            .parentFolder(PMEM_FS_DIR.toFile()).assureDeletion().build();

    private Path path = null;

    @Before
    public void setUp() throws Exception {
        path = tempDir.newFile().toPath();

        allocateFile(path, DEFAULT_LENGTH);
        assertTrue(Files.exists(path));
        assertEquals(DEFAULT_LENGTH, Files.size(path));
    }

    @Test
    public void testOpen() throws IOException {
        try (final PmemMappedFile pmem = PmemMappedFile.open(path.toString())) {
            assertNotNull(pmem);
            assertEquals(path.toString(), pmem.path());
            assertEquals(DEFAULT_LENGTH, pmem.length());
            assertTrue(pmem.isPmem());
            assertTrue(pmem.isHugeAligned());
        }

        assertEquals(DEFAULT_LENGTH, Files.size(path));
    }

    @Test
    public void testOpenDenied() {
        /* Set file to read-only before open() */
        assertTrue(path.toFile().setReadOnly());

        assertThrows(AccessDeniedException.class,
                () -> PmemMappedFile.open(path.toString()));
    }

    @Test
    public void testOpenNullPath() {
        assertThrows(NullPointerException.class,
                () -> PmemMappedFile.open(null));
    }

    @Test
    public void testCreateExists() {
        assertThrows(FileAlreadyExistsException.class,
                () -> PmemMappedFile.create(
                        path.toString(), DEFAULT_LENGTH, READ_WRITE_MODE));
    }

    @Test
    public void testCreateIllegalLengthZero() {
        assertThrows(IllegalArgumentException.class,
                () -> PmemMappedFile.create(
                        path.toString(), 0L, READ_WRITE_MODE));
    }

    @Test
    public void testCreateIllegalLengthNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> PmemMappedFile.create(
                        path.toString(), -1L, READ_WRITE_MODE));
    }

    @Test
    public void testCreateNullPath() {
        assertThrows(NullPointerException.class,
                () -> PmemMappedFile.create(
                        null, DEFAULT_LENGTH, READ_WRITE_MODE));
    }

    @Test
    public void testCreateNullMode() {
        assertThrows(NullPointerException.class,
                () -> PmemMappedFile.create(
                        path.toString(), DEFAULT_LENGTH, null));
    }

    @Test
    public void testOpenOrCreate() throws IOException {
        try (final PmemMappedFile pmem = PmemMappedFile.openOrCreate(
                path.toString(), DEFAULT_LENGTH, READ_WRITE_MODE)) {
            assertNotNull(pmem);
            assertEquals(path.toString(), pmem.path());
            assertEquals(DEFAULT_LENGTH, pmem.length());
            assertTrue(pmem.isPmem());
            assertTrue(pmem.isHugeAligned());
        }

        assertEquals(DEFAULT_LENGTH, Files.size(path));
    }

    @Test
    public void testOpenOrCreateCreateNoSpace() {
        assertThrows(IOException.class,
                () -> PmemMappedFile.openOrCreate(
                        path.toString(), NO_SPACE_LENGTH, READ_WRITE_MODE));
    }

    @Test
    public void testOpenOrCreateIllegalLengthZero() {
        assertThrows(IllegalArgumentException.class,
                () -> PmemMappedFile.openOrCreate(
                        path.toString(), 0L, READ_WRITE_MODE));
    }

    @Test
    public void testOpenOrCreateIllegalLengthNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> PmemMappedFile.openOrCreate(
                        path.toString(), -1L, READ_WRITE_MODE));
    }

    @Test
    public void testOpenOrCreateNullPath() {
        assertThrows(NullPointerException.class,
                () -> PmemMappedFile.openOrCreate(
                        null, DEFAULT_LENGTH, READ_WRITE_MODE));
    }

    @Test
    public void testOpenOrCreateNullMode() {
        assertThrows(NullPointerException.class,
                () -> PmemMappedFile.openOrCreate(
                        path.toString(), DEFAULT_LENGTH, null));
    }
}