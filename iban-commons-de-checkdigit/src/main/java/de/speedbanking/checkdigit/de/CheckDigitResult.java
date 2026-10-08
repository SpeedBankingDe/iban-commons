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
 * <p>
 * A method returns {@link #NOT_CHECKED} only where the Bundesbank specification defines no
 * check digit for the account number ("nicht pruefbar, da diese Nummern keine Pruefziffer
 * enthalten"). An account number that fails the calculation is always invalid, also where
 * the specification calls it "nicht pruefbar": method 91 lists account numbers that fail
 * every variant as "Testkontonummern (falsch)".
 *
 * @since 1.9.0
 */
public final class CheckDigitResult {

    private static final CheckDigitResult VALID   = new CheckDigitResult(true, true);
    private static final CheckDigitResult INVALID = new CheckDigitResult(false, true);

    /**
     * Sentinel result for account numbers without a check digit, for example method {@code 09}
     * and the ranges of other methods where the specification defines no check digit. Not
     * used for account numbers that fail the calculation.
     */
    public static final CheckDigitResult NOT_CHECKED = new CheckDigitResult(true, false);

    private final boolean                 validFlag;
    private final boolean                 checked;

    private CheckDigitResult(boolean valid, boolean checked) {
        this.validFlag = valid;
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
        return validFlag;
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
        return validFlag == other.validFlag && checked == other.checked;
    }

    @Override
    public int hashCode() {
        return Objects.hash(validFlag, checked);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[valid=" + validFlag + ", checked=" + checked + ']';
    }

}
