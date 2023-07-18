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
 *     PS1 --> [A, B, C, D, TC]
 *     PS2 --> [A, B, C, TC]
 *     PS3 --> [B, C, TC]
 * }
 * These options correspond to ps3, ps2, and ps1, respectively, in the aforementioned paper
 */
public enum TransitiveClosureOptions {
    PS1,
    PS2,
    PS3,
}
