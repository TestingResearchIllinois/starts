package edu.illinois.starts.enums;

/**
 * Options for computing transitive closure.
 * Consider the example graph presented in "Techniques for Evolution-Aware Runtime Verification"
 * (https://www.cs.cornell.edu/~legunsen/pubs/LegunsenETAL19eMOP.pdf):
 * {@literal
 *     A  --> []
 *     B  --> [A]
 *     C  --> [B, D]
 *     D  --> []
 *     E  --> [D]
 *     TC --> [C]
 *     TE --> [E]
 * }
 * The options will return the following values when performed on B:
 * {@literal
 *     TRANSITIVE --> [B, C, TC]
 *     TRANSITIVE_AND_INVERSE_TRANSITIVE --> [A, B, C, TC]
 *     TRANSITIVE_OF_INVERSE_TRANSITIVE --> [A, B, C, D, TC]
 * }
 * These options correspond to ps3, ps2, and ps1, respectively, in the aforementioned paper
 */
public enum TransitiveClosureOptions {
    TRANSITIVE,
    TRANSITIVE_AND_INVERSE_TRANSITIVE,
    TRANSITIVE_OF_INVERSE_TRANSITIVE,
}
