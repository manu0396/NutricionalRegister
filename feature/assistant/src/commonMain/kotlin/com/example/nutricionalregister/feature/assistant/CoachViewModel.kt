package com.example.nutricionalregister.feature.assistant

import com.example.nutricionalregister.domain.assistant.ActionOutcome
import com.example.nutricionalregister.domain.assistant.AssistantActionExecutor
import com.example.nutricionalregister.domain.assistant.AssistantClient
import com.example.nutricionalregister.domain.assistant.AssistantContext
import com.example.nutricionalregister.domain.assistant.AssistantMessage
import com.example.nutricionalregister.domain.assistant.AssistantReply
import com.example.nutricionalregister.domain.assistant.AssistantRole
import com.example.nutricionalregister.domain.diary.DayBoundaryProvider
import com.example.nutricionalregister.domain.diary.DiaryRepository
import com.example.nutricionalregister.domain.diary.NutritionGoalRepository
import com.example.nutricionalregister.domain.product.ConnectivityStatus
import com.example.nutricionalregister.domain.product.ConnectivityStatusProvider
import com.example.nutricionalregister.domain.product.EpochClock
import com.example.nutricionalregister.domain.purchase.SavedProductRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The presentation logic of the Coach, the conversational assistant.
 *
 * The assistant may only *propose*: this view model sends the conversation to
 * the configured AI service, shows the answer and keeps the proposed change
 * pending until the user confirms it. Nothing is written to the diary before
 * that confirmation, and every proposed action is validated by the shared
 * domain rules before it is applied (FR-024).
 *
 * The context sent to the service is the minimal one: the entries of the day,
 * the goal and the codes of the user's own library. The catalogue, the recent
 * searches and the purchase records are never sent (FR-028).
 *
 * @param scope the lifecycle scope that owns the requests; the caller clears
 *   it on dispose.
 * @param isConfigured whether an AI service is configured for this build.
 * @param hasConsent whether the user already consented to sending messages to
 *   the AI service.
 * @param connectivityStatusProvider the connectivity of the device, when the
 *   platform can report it.
 */
