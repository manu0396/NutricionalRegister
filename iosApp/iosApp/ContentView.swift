import Shared
import SwiftUI

/// The iOS app surface for feature 004: a saved-products list and the native
/// purchase UI, both driven by the shared Kotlin state through
/// `NutriLogViewModel` (FR-002, FR-014, SC-009).
///
/// The bottom navigation bar is Android UI in this feature; the iOS surfaces are
/// reached directly from this screen (see plan.md, Technical Context). No shared
/// UI is used, per the constitution's native-UI boundary.
struct ContentView: View {
  @StateObject private var viewModel = NutriLogViewModel()

  var body: some View {
    NavigationStack {
      List {
        purchaseRestoreSection
        Section("Saved products") {
          savedProductsContent
        }
      }
      .navigationTitle("NutriLog")
    }
  }

  // MARK: - Restore

  private var purchaseRestoreSection: some View {
    Section("Purchases") {
      Button("Restore purchases") {
        viewModel.restorePurchases()
      }
      .frame(minHeight: 44)
      .accessibilityLabel("Restore purchases")

      if let message = viewModel.restoreMessage {
        Text(message)
          .font(.footnote)
          .foregroundStyle(.secondary)
      }
    }
  }

  // MARK: - Saved products

  @ViewBuilder
  private var savedProductsContent: some View {
    if viewModel.isLoadingSaved {
      HStack {
        ProgressView()
        Text("Loading saved products...")
          .foregroundStyle(.secondary)
      }
    } else if viewModel.savedProducts.isEmpty {
      VStack(alignment: .leading, spacing: 8) {
        Text("No saved products yet")
          .font(.headline)
        Text("Products you save from search will appear here, available offline.")
          .font(.footnote)
          .foregroundStyle(.secondary)
      }
      .accessibilityElement(children: .combine)
    } else {
      ForEach(viewModel.savedProducts, id: \.code) { product in
        SavedProductRow(product: product, viewModel: viewModel)
      }
    }
  }
}

/// One saved product row, including the purchase control for eligible products.
private struct SavedProductRow: View {
  let product: SavedProduct
  @ObservedObject var viewModel: NutriLogViewModel

  @State private var priceLabel: String?
  @State private var didQueryPrice = false

  var body: some View {
    VStack(alignment: .leading, spacing: 8) {
      Text(product.name)
        .font(.body)
      if let brands = product.brands {
        Text(brands)
          .font(.caption)
          .foregroundStyle(.secondary)
      }

      purchaseContent

      Button("Remove") {
        viewModel.removeSavedProduct(code: product.code)
      }
      .frame(minHeight: 44)
      .accessibilityLabel("Remove \(product.name) from saved products")
    }
    .padding(.vertical, 4)
    .task {
      await refreshPriceIfNeeded()
    }
  }

  @ViewBuilder
  private var purchaseContent: some View {
    if viewModel.isPurchased(code: product.code) {
      Label("Purchased", systemImage: "checkmark.seal")
        .foregroundStyle(.green)
        .accessibilityLabel("\(product.name) is purchased")
    } else if viewModel.purchaseState(for: product.code) is PurchaseStatePending {
      // While pending the control is disabled so no second purchase can start
      // (FR-018).
      VStack(alignment: .leading, spacing: 4) {
        HStack(spacing: 8) {
          ProgressView()
          Text("Purchase in progress...")
            .foregroundStyle(.secondary)
        }
        Button("Buy") {}
          .disabled(true)
          .frame(minHeight: 44)
      }
    } else if viewModel.purchaseState(for: product.code) is PurchaseStateUnavailable {
      guidanceContent(
        message: "Payments are unavailable right now.",
        accessibilityLabel: "Retry checking payment availability"
      )
    } else if let failed = viewModel.purchaseState(for: product.code) as? PurchaseStateFailed {
      guidanceContent(
        message: reasonMessage(failed.reason),
        accessibilityLabel: "Retry purchase of \(product.name)"
      )
    } else if viewModel.purchaseState(for: product.code) is PurchaseStateCancelled {
      VStack(alignment: .leading, spacing: 4) {
        Text("Purchase cancelled. You have not been charged.")
          .font(.footnote)
          .foregroundStyle(.secondary)
        buyButton
      }
    } else {
      buyButton
    }
  }

  @ViewBuilder
  private var buyButton: some View {
    if let priceLabel {
      Button("Buy for \(priceLabel)") {
        viewModel.purchase(code: product.code, amountLabel: priceLabel)
      }
      .frame(minHeight: 44)
      .accessibilityLabel("Buy \(product.name) for \(priceLabel)")
    } else if didQueryPrice {
      // The store does not offer this product: no purchase action is shown
      // (FR-015).
      EmptyView()
    } else {
      HStack(spacing: 8) {
        ProgressView()
        Text("Checking availability...")
          .font(.footnote)
          .foregroundStyle(.secondary)
      }
    }
  }

  /// Unavailable / not configured shows guidance plus a retry control rather than
  /// failing silently (FR-019).
  private func guidanceContent(message: String, accessibilityLabel: String) -> some View {
    VStack(alignment: .leading, spacing: 4) {
      Text(message)
        .font(.footnote)
        .foregroundStyle(.secondary)
      Button("Retry") {
        Task { await refreshPrice() }
      }
      .frame(minHeight: 44)
      .accessibilityLabel(accessibilityLabel)
    }
  }

  private func reasonMessage(_ reason: PurchaseFailure) -> String {
    if reason == PurchaseFailure.network {
      return "The purchase could not be completed because of a connection problem."
    }
    if reason == PurchaseFailure.notConfigured {
      return "Payments are not configured. Please try again later."
    }
    if reason == PurchaseFailure.verificationFailed {
      return "The purchase could not be verified and was not completed."
    }
    if reason == PurchaseFailure.declined {
      return "The payment was declined. Please try a different payment method."
    }
    return "The purchase could not be completed. Please try again."
  }

  private func refreshPriceIfNeeded() async {
    guard !didQueryPrice else { return }
    await refreshPrice()
  }

  private func refreshPrice() async {
    await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
      viewModel.queryProductPrice(code: product.code) { label in
        priceLabel = label
        didQueryPrice = true
        continuation.resume()
      }
    }
  }
}
