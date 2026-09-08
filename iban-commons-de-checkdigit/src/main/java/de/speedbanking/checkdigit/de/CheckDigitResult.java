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
package de.speedbanking.checkdigit.de;

import java.util.Objects;

/**
 * Outcome of a {@link GermanCheckDigitMethod} computation.
 * <p>
 * A positive result ({@link #isValid()} {@code == true}) does <em>not</em> mean the
 * account actually exists at the bank — only that the check digit embedded in the
 * account number is internally consistent with the method used.
 * <p>
 * Some methods (e.g. {@link GermanCheckDigitMethod#M09}, and certain account-number
 * ranges of {@link GermanCheckDigitMethod#M08}) perform no verification at all — for
 * these, {@link #isChecked()} is {@code false} and {@link #isValid()} is {@code true}
 * by convention, so that callers who only branch on {@link #isValid()} tolerate the
 * account number rather than rejecting it.
 *
 * @since 1.9.0
 */
public final class CheckDigitResult {

    private static final CheckDigitResult VALID   = new CheckDigitResult(true, true);
    private static final CheckDigitResult INVALID = new CheckDigitResult(false, true);

    /**
     * Sentinel result for methods that perform no check digit verification at all
     * (method {@code 09}, and the unverifiable ranges of method {@code 08}).
     */
    public static final CheckDigitResult NOT_CHECKED = new CheckDigitResult(true, false);

    private final boolean                 valid;
    private final boolean                 checked;

    private CheckDigitResult(boolean valid, boolean checked) {
        this.valid = valid;
        this.checked = checked;
    }

    /**
     * Returns the result for a method that was actually evaluated.
     *
     * @param valid whether the computed check digit matched the one embedded in the account number
     * @return {@link #VALID} or {@link #INVALID}, depending on {@code valid}
     */
    static CheckDigitResult of(boolean valid) {
        return valid ? VALID : INVALID;
    }

    /**
     * Whether the account number is consistent with the check digit method — either
     * because verification passed, or because the method performs no verification
     * (see {@link #isChecked()}).
     *
     * @return {@code true} if the account number should be treated as acceptable
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Whether a check digit was actually computed and compared.
     *
     * @return {@code false} if the method is a no-op for this account number
     *         (e.g. method {@code 09}); {@code true} otherwise
     */
    public boolean isChecked() {
        return checked;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof CheckDigitResult)) {
            return false;
        }
        CheckDigitResult other = (CheckDigitResult) obj;
        return valid == other.valid && checked == other.checked;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valid, checked);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[valid=" + valid + ", checked=" + checked + ']';
    }

}