public class CoachViewModel(
    private val assistantClient: AssistantClient,
    private val actionExecutor: AssistantActionExecutor,
    private val diaryRepository: DiaryRepository,
    private val goalRepository: NutritionGoalRepository,
    private val savedProductRepository: SavedProductRepository,
    private val dayBoundaryProvider: DayBoundaryProvider,
    private val clock: EpochClock,
    private val scope: CoroutineScope,
    private val isConfigured: Boolean,
    private val hasConsent: Boolean = false,
    private val connectivityStatusProvider: ConnectivityStatusProvider? = null,
) {
    private var consentGranted: Boolean = hasConsent

    private var offline: Boolean = false

    private val mutableState = MutableStateFlow(AssistantState(availability = availabilityFor(hasConsent)))

    /** The state the Coach screen renders. */
    public val state: StateFlow<AssistantState> = mutableState.asStateFlow()

    init {
        refreshConnectivity()
    }

    /**
     * Records the user's consent to send messages to the AI service.
     *
     * The consent is asked for before the first message is sent, and it is
     * granted here so the screen can offer the disclosure and the button
     * together (FR-026).
     */
    public fun grantConsent() {
        consentGranted = true
        mutableState.update { current ->
            current.copy(
                availability = availabilityFor(consentGranted),
                error = null,
            )
        }
    }

    /**
     * Sends [text] to the assistant and shows its answer.
     *
     * A blank text is ignored, a build without an AI service never attempts a
     * request and a conversation without consent never leaves the device. The
     * user's turn is appended to the transcript straight away, so nothing the
     * user typed is lost when the request fails.
     */
    public fun send(text: String) {
        val message = text.trim()
        if (message.isEmpty()) {
            return
        }
        if (!isConfigured) {
            mutableState.update { current -> current.copy(availability = AssistantAvailability.NotConfigured) }
            return
        }
        if (!consentGranted) {
            mutableState.update { current -> current.copy(availability = AssistantAvailability.ConsentRequired) }
            return
        }
        mutableState.update { current ->
            current.copy(
                availability = AssistantAvailability.Ready,
                messages = current.messages + AssistantMessage(AssistantRole.USER, message),
                isSending = true,
                error = null,
            )
        }
        scope.launch {
            if (isOffline()) {
                mutableState.update { current ->
                    current.copy(
                        availability = AssistantAvailability.Offline,
                        isSending = false,
                        error = OFFLINE_MESSAGE,
                    )
                }
                return@launch
            }
            try {
                val reply = assistantClient.complete(mutableState.value.messages, buildContext())
                showReply(reply)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                mutableState.update { current ->
                    current.copy(
                        isSending = false,
                        error = failure.message ?: UNKNOWN_ERROR_MESSAGE,
                    )
                }
            }
        }
    }

    /**
     * Applies the pending proposal to the diary.
     *
     * Every action of the proposal goes through the shared executor, which
     * validates it first: an action that breaks a rule is rejected with an
     * explanation and the diary is left as it was. The proposal is cleared
     * either way, so a proposal can never be applied twice (FR-024).
     */
    public fun confirmProposal() {
        val proposal = mutableState.value.pendingProposal ?: return
        mutableState.update { current -> current.copy(pendingProposal = null) }
        scope.launch {
            for (action in proposal.actions) {
                val outcome = actionExecutor.apply(action)
                if (outcome is ActionOutcome.Rejected) {
                    mutableState.update { current -> current.copy(error = outcome.reason) }
                }
            }
        }
    }

    /** Drops the pending proposal; nothing is written to the diary. */
    public fun dismissProposal() {
        mutableState.update { current -> current.copy(pendingProposal = null) }
    }

    private fun showReply(reply: AssistantReply) {
        val answer = reply.text.takeIf { text -> text.isNotBlank() }?.let { text -> AssistantMessage(AssistantRole.ASSISTANT, text) }
        mutableState.update { current ->
            current.copy(
                messages = if (answer == null) current.messages else current.messages + answer,
                pendingProposal = reply,
                isSending = false,
            )
        }
    }

    /**
     * The context the assistant is allowed to see.
     *
     * Only the entries of the day, the goal and the codes of the user's own
     * library are collected; the catalogue, the recent searches and the
     * purchase records never leave the device (FR-028).
     */
    private suspend fun buildContext(): AssistantContext =
        AssistantContext(
            entries =
                diaryRepository
                    .observeEntriesForDay(dayBoundaryProvider.startOfDay(clock.nowEpochMillis()))
                    .first(),
            goal = goalRepository.observeGoal().first(),
            // The foods the user keeps, each with the nutrition the catalogue
            // reports for it, so a plan for the day can be made of them.
            library = actionExecutor.libraryFoods(),
        )

    private fun refreshConnectivity() {
        val provider = connectivityStatusProvider ?: return
        scope.launch {
            offline = provider.currentStatus() == ConnectivityStatus.OFFLINE
            mutableState.update { current -> current.copy(availability = availabilityFor(consentGranted)) }
        }
    }

    private suspend fun isOffline(): Boolean {
        val provider = connectivityStatusProvider ?: return false
        offline = provider.currentStatus() == ConnectivityStatus.OFFLINE
        return offline
    }

    /**
     * The availability of the assistant, in the order the gates apply.
     *
     * A build without an AI service is switched off whatever else is true, a
     * conversation without consent never starts and only a reachable service
     * is ready.
     */
    private fun availabilityFor(consent: Boolean): AssistantAvailability =
        when {
            !isConfigured -> AssistantAvailability.NotConfigured
            !consent -> AssistantAvailability.ConsentRequired
            offline -> AssistantAvailability.Offline
            else -> AssistantAvailability.Ready
        }

    private companion object {
        /** The explanation shown when the device has no connection. */
        const val OFFLINE_MESSAGE: String = "The assistant needs a connection to answer"

        /** The explanation shown when the failure carries no reason of its own. */
        const val UNKNOWN_ERROR_MESSAGE: String = "The assistant could not answer"
    }
}
