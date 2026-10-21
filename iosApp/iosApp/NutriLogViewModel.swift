import Shared
import SwiftUI

/// Owns the shared runtime bridge and republishes the shared saved-products and
/// purchase state to SwiftUI.
///
/// All storage and purchase logic lives in Kotlin Multiplatform; this object only
/// subscribes through `IosNutriLogBridge` and forwards actions, so the iOS and
/// Android surfaces share one source of truth (FR-002, FR-009 to FR-013).
@MainActor
final class NutriLogViewModel: ObservableObject {
  @Published private(set) var savedProducts: [SavedProduct] = []
  @Published private(set) var isLoadingSaved: Bool = true
  @Published private(set) var purchaseStates: [String: PurchaseState] = [:]
  @Published private(set) var purchaseRecords: [String: PurchaseRecord] = [:]
  @Published private(set) var purchasedCodes: Set<String> = []
  @Published var restoreMessage: String?

  private let bridge: IosNutriLogBridge
  private let store: StoreKitPurchaseClient
  private var subscriptions: [Int64] = []

  init(store: StoreKitPurchaseClient = StoreKitPurchaseClient()) {
    self.store = store
    self.bridge = IosNutriLogBridgeKt.createIosNutriLogBridge(purchaseClient: store)

    subscriptions.append(
      bridge.observeSavedProducts { [weak self] products in
        guard let self else { return }
        self.savedProducts = products
        self.isLoadingSaved = false
      }
    )
    subscriptions.append(
      bridge.observePurchasedCodes { [weak self] codes in
        self?.purchasedCodes = Set(codes)
      }
    )
  }

  /// Reconciles local records against the App Store and refreshes purchased codes
  /// (FR-013, restore affordance).
  func restorePurchases() {
    restoreMessage = "Restoring purchases..."
    store.restore { synced in
      Task { @MainActor in
        self.bridge.reconcile { codes in
          self.purchasedCodes = Set(codes)
          if synced {
            self.restoreMessage =
              codes.isEmpty
              ? "No previous purchases were found."
              : "Restored \(codes.count) purchase(s)."
          } else {
            self.restoreMessage = "Could not reach the App Store. Please try again."
          }
        }
      }
    }
  }

  func removeSavedProduct(code: String) {
    bridge.removeSavedProduct(code: code)
  }

  func isPurchased(code: String) -> Bool {
    purchasedCodes.contains(code)
  }

  /// Reports the store's price label for [code] when the product is offered, so
  /// the purchase action appears only for eligible products (FR-015).
  func queryProductPrice(code: String, onResult: @escaping (String?) -> Void) {
    bridge.queryProduct(code: code) { product in
      onResult(product?.amountLabel)
    }
  }

  func purchase(code: String, amountLabel: String) {
    purchaseStates[code] = PurchaseStatePending.shared
    _ = bridge.purchase(code: code) { [weak self] state in
      guard let self else { return }
      self.purchaseStates[code] = state
      if let record = state as? PurchaseStatePurchased {
        self.purchaseRecords[code] = record.record
        self.purchasedCodes.insert(code)
      }
    }
  }

  func purchaseState(for code: String) -> PurchaseState {
    purchaseStates[code] ?? PurchaseStateIdle.shared
  }

  deinit {
    for id in subscriptions {
      bridge.cancelSubscription(subscriptionId: id)
    }
    bridge.close()
  }
}
