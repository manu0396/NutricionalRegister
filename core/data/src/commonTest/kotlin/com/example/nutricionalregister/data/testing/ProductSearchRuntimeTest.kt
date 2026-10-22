package com.example.nutricionalregister.data.testing

import com.example.nutricionalregister.data.ProductSearchRuntime
import com.example.nutricionalregister.domain.assistant.AssistantClient
import com.example.nutricionalregister.domain.assistant.AssistantContext
import com.example.nutricionalregister.domain.assistant.AssistantMessage
import com.example.nutricionalregister.domain.assistant.AssistantReply
import com.example.nutricionalregister.domain.diary.DayBoundaryProvider
import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.DiaryRepository
import com.example.nutricionalregister.domain.diary.NutritionGoal
import com.example.nutricionalregister.domain.diary.NutritionGoalRepository
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.product.ProductSearchOutcome
import com.example.nutricionalregister.domain.product.ProductSearchRepository
import com.example.nutricionalregister.domain.product.RecentSearch
import com.example.nutricionalregister.domain.product.RecentSearchRepository
import com.example.nutricionalregister.domain.product.SearchTerm
import com.example.nutricionalregister.domain.purchase.PurchaseOutcome
import com.example.nutricionalregister.domain.purchase.PurchaseRecord
import com.example.nutricionalregister.domain.purchase.PurchaseRepository
import com.example.nutricionalregister.domain.purchase.SavedProduct
import com.example.nutricionalregister.domain.purchase.SavedProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ProductSearchRuntimeTest {
    @Test
    fun closeDelegatesToCloseResourcesAndKeepsRepositoriesExposed() {
        // GIVEN a runtime wired to stub repositories and a close callback
        val productRepository = StubProductSearchRepository
        val recentRepository = StubRecentSearchRepository
        var closeCalls = 0
        val runtime =
            ProductSearchRuntime(
                productSearchRepository = productRepository,
                recentSearchRepository = recentRepository,
                savedProductRepository = StubSavedProductRepository,
                purchaseRepository = StubPurchaseRepository,
                diaryRepository = StubDiaryRepository,
                nutritionGoalRepository = StubNutritionGoalRepository,
                dayBoundaryProvider = StubDayBoundaryProvider,
                assistantClient = StubAssistantClient,
                isAssistantConfigured = true,
                closeResources = { closeCalls += 1 },
            )

        // THEN both repositories stay exposed and nothing has been closed yet
        assertSame(productRepository, runtime.productSearchRepository)
        assertSame(recentRepository, runtime.recentSearchRepository)
        assertSame(StubSavedProductRepository, runtime.savedProductRepository)
        assertSame(StubPurchaseRepository, runtime.purchaseRepository)
        assertSame(StubDiaryRepository, runtime.diaryRepository)
        assertSame(StubNutritionGoalRepository, runtime.nutritionGoalRepository)
        assertSame(StubDayBoundaryProvider, runtime.dayBoundaryProvider)
        assertSame(StubAssistantClient, runtime.assistantClient)
        assertEquals(true, runtime.isAssistantConfigured)
        assertEquals(0, closeCalls)

        // WHEN the runtime is closed twice
        runtime.close()
        runtime.close()

        // THEN the close callback runs once per close
        assertEquals(2, closeCalls)
    }
}

private object StubProductSearchRepository : ProductSearchRepository {
    override fun search(term: SearchTerm): Flow<ProductSearchOutcome> = flowOf()

    override suspend fun getProduct(code: String): Product? = null
}

private object StubRecentSearchRepository : RecentSearchRepository {
    override fun observeRecentSearches(): Flow<List<RecentSearch>> = flowOf(emptyList())

    override suspend fun recordCompletedSearch(
        term: SearchTerm,
        completedAtEpochMillis: Long,
    ) = Unit

    override suspend fun deleteRecentSearch(normalizedTerm: String) = Unit
}

private object StubSavedProductRepository : SavedProductRepository {
    override suspend fun save(product: SavedProduct) = Unit

    override suspend fun remove(code: String) = Unit

    override fun observeSaved(): Flow<List<SavedProduct>> = flowOf(emptyList())

    override fun isSaved(code: String): Flow<Boolean> = flowOf(false)
}

private object StubPurchaseRepository : PurchaseRepository {
    override fun purchase(code: String): Flow<PurchaseOutcome> = flowOf()

    override fun observePurchase(code: String): Flow<PurchaseRecord?> = flowOf(null)

    override fun observePurchasedCodes(): Flow<Set<String>> = flowOf(emptySet())

    override suspend fun reconcile() = Unit
}

private object StubDiaryRepository : DiaryRepository {
    override fun observeEntriesForDay(dayEpochMillis: Long): Flow<List<DiaryEntry>> = flowOf(emptyList())

    override fun observeEntriesForWeek(dayEpochMillis: Long): Flow<List<DiaryEntry>> = flowOf(emptyList())

    override suspend fun addEntry(entry: DiaryEntry): Long = 0L

    override suspend fun updateEntry(entry: DiaryEntry) = Unit

    override suspend fun removeEntry(id: Long) = Unit
}

private object StubNutritionGoalRepository : NutritionGoalRepository {
    override fun observeGoal(): Flow<NutritionGoal> = flowOf(NutritionGoal())

    override suspend fun saveGoal(goal: NutritionGoal) = Unit
}

private object StubDayBoundaryProvider : DayBoundaryProvider {
    override fun startOfDay(epochMillis: Long): Long = epochMillis

    override fun addDays(
        dayStartEpochMillis: Long,
        days: Int,
    ): Long = dayStartEpochMillis

    override fun startOfWeek(epochMillis: Long): Long = epochMillis
}

private object StubAssistantClient : AssistantClient {
    override suspend fun complete(
        messages: List<AssistantMessage>,
        context: AssistantContext,
    ): AssistantReply = error("The stub assistant never answers")
}
