import Shared
import SwiftUI

@MainActor
final class ProductDetailViewModel: ObservableObject {
  @Published private(set) var state: ProductDetailState?

  private let bridge: IosAppBridge
  private let handle: ProductDetailHandle
  private var sub: Int64?

  init(bridge: IosAppBridge, code: String) {
    self.bridge = bridge
    self.handle = bridge.createProductDetailHandle(code: code)
    sub = handle.observeState { [weak self] state in
      self?.state = state
    }
    handle.load(code: code)
  }

  func save(product: Shared.Product) {
    bridge.saveProduct(product: product)
  }

  func remove(code: String) {
    bridge.removeSavedProduct(code: code)
  }

  deinit {
    if let sub { handle.cancelSubscription(subscriptionId: sub) }
    handle.clear()
  }
}

struct ProductDetailView: View {
  let code: String
  let bridge: IosAppBridge
  @EnvironmentObject private var navigator: AppNavigator
  @StateObject private var viewModel: ProductDetailViewModel

  init(code: String, bridge: IosAppBridge) {
    self.code = code
    self.bridge = bridge
    _viewModel = StateObject(wrappedValue: ProductDetailViewModel(bridge: bridge, code: code))
  }

  var body: some View {
    ScrollView {
      VStack(alignment: .leading, spacing: 12) {
        if let state = viewModel.state {
          if state is ProductDetailStateLoading {
            ProgressView()
          } else if state is ProductDetailStateNotFound {
            Text("Product not found")
          } else if let ready = state as? ProductDetailStateReady {
            let product = ready.product
            Text(product.name).font(.title2.bold())
            if let brands = product.brands {
              Text(brands).foregroundStyle(.secondary)
            }
            if let score = product.nutriScoreGrade {
              Text("Nutri-Score: \(score)")
            }
            Text("Nutrition entries: \(product.nutritionValues.count)")
            HStack {
              Button("Save") { viewModel.save(product: product) }
              Button("Remove") { viewModel.remove(code: product.code) }
              Button("Add to diary") { navigator.openAddFood(productCode: product.code) }
            }
          } else {
            ProgressView()
          }
        } else {
          ProgressView()
        }
      }
      .padding()
    }
    .navigationTitle("Product")
    .toolbar {
      Button("Back") { navigator.back() }
    }
  }
}
