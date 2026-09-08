package com.hangingspider.game.data.model

/**
 * Catalog of gift-card rewards the user can cash out coins for.
 *
 * Rates are 1000 coins = ₹1 at the entry tier, with a small bulk discount
 * at higher denominations to incentivize larger redemptions (fewer admin
 * fulfillment operations per rupee paid out).
 */
enum class RewardProvider(val display: String, val glyph: String, val accentHex: Long) {
    AMAZON("Amazon", "A", 0xFFFF9900)
}

data class RewardTier(
    val id: String,
    val provider: RewardProvider,
    val amountInr: Int,
    val coins: Long
) {
    val displayLabel: String get() = "₹$amountInr ${provider.display}"
}

object RewardCatalog {
    val tiers: List<RewardTier> = listOf(
        RewardTier("amz_100",  RewardProvider.AMAZON, 100,    100_000),
        RewardTier("amz_250",  RewardProvider.AMAZON, 250,    240_000),
        RewardTier("amz_500",  RewardProvider.AMAZON, 500,    470_000),
        RewardTier("amz_1000", RewardProvider.AMAZON, 1000,   900_000)
    )

    fun byId(id: String): RewardTier? = tiers.firstOrNull { it.id == id }
    fun byProvider(p: RewardProvider): List<RewardTier> = tiers.filter { it.provider == p }
}
