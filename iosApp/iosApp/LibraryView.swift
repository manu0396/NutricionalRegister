import Shared
import SwiftUI

@MainActor
final class LibraryViewModel: ObservableObject {
  @Published private(set) var state: SavedProductsState?

  private let bridge: IosAppBridge
  private var sub: Int64?

  init(bridge: IosAppBridge) {
    self.bridge = bridge
    sub = bridge.observeLibraryState { [weak self] state in
      self?.state = state
    }
  }

  func remove(code: String) { bridge.removeSavedProduct(code: code) }
  func addFood(code: String) {
    // navigation handled by parent
  }

  func openProduct(code: String) {
    // navigation handled by parent
  }

  deinit {
    if let sub { bridge.cancelSubscription(subscriptionId: sub) }
  }
}

struct LibraryView: View {
  let bridge: IosAppBridge
  @EnvironmentObject private var navigator: AppNavigator
  @StateObject private var viewModel: LibraryViewModel

  init(bridge: IosAppBridge) {
    self.bridge = bridge
    _viewModel = StateObject(wrappedValue: LibraryViewModel(bridge: bridge))
  }

  var body: some View {
    List {
      if let state = viewModel.state {
        if state.isLoading {
          ProgressView("Loading saved products...")
        } else if state.saved.isEmpty {
          VStack(alignment: .leading, spacing: 8) {
            Text("No saved products yet")
              .font(.headline)
            Text("Products you save from search will appear here, available offline.")
              .font(.footnote)
              .foregroundStyle(.secondary)
          }
        } else {
          ForEach(state.saved, id: \.code) { product in
            VStack(alignment: .leading, spacing: 4) {
              Text(product.name)
              if let brands = product.brands {
                Text(brands).font(.caption).foregroundStyle(.secondary)
              }
              HStack {
                Button("Open") { navigator.openProduct(product.code) }
                Button("Add") { navigator.openAddFood(productCode: product.code) }
                Button("Remove") { viewModel.remove(code: product.code) }
              }
              .font(.caption)
            }
            .padding(.vertical, 4)
          }
        }
      }
    }
    .navigationTitle("Library")
    .toolbar {
      Button("Find foods") { navigator.openAddFood() }
    }
  }
}
