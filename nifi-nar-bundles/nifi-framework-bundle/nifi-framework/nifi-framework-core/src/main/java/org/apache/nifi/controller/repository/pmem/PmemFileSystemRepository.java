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
package org.apache.nifi.controller.repository.pmem;

import org.apache.nifi.controller.repository.FileSystemRepository;
import org.apache.nifi.controller.repository.claim.StandardContentClaim;
import org.apache.nifi.pmem.PmemMappedFile;
import org.apache.nifi.pmem.PmemMappedFile.PmemOutputStream;
import org.apache.nifi.stream.io.ByteCountingOutputStream;
import org.apache.nifi.util.NiFiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import static org.apache.nifi.pmem.PmemMappedFile.PmemExtendStrategy.EXTEND_TO_FIT;
import static org.apache.nifi.pmem.PmemMappedFile.PmemPutStrategy.PUT_NO_DRAIN;

public class PmemFileSystemRepository extends FileSystemRepository {
    private static final Logger LOG = LoggerFactory.getLogger(FileSystemRepository.class);
    private static final Set<PosixFilePermission> DEFAULT_MODE = PosixFilePermissions.fromString("rw-r--r--"); // 0644

    public PmemFileSystemRepository() {
        super();
    }

    public PmemFileSystemRepository(final NiFiProperties nifiProperties) throws IOException {
        super(nifiProperties);
    }

    @Override
    protected OutputStream underlyingResourceClaimOutputStream(File file, long softLimit) throws IOException {
        final PmemMappedFile pmem = PmemMappedFile.create(file.toString(), softLimit, DEFAULT_MODE);
        /*
         * Here we use EXTEND_TO_FIT, not EXTEND_DOUBLY, because once the size of a resource claim file is over
         * softLimit (= maxAppendableClaimLength), the file will be closed soon. So EXTEND_TO_FIT is more suitable.
         *
         * Here we use PUT_NO_DRAIN, not PUT_NO_FLUSH, because whole content will be written in bulk, not in small
         * pieces. So PUT_NO_DRAIN will performs better, particularly when the size of the content is large.
         */
        final OutputStream out = pmem.uniqueOutputStream(0L, EXTEND_TO_FIT, PUT_NO_DRAIN);
        LOG.debug("PMEM created: {}", pmem.toString());
        return out;
    }

    @Override
    protected OutputStream newContentRepositoryOutputStream(StandardContentClaim scc, ByteCountingOutputStream bcos, int initialLength) {
        return new PmemContentRepositoryOutputStream(scc, bcos, initialLength);
    }

    protected class PmemContentRepositoryOutputStream extends ContentRepositoryOutputStream {
        public PmemContentRepositoryOutputStream(StandardContentClaim scc, ByteCountingOutputStream bcos, int initialLength) {
            super(scc, bcos, initialLength);
        }

        @Override
        protected synchronized void sync() throws IOException {
            // DO NOT call super.sync()

            final PmemOutputStream out = (PmemOutputStream) bcos.getWrappedStream();
            out.sync();
            LOG.debug("PMEM drained: {}", out.underlyingPmem().toString());
        }

        @Override
        public synchronized void close() throws IOException {
            super.close();
            LOG.debug("PMEM closed: {}", ((PmemOutputStream) bcos.getWrappedStream()).underlyingPmem().toString());
        }
    }
}
