package com.example.nutricionalregister.feature.savedproducts

import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.feature.savedproducts.testing.FakeSavedProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SavedProductsViewModelTest {
    @Test
    fun savingInASingleActionUpdatesTheListWithNoDuplicate() =
        runTest {
            val repository = FakeSavedProductRepository()
            val viewModel =
                SavedProductsViewModel(
                    repository,
                    clockAt(100L),
                    CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
                )

            viewModel.save(product("0001", "Oats"))
            advanceUntilIdle()

            val saved = viewModel.state.value
            assertEquals(listOf("0001"), saved.saved.map { it.code })
            assertTrue(saved.savedCodes.contains("0001"))

            // re-saving the same product is an upsert, never a duplicate (FR-004)
            viewModel.save(product("0001", "Oats renamed"))
            advanceUntilIdle()

            val resaved = viewModel.state.value
            assertEquals(1, resaved.saved.size)
            assertEquals("Oats renamed", resaved.saved.single().name)
            viewModel.clear()
        }

    @Test
    fun removingTakesTheProductOutOfTheList() =
        runTest {
            val repository = FakeSavedProductRepository()
            var now = 100L
            val viewModel =
                SavedProductsViewModel(
                    repository,
                    EpochClock { now },
                    CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
                )

            viewModel.save(product("0001", "Oats"))
            advanceUntilIdle()
            now = 200L
            viewModel.save(product("0002", "Beans"))
            advanceUntilIdle()
            val ordered = viewModel.state.value
            assertEquals(listOf("0002", "0001"), ordered.saved.map { it.code })

            viewModel.remove("0001")
            advanceUntilIdle()

            val afterRemoval = viewModel.state.value
            assertEquals(listOf("0002"), afterRemoval.saved.map { it.code })
            assertFalse(afterRemoval.savedCodes.contains("0001"))
            viewModel.clear()
        }

    @Test
    fun emptyStateOnlyAppearsOnceLoadingCompletes() =
        runTest {
            val repository = FakeSavedProductRepository()
            val viewModel =
                SavedProductsViewModel(
                    repository,
                    clockAt(100L),
                    CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
                )

            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state.isEmpty)
            assertFalse(state.isLoading)
            viewModel.clear()
        }

    @Test
    fun openingAndClosingADetailTogglesOnlyTheSelectedProduct() =
        runTest {
            val repository = FakeSavedProductRepository()
            val viewModel =
                SavedProductsViewModel(
                    repository,
                    clockAt(100L),
                    CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
                )
            viewModel.save(product("0001", "Oats"))
            advanceUntilIdle()

            viewModel.openProduct("0001")
            val selected = viewModel.state.value.selectedProduct
            assertEquals("0001", selected?.code)

            viewModel.closeProductDetails()
            assertNull(viewModel.state.value.selectedProduct)
            viewModel.clear()
        }

    private fun clockAt(epochMillis: Long): EpochClock = EpochClock { epochMillis }

    private fun product(
        code: String,
        name: String,
    ): Product = Product(code = code, name = name)
}
