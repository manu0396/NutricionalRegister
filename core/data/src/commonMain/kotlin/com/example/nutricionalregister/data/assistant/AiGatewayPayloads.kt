package com.example.nutricionalregister.data.assistant

import com.example.nutricionalregister.domain.assistant.AssistantAction
import com.example.nutricionalregister.domain.assistant.AssistantContext
import com.example.nutricionalregister.domain.assistant.AssistantLibraryFood
import com.example.nutricionalregister.domain.assistant.AssistantMessage
import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.MealType
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

/**
 * The body the app posts to the AI gateway.
 *
 * The envelope carries the conversation and the minimal context, and nothing
 * else: the catalogue, the recent search terms and the purchase records never
 * reach this object, so they cannot reach the network (FR-028).
 *
 * @property messages the conversation so far.
 * @property context the entries of the day, the goal and the library codes.
 */
@Serializable
internal data class GatewayRequest(
    val messages: List<GatewayMessage>,
    val context: GatewayContext,
)

/**
 * One turn of the conversation, as the gateway sees it.
 *
 * @property role who wrote the turn.
 * @property text what was said.
 */
@Serializable
internal data class GatewayMessage(
    val role: String,
    val text: String,
)

/**
 * One food of the user's library, as the gateway sees it.
 *
 * A plan for the day needs to know what a food contains, so the food is
 * carried with the nutrition the catalogue reports for it per 100 g; a value
 * the catalogue does not report is omitted, so the assistant can speak only of
 * the amounts it can see (FR-028).
 *
 * @property code the catalogue identity of the food.
 * @property name the food's display name, so a plan can say what it proposes.
 * @property energyKcal the calories per 100 g, when reported.
 * @property proteinGrams the protein per 100 g, when reported.
 * @property carbohydrateGrams the carbohydrate per 100 g, when reported.
 * @property fatGrams the fat per 100 g, when reported.
 */
@Serializable
internal data class GatewayLibraryFood(
    val code: String,
    val name: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val energyKcal: Double? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val proteinGrams: Double? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val carbohydrateGrams: Double? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val fatGrams: Double? = null,
)

/**
 * The minimal diary context the gateway is allowed to see.
 *
 * @property entries the entries of the day.
 * @property goal the daily targets.
 * @property library the foods the user keeps, with what each one contains.
 */
@Serializable
internal data class GatewayContext(
    val entries: List<GatewayEntry>,
    val goal: GatewayGoal,
    val library: List<GatewayLibraryFood> = emptyList(),
)

/**
 * One entry of the day, as the gateway sees it.
 *
 * The entry is the user's own log, so it carries the nutrients that were
 * logged with it: the assistant can only talk about the day if it can see what
 * the day contains. The nutrient values are omitted when the catalogue did not
 * report them, so a missing value always means "not reported".
 *
 * @property entryId the identifier the assistant refers to.
 * @property productCode the catalogue identity of the logged product.
 * @property productName the product name at log time.
 * @property meal the meal the entry belongs to.
 * @property quantityGrams the logged quantity, in grams.
 * @property energyKcal the calories the logged quantity contributes.
 * @property proteinGrams the protein the logged quantity contributes.
 * @property carbohydrateGrams the carbohydrates the logged quantity contributes.
 * @property fatGrams the fat the logged quantity contributes.
 */
@Serializable
internal data class GatewayEntry(
    val entryId: Long,
    val productCode: String,
    val productName: String,
    val meal: String,
    val quantityGrams: Double,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val energyKcal: Double? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val proteinGrams: Double? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val carbohydrateGrams: Double? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val fatGrams: Double? = null,
)

/**
 * The daily targets, as the gateway sees them.
 *
 * @property energyKcal the daily calorie target.
 * @property proteinGrams the daily protein target, in grams.
 * @property carbohydrateGrams the daily carbohydrate target, in grams.
 * @property fatGrams the daily fat target, in grams.
 */
@Serializable
internal data class GatewayGoal(
    val energyKcal: Int,
    val proteinGrams: Int,
    val carbohydrateGrams: Int,
    val fatGrams: Int,
)

/**
 * The answer the gateway sends back.
 *
 * @property reply the text shown to the user; it is displayed and never run.
 * @property actions the changes the assistant proposes.
 */
@Serializable
internal data class GatewayResponse(
    val reply: String,
    val actions: List<GatewayAction> = emptyList(),
)

/**
 * One proposed change, as the gateway sends it.
 *
 * The kind of the action is read from [type], or from [kind] when [type] is
 * absent. Every field is optional here on purpose: an action that is missing
 * a field it needs is dropped instead of failing the whole reply.
 *
 * @property type the kind of the action: `log`, `remove` or `change`.
 * @property kind the kind of the action, when [type] is not used.
 * @property productCode the product to log, for a `log` action.
 * @property quantityGrams the quantity in grams, for a `log` or `change` action.
 * @property meal the meal, for a `log` or `change` action.
 * @property entryId the entry to remove or change.
 */
