package de.speedbanking.iban;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class containing common methods used in testing environments,
 * such as generating invalid data or selecting random elements from a collection.
 * <p>
 * This class cannot be instantiated.
 */
public final class TestUtil {

    private TestUtil() {
        throw new UnsupportedOperationException(
            "Utility class " + TestUtil.class.getSimpleName() + " cannot be instantiated");
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

}
