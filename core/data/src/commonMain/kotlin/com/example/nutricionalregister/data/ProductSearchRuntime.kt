package com.example.nutricionalregister.data

import com.example.nutricionalregister.domain.assistant.AssistantClient
import com.example.nutricionalregister.domain.diary.DayBoundaryProvider
import com.example.nutricionalregister.domain.diary.DiaryRepository
import com.example.nutricionalregister.domain.diary.NutritionGoalRepository
import com.example.nutricionalregister.domain.product.ProductSearchRepository
import com.example.nutricionalregister.domain.product.RecentSearchRepository
import com.example.nutricionalregister.domain.purchase.PurchaseRepository
import com.example.nutricionalregister.domain.purchase.SavedProductRepository

/**
 * The shared runtime: every repository the app needs, over one database and one
 * HTTP client.
 *
 * The runtime is created by the platform composition root so that the shared
 * code never depends on a platform API directly.
 *
 * @property assistantClient the client of the AI gateway the Coach talks to.
 * @property isAssistantConfigured whether the gateway address is configured for
 *   this build. A build without an address has no assistant, and the Coach
 *   screen says so plainly instead of offering an input field that cannot work
 *   (ADR-0016, FR-025).
 */
public class ProductSearchRuntime internal constructor(
    public val productSearchRepository: ProductSearchRepository,
    public val recentSearchRepository: RecentSearchRepository,
    public val savedProductRepository: SavedProductRepository,
    public val purchaseRepository: PurchaseRepository,
    public val diaryRepository: DiaryRepository,
    public val nutritionGoalRepository: NutritionGoalRepository,
    public val dayBoundaryProvider: DayBoundaryProvider,
    public val assistantClient: AssistantClient,
    public val isAssistantConfigured: Boolean,
    private val closeResources: () -> Unit,
) : AutoCloseable {
    override fun close() {
        closeResources()
    }
}
