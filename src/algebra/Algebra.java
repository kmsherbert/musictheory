package algebra;

import music.Phrase;

/**
 * standard interface for the algebras this project defines
 * See report.pdf for details
 */
public interface Algebra {
    /**
     * sum(P1, P2, ... Pn) = (P1 + P2) + ... + Pn, whatever the "+" symbol means
     */
    Phrase sum(Phrase... Ps);

    /**
     * magnify(A, P) = AP, where A is a scalar real number
     *
     * In particular, magnify(-1, P) = -P, whatever the "-" symbol means
     */
    Phrase magnify(float A, Phrase P);

    /**
     * product(P1, P2) = P1 * P2, whatever the "*" symbol means
     */
    Phrase product(Phrase P1, Phrase P2);

    /**
     * inverse(P) = ~P, whatever the "~" symbol means
     */
    Phrase inverse(Phrase P);
}
