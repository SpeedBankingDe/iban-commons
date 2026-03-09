package de.speedbanking.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class containing common methods used in testing environments,
 * such as generating invalid data or selecting random elements from a collection.
 * <p>
 * This class cannot be instantiated.
 */
public final class TestUtil {

    /**
     * Private constructor to prevent instantiation of this utility class.
     * @throws UnsupportedOperationException always
     */
    private TestUtil() {
        throw new UnsupportedOperationException(
            "Utility class " + getClass().getSimpleName() + " cannot be instantiated");
    }

    /**
     * Swaps two random, distinct characters in the input string to intentionally
     * corrupt a string such as an IBAN, making it invalid (e.g., failing a checksum validation).
     *
     * @param input the string to corrupt, must have a length of at least 2
     * @return the corrupted string, or the original string if length less than 2 or input is null
     */
    public static String swapRandomChars(String input) {
        if (input == null || input.length() < 2) {
            return input;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int len = input.length();
        char[] chars = input.toCharArray();

        int index1 = random.nextInt(len);
        // Generates an index in the range [0, len - 2].
        // This index represents the position in the "shortened" range of indices
        // that are not index1.
        int index2 = random.nextInt(len - 1);

        // If the generated index2 is greater than or equal to index1,
        // we increment it to skip index1 and map it back to the full range [0, len-1].
        // This ensures index2 != index1 and maintains uniform probability for all distinct pairs.
        if (index2 >= index1) {
            index2++;
        }

        // swap chars
        char temp = chars[index1];
        chars[index1] = chars[index2];
        chars[index2] = temp;

        return new String(chars);
    }

    /**
     * Retrieves a random element from the provided list.
     *
     * @param <T>  the type of elements in the list
     * @param list the list from which a random element should be selected, must not be {@code null} or empty
     * @return a randomly selected element of type {@code T} from the list or {@code null} if the list is {@code null} or empty
     */
    public static <T> T getRandomListEntry(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        int size = list.size();
        if (size == 1) {
            return list.get(0);
        }

        return list.get(ThreadLocalRandom.current().nextInt(size));
    }

    /**
     * Serializes the given object to a byte array using Java object serialization.
     *
     * @param <T>    the type of the object to serialize
     * @param object the object to serialize; must not be {@code null}
     * @return the serialized byte representation
     * @throws IOException if an I/O error occurs
     */
    public static <T extends Serializable> byte[] serialize(final T object) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            return baos.toByteArray();
        }
    }

    /**
     * Deserializes an object of type {@code T} from the given byte array.
     *
     * @param <T>   the expected type of the deserialized object
     * @param bytes the byte array produced by {@link #serialize}; must not be {@code null}
     * @return the deserialized object
     * @throws IOException            if an I/O error occurs
     * @throws ClassNotFoundException if the class of the serialized object cannot be found
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deserialize(final byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (T) ois.readObject();
        }
    }

    /**
     * Replaces the first occurrence of {@code oldName} with {@code newName} in a byte array,
     * adjusting the two-byte big-endian length prefix that precedes the class name in the
     * Java serialization stream format (see the Java Object Serialization Specification,
     * section 6.2 — TC_CLASSDESC).
     * <p>
     * Used in tests to craft a manipulated stream where a proxy class descriptor
     * (e.g., {@code Foo$Memento}) is replaced by the target class ({@code Foo}),
     * simulating a byte-stream injection attack.
     *
     * @param stream  the original serialized byte array; returned unchanged if {@code oldName} is not found
     * @param oldName the UTF-8 class name to search for (e.g., {@code "de.speedbanking.iban.Iban$Memento"})
     * @param newName the UTF-8 class name to substitute (e.g., {@code "de.speedbanking.iban.Iban"})
     * @return a new byte array with the class name replaced, or the original array if no match was found
     */
    public static byte[] replaceClassName(final byte[] stream, final String oldName, final String newName) {
        byte[] oldBytes = oldName.getBytes(StandardCharsets.UTF_8);
        byte[] newBytes = newName.getBytes(StandardCharsets.UTF_8);

        int pos = indexOf(stream, oldBytes);
        if (pos < 2) {
            return stream; // not found or no room for length prefix — return unchanged
        }

        // The two bytes before the class name encode its UTF-8 length (big-endian short)
        int prefixPos = pos - 2;
        byte[] result = new byte[stream.length - oldBytes.length + newBytes.length];
        System.arraycopy(stream, 0, result, 0, prefixPos);
        result[prefixPos]     = (byte) ((newBytes.length >>> 8) & 0xFF);
        result[prefixPos + 1] = (byte)  (newBytes.length        & 0xFF);
        System.arraycopy(newBytes, 0, result, prefixPos + 2, newBytes.length);
        System.arraycopy(stream, pos + oldBytes.length, result, prefixPos + 2 + newBytes.length,
                         stream.length - pos - oldBytes.length);
        return result;
    }

    /**
     * Builds a minimal serialized stream containing a Memento proxy with
     * {@code STREAM_VERSION = 1L} and the given (potentially invalid) value string.
     * <p>
     * Convenience overload of {@link #buildMementoStream(Serializable, long, String)}.
     *
     * @param <T>      the type of the template object (must use the Memento pattern)
     * @param template a valid instance used to obtain the class descriptor; must not be {@code null}
     * @param value    the raw string to embed as the Memento payload
     * @return a crafted serialization byte stream
     * @throws IOException if writing fails
     */
    public static <T extends Serializable> byte[] buildMementoStream(T template, String value) throws IOException {
        return buildMementoStream(template, 1L, value);
    }

    /**
     * Builds a minimal serialized stream containing a Memento proxy with the given stream
     * version and value string.
     * <p>
     * The stream is assembled by serializing {@code template} to obtain a well-formed class
     * descriptor and stream trailer, then splicing in a replacement payload that carries the
     * supplied {@code streamVersion} long and {@code value} string.
     * <p>
     * The custom {@code writeObject()} payload is framed by the JVM in a {@code TC_BLOCKDATA}
     * block ({@code 0x77 | length_byte | payload}). The method locates this block by searching
     * for {@code TC_BLOCKDATA} immediately followed by {@code STREAM_VERSION=1L} as an 8-byte
     * big-endian long — rather than searching for the version bytes alone, which would
     * accidentally match the class descriptor's {@code serialVersionUID} field (also {@code 1L}).
     * <p>
     * Both the payload content and the {@code TC_BLOCKDATA} length byte are patched so the JVM
     * deserializer reads the correct number of bytes and reaches either {@code readObject()} or
     * {@code readResolve()} without an {@code EOFException}.
     * <p>
     * Used to drive:
     * <ul>
     *   <li>The version-mismatch branch in {@code Memento.readObject()} — pass a version != 1L</li>
     *   <li>The validation-failure branch in {@code Memento.readResolve()} — pass an invalid value</li>
     * </ul>
     *
     * @param <T>           the type of the template object (must use the Memento pattern)
     * @param template      a valid instance used to obtain the class descriptor; must not be {@code null}
     * @param streamVersion the version long to embed ({@code 1L} = currently supported version)
     * @param value         the raw string to embed as the Memento payload
     * @return a crafted serialization byte stream
     * @throws IOException if writing fails
     */
    public static <T extends Serializable> byte[] buildMementoStream(
            final T template, final long streamVersion, final String value) throws IOException {
        // 1. Serialize the template to get a well-formed stream with the correct class descriptor.
        byte[] templateBytes = serialize(template);

        // 2. Build the replacement payload: version (8 bytes big-endian long) +
        //    modified UTF-8 string (2-byte length prefix + content),
        //    matching the layout written by Memento.writeObject().
        ByteArrayOutputStream payloadBuf = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(payloadBuf);
        dos.writeLong(streamVersion);
        dos.writeUTF(value);
        dos.flush();
        byte[] newPayload = payloadBuf.toByteArray();

        // 3. Locate the TC_BLOCKDATA block that wraps the Memento.writeObject() payload.
        //
        //    The serialization stream contains several occurrences of 0x00..0x01 (the value 1L):
        //    most notably the class descriptor's serialVersionUID field (also 1L). A naive
        //    search for the version-long bytes would hit the serialVersionUID first and point
        //    into the class descriptor rather than the data payload.
        //
        //    A custom writeObject() is always framed by the JVM in a TC_BLOCKDATA block:
        //      TC_BLOCKDATA (0x77) | block_length (1 byte) | payload bytes ...
        //    We therefore search for the 10-byte anchor:
        //      0x77 | any_byte | 0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x01
        //    which uniquely identifies TC_BLOCKDATA immediately followed by STREAM_VERSION=1L,
        //    and is guaranteed to appear only once in the stream.
        int blockHeaderPos = findBlockDataWithVersionMarker(templateBytes);
        if (blockHeaderPos < 0) {
            throw new IllegalArgumentException(
                "Cannot locate TC_BLOCKDATA block with STREAM_VERSION=1L in the serialized stream");
        }
        // payloadStart: the byte immediately after TC_BLOCKDATA + block_length_byte
        int payloadStart = blockHeaderPos + 2;

        // 4. Determine the length of the original payload so the stream trailer is preserved.
        //    Layout written by Memento.writeObject():
        //      [8 bytes]  version long
        //      [2 bytes]  UTF-8 string length prefix (big-endian short)
        //      [n bytes]  UTF-8 string content
        int originalUtfLen = ((templateBytes[payloadStart + 8] & 0xFF) << 8)
                           |  (templateBytes[payloadStart + 9] & 0xFF);
        int originalPayloadLen = 8 + 2 + originalUtfLen;
        int trailerStart      = payloadStart + originalPayloadLen;

        // 5. Patch the TC_BLOCKDATA length byte to reflect the new payload size.
        //    ObjectInputStream uses this byte to know how many bytes belong to the block;
        //    if it is stale the stream is read incorrectly (EOFException or silent corruption).
        //    TC_BLOCKDATA supports block lengths up to 255; Memento payloads are well below that.
        int newBlockLen = newPayload.length;
        if (newBlockLen > 255) {
            throw new IllegalArgumentException(
                "New Memento payload exceeds TC_BLOCKDATA capacity (255 bytes): " + newBlockLen);
        }

        // 6. Splice: header (with patched block-length byte) | new payload | original trailer
        byte[] result = new byte[payloadStart + newPayload.length + templateBytes.length - trailerStart];
        System.arraycopy(templateBytes, 0,            result, 0,           payloadStart);
        result[blockHeaderPos + 1] = (byte) newBlockLen; // patch block length
        System.arraycopy(newPayload,    0,            result, payloadStart, newPayload.length);
        System.arraycopy(templateBytes, trailerStart, result, payloadStart + newPayload.length,
                         templateBytes.length - trailerStart);
        return result;
    }

    /**
     * Finds the position of the {@code TC_BLOCKDATA} (0x77) byte that immediately precedes
     * a Memento {@code writeObject()} payload starting with {@code STREAM_VERSION = 1L}.
     * <p>
     * Searches for the 10-byte pattern: {@code 0x77 | any | 0x00 0x00 0x00 0x00 0x00 0x00 0x00 0x01}
     *
     * @param stream the serialized byte array to search
     * @return the index of the {@code TC_BLOCKDATA} byte, or {@code -1} if not found
     */
    private static int findBlockDataWithVersionMarker(final byte[] stream) {
        // needle: TC_BLOCKDATA (0x77), skip 1 byte (block length), then STREAM_VERSION=1L
        byte[] versionLong = {0, 0, 0, 0, 0, 0, 0, 1};
        int result = -1;
        for (int i = 0; result == -1 && i <= stream.length - 10; i++) {
            if ((stream[i] & 0xFF) != 0x77) {
                continue;
            }
            // stream[i+1] is the block length byte — accept any value
            boolean match = true;
            for (int j = 0; match && j < versionLong.length; j++) {
                match = stream[i + 2 + j] == versionLong[j];
            }
            if (match) {
                result = i;
            }
        }
        return result;
    }

    /**
     * Returns the index of the first occurrence of {@code needle} in {@code haystack},
     * or {@code -1} if not found.
     * <p>
     * Uses a straightforward two-pointer scan. The inner loop sets a {@code match} flag
     * rather than branching out of the loop directly, avoiding the PMD
     * {@code AvoidBranchingStatementAsLastInLoop} warning.
     *
     * @param haystack the byte array to search in; must not be {@code null}
     * @param needle   the byte sequence to search for; must not be {@code null}
     * @return the zero-based start index of the first match, or {@code -1} if none
     */
    public static int indexOf(final byte[] haystack, final byte[] needle) {
        int result = -1;
        for (int i = 0; result == -1 && i <= haystack.length - needle.length; i++) {
            boolean match = true;
            for (int j = 0; match && j < needle.length; j++) {
                match = haystack[i + j] == needle[j];
            }
            if (match) {
                result = i;
            }
        }
        return result;
    }

}
