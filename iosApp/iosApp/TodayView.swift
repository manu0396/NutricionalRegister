import Shared
import SwiftUI

@MainActor
final class TodayViewModel: ObservableObject {
  @Published private(set) var diaryState: DiaryState?
  @Published private(set) var weeklySummary: WeeklySummary?

  private let bridge: IosAppBridge
  private var subs: [Int64] = []

  init(bridge: IosAppBridge) {
    self.bridge = bridge
    subs.append(
      bridge.observeDiaryState { [weak self] state in
        self?.diaryState = state
      })
    subs.append(
      bridge.observeWeeklySummary { [weak self] summary in
        self?.weeklySummary = summary
      })
  }

  func refresh() {
    bridge.refreshDiary()
  }

  func removeEntry(id: Int64) {
    bridge.removeDiaryEntry(id: id)
  }

  deinit {
    for sub in subs { bridge.cancelSubscription(subscriptionId: sub) }
  }
}

struct TodayView: View {
  let bridge: IosAppBridge
  @EnvironmentObject private var navigator: AppNavigator
  @StateObject private var viewModel: TodayViewModel

  init(bridge: IosAppBridge) {
    self.bridge = bridge
    _viewModel = StateObject(wrappedValue: TodayViewModel(bridge: bridge))
  }

  var body: some View {
    VStack(alignment: .leading, spacing: 0) {
      HStack {
        Text(formatDayLabel(viewModel.diaryState?.dayEpochMillis ?? 0))
          .font(.title2.bold())
          .frame(maxWidth: .infinity, alignment: .leading)
        Button("Week") { navigator.openWeekly() }
          .buttonStyle(.bordered)
      }
      .padding()

      ScrollView {
        VStack(alignment: .leading, spacing: 16) {
          if let state = viewModel.diaryState {
            DaySummaryCard(state: state)
            MacroTiles(state: state)
            if state.isEmpty {
              VStack(alignment: .leading, spacing: 8) {
                Text("Nothing logged today")
                  .font(.headline)
                Text("Log your first food and the totals of the day build up from there.")
                  .font(.footnote)
                  .foregroundStyle(.secondary)
                Button("Add food") { navigator.openAddFood() }
                  .buttonStyle(.borderedProminent)
              }
              .padding()
            }
            ForEach(
              [MealType.breakfast, MealType.lunch, MealType.dinner, MealType.snacks], id: \.self
            ) { meal in
              MealSection(
                meal: meal,
                entries: (state.entriesByMeal[meal] as? [DiaryEntry]) ?? [],
                totals: state.mealTotals[meal]
                  ?? NutritionTotals(
                    energyKcal: 0, proteinGrams: 0, carbohydrateGrams: 0, fatGrams: 0,
                    incomplete: Set<Nutrient>()),
                onAddFood: { navigator.openAddFood(meal: meal.storageName) },
                onRemoveEntry: { id in viewModel.removeEntry(id: id) }
              )
            }
            WeekCard(summary: viewModel.weeklySummary, onOpen: { navigator.openWeekly() })
          } else {
            ProgressView()
              .frame(maxWidth: .infinity, maxHeight: .infinity)
          }
        }
        .padding()
      }
    }
    .onAppear { viewModel.refresh() }
  }
}

private func formatDayLabel(_ millis: Int64) -> String {
  let date = Date(timeIntervalSince1970: Double(millis) / 1000.0)
  let formatter = DateFormatter()
  formatter.dateStyle = .full
  formatter.timeStyle = .none
  return formatter.string(from: date)
}

private struct DaySummaryCard: View {
  let state: DiaryState

  var body: some View {
    VStack(alignment: .leading, spacing: 8) {
      Text("\(Int((state.totals.energyKcal as? Double) ?? 0)) kcal")
        .font(.largeTitle.bold())
      Text("of \(state.goal.energyKcal) kcal goal")
        .foregroundStyle(.secondary)
      Text("Remaining today")
        .font(.footnote)
        .foregroundStyle(.secondary)
    }
    .frame(maxWidth: .infinity, alignment: .leading)
    .padding()
    .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16))
  }
}

private struct MacroTiles: View {
  let state: DiaryState

  var body: some View {
    HStack(spacing: 12) {
      macroTile(
        "Protein", value: (state.totals.proteinGrams as? Double) ?? 0,
        goal: Double(state.goal.proteinGrams))
      macroTile(
        "Carbs", value: (state.totals.carbohydrateGrams as? Double) ?? 0,
        goal: Double(state.goal.carbohydrateGrams))
      macroTile(
        "Fat", value: (state.totals.fatGrams as? Double) ?? 0, goal: Double(state.goal.fatGrams))
    }
  }

  private func macroTile(_ title: String, value: Double, goal: Double) -> some View {
    VStack {
      Text("\(Int(value)) g")
        .font(.headline)
      Text("\(title), goal \(Int(goal)) g")
        .font(.caption)
        .foregroundStyle(.secondary)
    }
    .frame(maxWidth: .infinity)
    .padding()
    .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 12))
  }
}

private struct MealSection: View {
  let meal: MealType
  let entries: [DiaryEntry]
  let totals: NutritionTotals
  let onAddFood: () -> Void
  let onRemoveEntry: (Int64) -> Void

  var body: some View {
    VStack(alignment: .leading, spacing: 8) {
      HStack {
        Text(mealName(meal))
          .font(.headline)
        Spacer()
        Button("Add") { onAddFood() }
          .buttonStyle(.bordered)
      }
      if entries.isEmpty {
        Text("Nothing logged yet")
          .font(.footnote)
          .foregroundStyle(.secondary)
      } else {
        Text("\(Int((totals.energyKcal as? Double) ?? 0)) kcal")
          .font(.footnote)
          .foregroundStyle(.secondary)
        Text("\(entries.count ?? 0) entries")
          .font(.caption)
          .foregroundStyle(.secondary)
      }
    }
    .padding()
    .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16))
  }

  private func mealName(_ meal: MealType) -> String {
    switch meal {
    case .breakfast: return "Breakfast"
    case .lunch: return "Lunch"
    case .dinner: return "Dinner"
    case .snacks: return "Snacks"
    default: return meal.name
    }
  }
}

private struct WeekCard: View {
  let summary: WeeklySummary?
  let onOpen: () -> Void

  var body: some View {
    Button(action: onOpen) {
      VStack(alignment: .leading, spacing: 4) {
        Text("This week")
          .font(.headline)
        if let summary {
          Text("Average \(Int((summary.averageTotals.energyKcal as? Double) ?? 0)) kcal/day")
            .font(.footnote)
            .foregroundStyle(.secondary)
        } else {
          Text("Loading weekly summary")
            .font(.footnote)
            .foregroundStyle(.secondary)
        }
      }
      .frame(maxWidth: .infinity, alignment: .leading)
      .padding()
      .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16))
    }
    .buttonStyle(.plain)
  }
}

extension MealType {
  var storageName: String { name }
}
