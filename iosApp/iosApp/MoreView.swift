import Shared
import SwiftUI

@MainActor
final class MoreViewModel: ObservableObject {
  @Published private(set) var goal: NutritionGoal = NutritionGoal(
    energyKcal: 2000, proteinGrams: 120, carbohydrateGrams: 220, fatGrams: 70)
  @Published private(set) var premiumState: PremiumPurchaseState?

  private let bridge: IosAppBridge
  private var subs: [Int64] = []

  init(bridge: IosAppBridge) {
    self.bridge = bridge
    subs.append(
      bridge.observeDiaryState { [weak self] state in
        self?.goal = state.goal
      })
    subs.append(
      bridge.observePremiumState { [weak self] state in
        self?.premiumState = state
      })
  }

  func updateGoal(_ goal: NutritionGoal) {
    bridge.updateGoal(goal: goal)
  }

  func refreshPremium() { bridge.refreshPremium() }
  func buyPremium() { bridge.startPremiumPurchase() }
  func restorePremium() { bridge.restorePremiumPurchases() }

  deinit {
    for sub in subs { bridge.cancelSubscription(subscriptionId: sub) }
  }
}

struct MoreView: View {
  let bridge: IosAppBridge
  @EnvironmentObject private var appRoot: AppRoot
  @EnvironmentObject private var navigator: AppNavigator
  @StateObject private var viewModel: MoreViewModel

  @State private var calories: String = "2000"
  @State private var protein: String = "120"
  @State private var carbs: String = "220"
  @State private var fat: String = "70"

  init(bridge: IosAppBridge) {
    self.bridge = bridge
    _viewModel = StateObject(wrappedValue: MoreViewModel(bridge: bridge))
  }

  var body: some View {
    ScrollView {
      VStack(alignment: .leading, spacing: 16) {
        Section("Goals") {
          TextField("Calories", text: $calories)
          TextField("Protein (g)", text: $protein)
          TextField("Carbs (g)", text: $carbs)
          TextField("Fat (g)", text: $fat)
          Button("Save goals") {
            if let c = Int(calories), let p = Int(protein), let cb = Int(carbs), let f = Int(fat) {
              viewModel.updateGoal(
                NutritionGoal(
                  energyKcal: Int32(c), proteinGrams: Int32(p), carbohydrateGrams: Int32(cb),
                  fatGrams: Int32(f)))
            }
          }
        }

        Section("Premium") {
          if let state = viewModel.premiumState {
            Text(state.isUnlocked ? "Unlocked" : "Locked")
            if let price = state.amountLabel {
              Text(price)
            }
            Button("Buy") { viewModel.buyPremium() }
              .disabled(!state.isEligibleForPurchase || state.isPending)
            Button("Restore purchases") { viewModel.restorePremium() }
          } else {
            ProgressView()
          }
        }

        Section {
          Button("Privacy policy") { appRoot.showPrivacyPolicy = true }
          Button("Ask the Coach") { appRoot.showCoach = true }
            .disabled(!(viewModel.premiumState?.isUnlocked ?? false) && !isDebugBuild)
        }
      }
    }
    .navigationTitle("More")
    .onAppear { viewModel.refreshPremium() }
  }

  private var isDebugBuild: Bool {
    #if DEBUG
      return true
    #else
      return false
    #endif
  }
}