@Serializable
internal data class GatewayAction(
    val type: String? = null,
    val kind: String? = null,
    val productCode: String? = null,
    val quantityGrams: Double? = null,
    val meal: String? = null,
    val entryId: Long? = null,
)

/**
 * The request the app sends for one turn of the conversation.
 */
internal fun gatewayRequest(
    messages: List<AssistantMessage>,
    context: AssistantContext,
): GatewayRequest =
    GatewayRequest(
        messages = messages.map { message -> GatewayMessage(role = message.role.name, text = message.text) },
        context = context.toGatewayContext(),
    )

/**
 * The context of the request, with only what the app is allowed to send.
 */
internal fun AssistantContext.toGatewayContext(): GatewayContext =
    GatewayContext(
        entries = entries.map { entry -> entry.toGatewayEntry() },
        goal =
            GatewayGoal(
                energyKcal = goal.energyKcal,
                proteinGrams = goal.proteinGrams,
                carbohydrateGrams = goal.carbohydrateGrams,
                fatGrams = goal.fatGrams,
            ),
        library = library.map { food -> food.toGatewayFood() },
    )

/**
 * One food of the library, with only what the app is allowed to send.
 */
private fun AssistantLibraryFood.toGatewayFood(): GatewayLibraryFood =
    GatewayLibraryFood(
        code = code,
        name = name,
        energyKcal = energyKcal,
        proteinGrams = proteinGrams,
        carbohydrateGrams = carbohydrateGrams,
        fatGrams = fatGrams,
    )

/**
 * One entry of the day, with only what the app is allowed to send.
 */
private fun DiaryEntry.toGatewayEntry(): GatewayEntry =
    GatewayEntry(
        entryId = id,
        productCode = productCode,
        productName = productName,
        meal = mealType.name,
        quantityGrams = quantityGrams,
        energyKcal = energyKcal,
        proteinGrams = proteinGrams,
        carbohydrateGrams = carbohydrateGrams,
        fatGrams = fatGrams,
    )

/**
 * The action the app can honour, or null when it cannot.
 *
 * An action the app cannot honour - an unknown kind, a missing field, a
 * quantity that is not a usable amount - is dropped here: the reply text is
 * still shown, and nothing that does not make sense reaches the diary
 * (FR-024).
 *
 * A quantity that cannot be a number at all never reaches this function: the
 * JSON decoder refuses the whole reply instead. Whether a well-formed
 * proposal may actually be applied is decided by the domain executor, which
 * is the single place where a proposal becomes a diary change.
 */
internal fun GatewayAction.toAssistantAction(): AssistantAction? {
    val actionKind = (type ?: kind)?.trim()?.lowercase()
    return when (actionKind) {
        LOG_ACTION -> toLogFood()
        REMOVE_ACTION -> toRemoveEntry()
        CHANGE_ACTION -> toChangeEntry()
        else -> null
    }
}

private fun GatewayAction.toLogFood(): AssistantAction? {
    val code = productCode?.trim().orEmpty()
    if (code.isEmpty()) {
        return null
    }
    val quantity = quantityGrams
    if (quantity == null || quantity <= 0.0) {
        return null
    }
    val meal = toMealType()
    if (meal == null) {
        return null
    }
    return AssistantAction.LogFood(productCode = code, quantityGrams = quantity, mealType = meal)
}

private fun GatewayAction.toRemoveEntry(): AssistantAction? {
    val entryId = entryId
    if (entryId == null || entryId <= 0L) {
        return null
    }
    return AssistantAction.RemoveEntry(entryId = entryId)
}

private fun GatewayAction.toChangeEntry(): AssistantAction? {
    val entryId = entryId
    if (entryId == null || entryId <= 0L) {
        return null
    }
    val quantity = quantityGrams
    if (quantity != null && quantity <= 0.0) {
        return null
    }
    return AssistantAction.ChangeEntry(
        entryId = entryId,
        quantityGrams = quantity,
        mealType = toMealType(),
    )
}

/**
 * The meal the action names, or null when it names none the app knows.
 */
private fun GatewayAction.toMealType(): MealType? {
    val value = meal?.trim().orEmpty()
    if (value.isEmpty()) {
        return null
    }
    return MealType.entries.firstOrNull { mealType -> mealType.name.equals(value, ignoreCase = true) }
}

private const val LOG_ACTION: String = "log"
private const val REMOVE_ACTION: String = "remove"
private const val CHANGE_ACTION: String = "change"
