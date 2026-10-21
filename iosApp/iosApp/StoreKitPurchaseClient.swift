import Foundation
import Shared
import StoreKit

/// The iOS-side implementation of the shared `PurchaseClient` contract.
///
/// StoreKit 2 is a pure-Swift API that Kotlin/Native cannot bind through
/// cinterop, so the purchase interaction lives here in Swift and is injected into
/// the shared runtime (see `createIosNutriLogBridge` and ADR-0013). The shared
/// code depends only on the exported `PurchaseClient` protocol, and this class is
/// the only place that talks to StoreKit.
///
/// Only a **verified** transaction grants a purchase: `.success(.verified)` is
/// mapped to `PurchaseClientResult.Verified` and finished; `.unverified` maps to
/// `Failed(VERIFICATION_FAILED)` and is never granted. No card or credential data
/// is ever read here; all payment entry happens in the platform sheet (FR-014).
final class StoreKitPurchaseClient: NSObject, PurchaseClient {
  private let productIdsByCode: [String: String]
  private let fallbackProductIdPrefix: String

  /// - Parameters:
  ///   - productIdsByCode: optional explicit map from a catalogue product code to
  ///     the App Store product identifier. When absent, the identifier is derived
  ///     from `fallbackProductIdPrefix` plus the product code.
  ///   - fallbackProductIdPrefix: App Store product identifier prefix used when no
  ///     explicit mapping is supplied.
  init(
    productIdsByCode: [String: String] = [:],
    fallbackProductIdPrefix: String = "com.example.nutricionalregister.product."
  ) {
    self.productIdsByCode = productIdsByCode
    self.fallbackProductIdPrefix = fallbackProductIdPrefix
    super.init()
  }

  // MARK: - PurchaseClient (completion-handler bridge over suspend functions)

  func queryProduct(
    code: String,
    completionHandler: @escaping (PurchasableProduct?, Error?) -> Void
  ) {
    Task {
      do {
        guard let product = try await loadProduct(code: code) else {
          completionHandler(nil, nil)
          return
        }
        completionHandler(
          PurchasableProduct(code: code, amountLabel: product.displayPrice),
          nil
        )
      } catch let error as StoreKitError {
        completionHandler(nil, self.toNSError(self.mapStoreKitError(error)))
      } catch {
        completionHandler(nil, self.toNSError(.notConfigured))
      }
    }
  }

  func launchPurchase(
    code: String,
    completionHandler: @escaping (PurchaseClientResult?, Error?) -> Void
  ) {
    Task {
      do {
        guard let product = try await loadProduct(code: code) else {
          completionHandler(PurchaseClientResultUnavailable.shared, nil)
          return
        }
        let result = try await product.purchase()
        switch result {
        case .success(let verification):
          switch verification {
          case .verified(let transaction):
            await transaction.finish()
            let verified = VerifiedTransaction(
              productCode: code,
              transactionId: String(transaction.id),
              purchaseToken: verification.jwsRepresentation,
              completedAtEpochMillis: Self.epochMillis(transaction.purchaseDate)
            )
            completionHandler(PurchaseClientResultVerified(transaction: verified), nil)
          case .unverified:
            completionHandler(
              PurchaseClientResultFailed(reason: PurchaseFailure.verificationFailed),
              nil
            )
          }
        case .userCancelled:
          completionHandler(PurchaseClientResultUserCancelled.shared, nil)
        case .pending:
          // The payment is deferred (e.g. Ask to Buy); treat as unavailable until
          // the transaction arrives through currentEntitlements on reconcile.
          completionHandler(PurchaseClientResultUnavailable.shared, nil)
        @unknown default:
          completionHandler(
            PurchaseClientResultFailed(reason: PurchaseFailure.unknown),
            nil
          )
        }
      } catch let error as StoreKitError {
        completionHandler(
          PurchaseClientResultFailed(reason: self.mapStoreKitError(error)),
          nil
        )
      } catch {
        completionHandler(
          PurchaseClientResultFailed(reason: PurchaseFailure.notConfigured),
          nil
        )
      }
    }
  }

  func currentEntitlements(
    completionHandler: @escaping ([VerifiedTransaction]?, Error?) -> Void
  ) {
    Task {
      var transactions: [VerifiedTransaction] = []
      for await result in Transaction.currentEntitlements {
        guard case .verified(let transaction) = result else { continue }
        transactions.append(
          VerifiedTransaction(
            productCode: self.codeForProductId(transaction.productID),
            transactionId: String(transaction.id),
            purchaseToken: nil,
            completedAtEpochMillis: Self.epochMillis(transaction.purchaseDate)
          )
        )
      }
      completionHandler(transactions, nil)
    }
  }

  // MARK: - Restore

  /// User-triggered restore required by the App Store guideline; reconciles the
  /// device with the App Store, after which
  /// `PurchaseRepository.reconcile()` records any verified entitlement.
  func restore(onComplete: @escaping (Bool) -> Void) {
    Task {
      do {
        try await AppStore.sync()
        onComplete(true)
      } catch {
        onComplete(false)
      }
    }
  }

  // MARK: - Helpers

  private func loadProduct(code: String) async throws -> StoreKit.Product? {
    let productId = productIdsByCode[code] ?? (fallbackProductIdPrefix + code)
    let products = try await StoreKit.Product.products(for: [productId])
    return products.first
  }

  private func codeForProductId(_ productId: String) -> String {
    if let match = productIdsByCode.first(where: { $0.value == productId }) {
      return match.key
    }
    if productId.hasPrefix(fallbackProductIdPrefix) {
      return String(productId.dropFirst(fallbackProductIdPrefix.count))
    }
    return productId
  }

  private func mapStoreKitError(_ error: StoreKitError) -> PurchaseFailure {
    switch error {
    case .userCancelled:
      return PurchaseFailure.userCancelled
    case .networkError:
      return PurchaseFailure.network
    case .systemError, .notAvailableInStorefront, .notEntitled, .unsupported, .unknown:
      return PurchaseFailure.notConfigured
    @unknown default:
      return PurchaseFailure.unknown
    }
  }

  private func toNSError(_ failure: PurchaseFailure) -> NSError {
    NSError(
      domain: "StoreKitPurchaseClient",
      code: 1,
      userInfo: [NSLocalizedDescriptionKey: failure.name]
    )
  }

  private static func epochMillis(_ date: Date) -> Int64 {
    Int64((date.timeIntervalSince1970 * 1000.0).rounded())
  }
}
