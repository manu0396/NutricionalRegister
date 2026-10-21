package com.example.nutricionalregister.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.nutricionalregister.domain.diary.DayBoundaryProvider
import com.example.nutricionalregister.domain.diary.DiaryEntry
import com.example.nutricionalregister.domain.diary.DiaryRepository
import com.example.nutricionalregister.domain.diary.MealType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * SQLDelight-backed [DiaryRepository] over the on-device `DiaryEntry` table.
 *
 * The diary is the app's primary record, so every read is local: no method
 * performs a network call (FR-018). Entries are grouped by the local day they
 * were logged in, which is stored on the row itself so that a later time-zone
 * change cannot move a past entry into a different day.
 */
public class DiaryRepositoryImpl(
    private val database: ProductDatabase,
    private val dayBoundaryProvider: DayBoundaryProvider,
    private val databaseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : DiaryRepository {
    override fun observeEntriesForDay(dayEpochMillis: Long): Flow<List<DiaryEntry>> =
        database.diaryEntryQueries
            .selectDiaryEntriesForDay(dayEpochMillis)
            .asFlow()
            .mapToList(databaseDispatcher)
            .map { rows -> rows.map { row -> row.toDiaryEntry() } }

    override fun observeEntriesForWeek(dayEpochMillis: Long): Flow<List<DiaryEntry>> {
        val firstDay = dayBoundaryProvider.addDays(dayEpochMillis, -(WEEK_LENGTH_IN_DAYS - 1))
        return database.diaryEntryQueries
            .selectDiaryEntriesBetweenDays(firstDay, dayEpochMillis)
            .asFlow()
            .mapToList(databaseDispatcher)
            .map { rows -> rows.map { row -> row.toDiaryEntry() } }
    }

    override suspend fun addEntry(entry: DiaryEntry): Long =
        withContext(databaseDispatcher) {
            var newEntryId = 0L
            database.transaction {
                database.diaryEntryQueries.insertDiaryEntry(
                    product_code = entry.productCode,
                    product_name = entry.productName,
                    brands = entry.brands,
                    image_thumbnail_url = entry.imageThumbnailUrl,
                    meal_type = entry.mealType.name,
                    quantity_grams = entry.quantityGrams,
                    unit = entry.unit,
                    energy_kcal = entry.energyKcal,
                    protein_grams = entry.proteinGrams,
                    carbohydrate_grams = entry.carbohydrateGrams,
                    fat_grams = entry.fatGrams,
                    logged_at_epoch_millis = entry.loggedAtEpochMillis,
                    day_epoch_millis = entry.dayEpochMillis,
                )
                newEntryId = database.diaryEntryQueries.selectLastInsertedDiaryEntryId().executeAsOne()
            }
            newEntryId
        }

    override suspend fun updateEntry(entry: DiaryEntry) {
        withContext(databaseDispatcher) {
            database.diaryEntryQueries.updateDiaryEntry(
                product_code = entry.productCode,
                product_name = entry.productName,
                brands = entry.brands,
                image_thumbnail_url = entry.imageThumbnailUrl,
                meal_type = entry.mealType.name,
                quantity_grams = entry.quantityGrams,
                unit = entry.unit,
                energy_kcal = entry.energyKcal,
                protein_grams = entry.proteinGrams,
                carbohydrate_grams = entry.carbohydrateGrams,
                fat_grams = entry.fatGrams,
                logged_at_epoch_millis = entry.loggedAtEpochMillis,
                day_epoch_millis = entry.dayEpochMillis,
                id = entry.id,
            )
        }
    }

    override suspend fun removeEntry(id: Long) {
        withContext(databaseDispatcher) {
            database.diaryEntryQueries.deleteDiaryEntry(id)
        }
    }

    private companion object {
        private const val WEEK_LENGTH_IN_DAYS = 7
    }
}

/**
 * Maps the generated row to the domain entity.
 *
 * The generated row type lives in this package and clashes with the domain
 * type of the same name, so it is referenced by its fully qualified name.
 */
private fun com.example.nutricionalregister.data.DiaryEntry.toDiaryEntry(): DiaryEntry =
    DiaryEntry(
        id = id,
        productCode = product_code,
        productName = product_name,
        brands = brands,
        imageThumbnailUrl = image_thumbnail_url,
        mealType = MealType.fromStorage(meal_type),
        quantityGrams = quantity_grams,
        unit = unit,
        energyKcal = energy_kcal,
        proteinGrams = protein_grams,
        carbohydrateGrams = carbohydrate_grams,
        fatGrams = fat_grams,
        loggedAtEpochMillis = logged_at_epoch_millis,
        dayEpochMillis = day_epoch_millis,
    )
