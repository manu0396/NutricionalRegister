package com.example.nutricionalregister.feature.assistant

import com.example.nutricionalregister.domain.assistant.AssistantAction
import com.example.nutricionalregister.domain.assistant.AssistantActionExecutor
import com.example.nutricionalregister.domain.assistant.AssistantReply
import com.example.nutricionalregister.domain.assistant.AssistantRole
import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.diary.NutritionGoal
import com.example.nutricionalregister.domain.product.ConnectivityStatus
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.product.NutritionBasis
import com.example.nutricionalregister.domain.product.NutritionValue
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.domain.purchase.SavedProduct
import com.example.nutricionalregister.feature.assistant.testing.FakeAssistantClient
import com.example.nutricionalregister.feature.assistant.testing.FakeDayBoundaryProvider
import com.example.nutricionalregister.feature.assistant.testing.FakeDiaryRepository
import com.example.nutricionalregister.feature.assistant.testing.FakeNutritionGoalRepository
import com.example.nutricionalregister.feature.assistant.testing.FakeProductSearchRepository
import com.example.nutricionalregister.feature.assistant.testing.FakeSavedProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CoachViewModelTest {
    @Test
    fun sendingAMessageShowsTheUserTurnAndTheAnswer() =
        runTest {
            val client = FakeAssistantClient(reply = AssistantReply(text = "You ate 30 g of oats", actions = emptyList()))
            val viewModel = viewModel(client = client, scope = scope(testScheduler), hasConsent = true)

            viewModel.send("I ate oats")

            val sending = viewModel.state.value
            assertEquals(1, sending.messages.size)
            assertEquals(AssistantRole.USER, sending.messages.single().role)
            assertTrue(sending.isSending)

            advanceUntilIdle()

            val state = viewModel.state.value
            val messages = state.messages
            assertEquals(2, messages.size)
            assertEquals("I ate oats", messages.first().text)
            assertEquals(AssistantRole.ASSISTANT, messages.last().role)
            assertEquals("You ate 30 g of oats", messages.last().text)
            assertFalse(state.isSending)
        }

    @Test
    fun aBlankMessageIsIgnored() =
        runTest {
            val client = FakeAssistantClient()
            val viewModel = viewModel(client = client, scope = scope(testScheduler), hasConsent = true)

            viewModel.send("   ")
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(0, client.requestCount)
            assertTrue(state.messages.isEmpty())
            assertFalse(state.isSending)
        }

    @Test
    fun theAssistantIsSwitchedOffWhenNoAiServiceIsConfigured() =
        runTest {
            val client = FakeAssistantClient()
            val viewModel = viewModel(client = client, scope = scope(testScheduler), isConfigured = false)

            assertEquals(AssistantAvailability.NotConfigured, viewModel.state.value.availability)

            viewModel.send("I ate oats")
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(AssistantAvailability.NotConfigured, state.availability)
            assertEquals(0, client.requestCount)
            assertTrue(state.messages.isEmpty())
        }

    @Test
    fun theAssistantIsOfflineWhenTheDeviceHasNoConnection() =
        runTest {
            val client = FakeAssistantClient()
            val viewModel =
                viewModel(
                    client = client,
                    scope = scope(testScheduler),
                    hasConsent = true,
                    connectivity = ConnectivityStatus.OFFLINE,
                )

            advanceUntilIdle()

            assertEquals(AssistantAvailability.Offline, viewModel.state.value.availability)

            viewModel.send("I ate oats")
            advanceUntilIdle()

            assertEquals(AssistantAvailability.Offline, viewModel.state.value.availability)
            assertEquals(0, client.requestCount)
            assertFalse(viewModel.state.value.isSending)
        }

    @Test
    fun theAssistantAsksForConsentBeforeItReachesTheService() =
        runTest {
            val client = FakeAssistantClient()
            val viewModel = viewModel(client = client, scope = scope(testScheduler), hasConsent = false)

            assertEquals(AssistantAvailability.ConsentRequired, viewModel.state.value.availability)

            viewModel.send("I ate oats")
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(AssistantAvailability.ConsentRequired, state.availability)
            assertEquals(0, client.requestCount)
            assertTrue(state.messages.isEmpty())
        }

    @Test
    fun grantingConsentLetsTheConversationStart() =
        runTest {
            val client = FakeAssistantClient()
            val viewModel = viewModel(client = client, scope = scope(testScheduler), hasConsent = false)

            viewModel.grantConsent()
            advanceUntilIdle()

            assertEquals(AssistantAvailability.Ready, viewModel.state.value.availability)

            viewModel.send("I ate oats")
            advanceUntilIdle()

            assertEquals(1, client.requestCount)
        }

    @Test
    fun nothingIsWrittenToTheDiaryBeforeTheProposalIsConfirmed() =
        runTest {
            val diaryRepository = FakeDiaryRepository()
            val client = FakeAssistantClient(reply = replyWithLogFood())
            val viewModel =
                viewModel(
                    client = client,
                    scope = scope(testScheduler),
                    diaryRepository = diaryRepository,
                    hasConsent = true,
                )

            viewModel.send("I ate oats")
            advanceUntilIdle()

            assertNotNull(viewModel.state.value.pendingProposal)
            assertEquals(0, diaryRepository.writeCount)
        }

    @Test
    fun confirmingTheProposalWritesTheEntryToTheDiary() =
        runTest {
            val diaryRepository = FakeDiaryRepository()
            val client = FakeAssistantClient(reply = replyWithLogFood())
            val viewModel =
                viewModel(
                    client = client,
                    scope = scope(testScheduler),
                    diaryRepository = diaryRepository,
                    hasConsent = true,
                )

            viewModel.send("I ate oats")
            advanceUntilIdle()

            viewModel.confirmProposal()
            advanceUntilIdle()

            val logged = diaryRepository.addedEntries.single()
            assertEquals("Oats", logged.productName)
            assertEquals(30.0, logged.quantityGrams)
            assertEquals(MealType.BREAKFAST, logged.mealType)
            assertNull(viewModel.state.value.pendingProposal)
        }

    @Test
    fun anActionThatBreaksTheRulesIsRejectedAndNothingIsWritten() =
        runTest {
            val diaryRepository = FakeDiaryRepository()
            val client =
                FakeAssistantClient(
                    reply =
                        AssistantReply(
                            text = "I can log that",
                            actions =
                                listOf(
                                    AssistantAction.LogFood(
                                        productCode = "9999",
                                        quantityGrams = 30.0,
                                        mealType = MealType.BREAKFAST,
                                    ),
                                ),
                        ),
                )
            val viewModel =
                viewModel(
                    client = client,
                    scope = scope(testScheduler),
                    diaryRepository = diaryRepository,
                    hasConsent = true,
                )

            viewModel.send("I ate oats")
            advanceUntilIdle()

            viewModel.confirmProposal()
            advanceUntilIdle()

            assertEquals(0, diaryRepository.writeCount)
            assertNotNull(viewModel.state.value.error)
        }

    @Test
    fun dismissingTheProposalWritesNothing() =
        runTest {
            val diaryRepository = FakeDiaryRepository()
            val client = FakeAssistantClient(reply = replyWithLogFood())
            val viewModel =
                viewModel(
                    client = client,
                    scope = scope(testScheduler),
                    diaryRepository = diaryRepository,
                    hasConsent = true,
                )

            viewModel.send("I ate oats")
            advanceUntilIdle()

            viewModel.dismissProposal()
            advanceUntilIdle()

            assertNull(viewModel.state.value.pendingProposal)
            assertEquals(0, diaryRepository.writeCount)
        }

    @Test
    fun aFailedRequestKeepsTheTranscriptAndReportsTheError() =
        runTest {
            val client = FakeAssistantClient(failure = IllegalStateException("The assistant is not configured"))
            val viewModel = viewModel(client = client, scope = scope(testScheduler), hasConsent = true)

            viewModel.send("I ate oats")
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(1, state.messages.size)
            assertEquals("I ate oats", state.messages.single().text)
            assertFalse(state.isSending)
            assertEquals("The assistant is not configured", state.error)
        }

    @Test
    fun theContextCarriesTheDayTheGoalAndTheLibraryCodesOnly() =
        runTest {
            val diaryRepository = FakeDiaryRepository()
            diaryRepository.addEntry(entry())
            val goalRepository = FakeNutritionGoalRepository(NutritionGoal(energyKcal = 2400))
            val savedProductRepository = FakeSavedProductRepository(listOf(savedProduct()))
            val client = FakeAssistantClient()
            val viewModel =
                viewModel(
                    client = client,
                    scope = scope(testScheduler),
                    diaryRepository = diaryRepository,
                    goalRepository = goalRepository,
                    savedProductRepository = savedProductRepository,
                    hasConsent = true,
                )

            viewModel.send("What should I eat?")
            advanceUntilIdle()

            val context = client.requests.single().context
            assertEquals(1, context.entries.size)
            assertEquals(2400, context.goal.energyKcal)
            assertEquals(listOf("0001"), context.libraryCodes)
        }

    private fun scope(scheduler: TestCoroutineScheduler): CoroutineScope = CoroutineScope(StandardTestDispatcher(scheduler))

    private fun viewModel(
        client: FakeAssistantClient,
        scope: CoroutineScope,
        diaryRepository: FakeDiaryRepository = FakeDiaryRepository(),
        goalRepository: FakeNutritionGoalRepository = FakeNutritionGoalRepository(),
        savedProductRepository: FakeSavedProductRepository = FakeSavedProductRepository(),
        isConfigured: Boolean = true,
        hasConsent: Boolean = false,
        connectivity: ConnectivityStatus = ConnectivityStatus.ONLINE,
    ): CoachViewModel {
        val dayBoundaryProvider = FakeDayBoundaryProvider()
        val clock = EpochClock { 100L }
        return CoachViewModel(
            assistantClient = client,
            actionExecutor =
                AssistantActionExecutor(
                    diaryRepository = diaryRepository,
                    productRepository = FakeProductSearchRepository(listOf(product())),
                    savedProductRepository = savedProductRepository,
                    clock = clock,
                    dayBoundaryProvider = dayBoundaryProvider,
                ),
            diaryRepository = diaryRepository,
            goalRepository = goalRepository,
            savedProductRepository = savedProductRepository,
            dayBoundaryProvider = dayBoundaryProvider,
            clock = clock,
            scope = scope,
            isConfigured = isConfigured,
            hasConsent = hasConsent,
            connectivityStatusProvider = { connectivity },
        )
    }

    private fun replyWithLogFood(): AssistantReply =
        AssistantReply(
            text = "I can log 30 g of oats for breakfast",
            actions =
                listOf(
                    AssistantAction.LogFood(
                        productCode = "0001",
                        quantityGrams = 30.0,
                        mealType = MealType.BREAKFAST,
                    ),
                ),
        )

    private fun entry(): DiaryEntry =
        DiaryEntry(
            productCode = "0001",
            productName = "Oats",
            mealType = MealType.BREAKFAST,
            quantityGrams = 30.0,
            energyKcal = 114.0,
            loggedAtEpochMillis = 100L,
            dayEpochMillis = 0L,
        )

    private fun savedProduct(): SavedProduct =
        SavedProduct(
            code = "0001",
            name = "Oats",
            savedAtEpochMillis = 100L,
        )

    private fun product(): Product =
        Product(
            code = "0001",
            name = "Oats",
            nutritionValues =
                listOf(
                    NutritionValue("energy", 380.0, null, NutritionBasis.PER_100_G),
                    NutritionValue("protein", 10.0, null, NutritionBasis.PER_100_G),
                ),
        )
}
