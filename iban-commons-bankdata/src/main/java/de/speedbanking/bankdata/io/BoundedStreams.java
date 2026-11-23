/*
 * Copyright © 2025-2026 Markus Spann, SpeedBankingDe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.speedbanking.bankdata.io;

import de.speedbanking.util.UtilityClasses;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads streams into memory up to a fixed size limit.
 * <p>
 * Bank data is downloaded and unpacked in a background thread of the host application, so an
 * unexpectedly large response or a highly compressed XLSX entry must fail the refresh with an
 * {@link IOException} instead of exhausting the heap.
 *
 * @since 1.8.15
 */
final class BoundedStreams {

    /**
     * Upper limit for a downloaded file and for each unpacked XLSX part. The largest upstream
     * source is a few MB, so this leaves ample headroom.
     */
    static final int MAX_BYTES = 64 * 1024 * 1024;

    private BoundedStreams() {
        throw UtilityClasses.cannotInstantiate(getClass());
    }

    /**
     * Reads the stream to its end, failing once more than {@code maxBytes} bytes have been read.
     * The stream is not closed.
     *
     * @param in       the stream to read
     * @param maxBytes the maximum number of bytes accepted
     * @param what     a description of the content for the exception message
     * @return the bytes read
     * @throws IOException if reading fails or the stream holds more than {@code maxBytes} bytes
     */
    static byte[] readAll(InputStream in, int maxBytes, String what) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int bytesRead;
        while ((bytesRead = in.read(chunk)) != -1) {
            if (buffer.size() + bytesRead > maxBytes) {
                throw new IOException(String.format("%s exceeds the limit of %d bytes", what, maxBytes));
            }
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

}
