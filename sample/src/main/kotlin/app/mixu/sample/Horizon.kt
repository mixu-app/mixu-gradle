package app.mixu.sample

/**
 * A deliberately tiny value type: the sample exists to prove the conventions
 * compile and test, not to model the product.
 */
public data class Horizon(
    public val ordinal: Int,
) : Comparable<Horizon> {
    init {
        require(ordinal >= 0) { "ordinal must be non-negative, was $ordinal" }
    }

    public fun reveals(firstReveal: Int): Boolean = firstReveal <= ordinal

    override fun compareTo(other: Horizon): Int = ordinal.compareTo(other.ordinal)
}
