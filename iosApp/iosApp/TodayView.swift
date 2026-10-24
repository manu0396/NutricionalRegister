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
    ScrollView {
      VStack(alignment: .leading, spacing: 16) {
        header
        if let state = viewModel.diaryState {
          totalsCard(state)
          macrosRow(state)
          if state.isEmpty {
            emptyState
          }
        } else {
          ProgressView()
        }
        if let weekly = viewModel.weeklySummary {
          weeklyCard(weekly)
        }
      }
      .padding()
    }
    .navigationTitle("Today")
    .toolbar {
      Button("Refresh") { viewModel.refresh() }
    }
    .onAppear { viewModel.refresh() }
  }

  private var header: some View {
    HStack {
      Text("NutriLog")
        .font(.title2.bold())
      Spacer()
      Button("Week") { navigator.openWeekly() }
    }
  }

  private func totalsCard(_ state: DiaryState) -> some View {
    VStack(alignment: .leading, spacing: 4) {
      Text("Totals")
        .font(.largeTitle.bold())
    }
    .padding()
    .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16))
  }

  private func macrosRow(_ state: DiaryState) -> some View {
    HStack {
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
  }

  private var emptyState: some View {
    VStack(alignment: .leading, spacing: 8) {
      Text("Nothing logged today")
        .font(.headline)
      Text("Log your first food and the totals of the day build up from there.")
        .font(.footnote)
        .foregroundStyle(.secondary)
    }
    .padding()
  }

  private func mealSection(_ meal: MealType, entries: [DiaryEntry]) -> some View {
    VStack(alignment: .leading, spacing: 8) {
      HStack {
        Text(meal.displayName)
          .font(.headline)
        Spacer()
        Button("Add") { navigator.openAddFood(meal: meal.storageName) }
          .buttonStyle(.bordered)
      }
      if entries.isEmpty {
        Text("Nothing logged yet")
          .font(.footnote)
          .foregroundStyle(.secondary)
      } else {
        Text("\(entries.count ?? 0) entries")
          .font(.footnote)
      }
    }
    .padding()
    .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16))
  }

  private func weeklyCard(_ summary: WeeklySummary) -> some View {
    VStack(alignment: .leading, spacing: 4) {
      Text("This week")
        .font(.headline)
      Text("Average \(Int((summary.averageTotals.energyKcal as? Double) ?? 0)) kcal/day")
        .font(.footnote)
        .foregroundStyle(.secondary)
    }
    .padding()
    .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16))
    .onTapGesture { navigator.openWeekly() }
  }
}

extension MealType {
  static var allCases: [MealType] {
    [breakfast, lunch, dinner, snacks]
  }

  var displayName: String {
    switch self {
    case .breakfast: return "Breakfast"
    case .lunch: return "Lunch"
    case .dinner: return "Dinner"
    case .snacks: return "Snacks"
    default: return name
    }
  }

  var storageName: String {
    name
  }
}
