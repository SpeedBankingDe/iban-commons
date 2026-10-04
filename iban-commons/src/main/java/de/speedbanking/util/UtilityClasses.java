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
package de.speedbanking.util;

/**
 * Supports the private, no-instance constructor that every utility class in this library
 * declares, so each one throws with the exact same message instead of repeating it.
 *
 * @since 1.8.12
 */
public final class UtilityClasses {

    /**
     * Private constructor to prevent instantiation of this utility class.
     * @throws UnsupportedOperationException always
     */
    private UtilityClasses() {
        throw cannotInstantiate(getClass());
    }

    /**
     * Builds the exception a utility class's private constructor throws when called, e.g. via reflection.
     *
     * @param clazz the utility class whose constructor was invoked
     * @return an {@link UnsupportedOperationException} describing the violation
     */
    public static UnsupportedOperationException cannotInstantiate(Class<?> clazz) {
        return new UnsupportedOperationException(
            String.format("Utility class %s cannot be instantiated", clazz.getSimpleName()));
    }

}
