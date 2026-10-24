import Shared
import SwiftUI

@MainActor
final class WeeklyViewModel: ObservableObject {
  @Published private(set) var summary: WeeklySummary?

  private let bridge: IosAppBridge
  private var sub: Int64?

  init(bridge: IosAppBridge) {
    self.bridge = bridge
    sub = bridge.observeWeeklySummary { [weak self] summary in
      self?.summary = summary
    }
  }

  func refresh() { bridge.refreshWeekly() }

  deinit {
    if let sub { bridge.cancelSubscription(subscriptionId: sub) }
  }
}

struct WeeklyView: View {
  let bridge: IosAppBridge
  @StateObject private var viewModel: WeeklyViewModel

  init(bridge: IosAppBridge) {
    self.bridge = bridge
    _viewModel = StateObject(wrappedValue: WeeklyViewModel(bridge: bridge))
  }

  var body: some View {
    ScrollView {
      VStack(alignment: .leading, spacing: 12) {
        if let summary = viewModel.summary {
          Text("Weekly summary").font(.title2.bold())
          Text("Days with entries: \(summary.daysWithEntries)")
          Text("Average calories: \(Int(summary.averageTotals.energyKcal ?? 0)) kcal")
          Text("Days: \(summary.days.count)")
        } else {
          ProgressView()
        }
      }
      .padding()
    }
    .navigationTitle("Weekly")
    .onAppear { viewModel.refresh() }
  }
}
