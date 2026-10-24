import Shared
import SwiftUI

@MainActor
final class AddFoodFlowViewModel: ObservableObject {
  @Published private(set) var addFoodState: AddFoodState?
  @Published private(set) var searchState: ProductSearchState?
  @Published private(set) var libraryState: SavedProductsState?

  private let bridge: IosAppBridge
  private var subs: [Int64] = []
  private var sessionId: String = UUID().uuidString

  init(bridge: IosAppBridge) {
    self.bridge = bridge
    subs.append(
      bridge.observeAddFoodState { [weak self] state in
        self?.addFoodState = state
      })
    subs.append(
      bridge.observeSearchState { [weak self] state in
        self?.searchState = state
      })
    subs.append(
      bridge.observeLibraryState { [weak self] state in
        self?.libraryState = state
      })
  }

  func start(meal: String? = nil, productCode: String? = nil) {
    sessionId = UUID().uuidString
    bridge.startAddFood(sessionId: sessionId, meal: meal, productCode: productCode)
  }

  func selectSource(_ source: String) {
    bridge.selectAddFoodSource(source: source)
    if source == "SEARCH" {
      bridge.submitSearch()
    }
  }

  func setQuery(_ query: String) {
    bridge.setAddFoodQuery(query: query)
  }

  func submitSearch() {
    bridge.submitSearch()
  }

  func selectProduct(_ product: Shared.Product) {
    bridge.selectAddFoodProduct(product: product)
  }

  func setQuantity(_ grams: Double) {
    bridge.setAddFoodQuantity(grams: grams)
  }

  func openMealSheet() { bridge.openAddFoodMealSheet() }
  func closeMealSheet() { bridge.closeAddFoodMealSheet() }
  func selectMeal(_ meal: String) { bridge.selectAddFoodMeal(meal: meal) }

  func scanBarcode(_ code: String) {
    bridge.resolveBarcode(code: code) { [weak self] resolution in
      guard let self else { return }
      if let resolved = resolution as? BarcodeResolutionResolved {
        let product = resolved.product
        self.bridge.selectAddFoodProduct(product: product)
      } else if let notInCache = resolution as? BarcodeResolutionNotInCache {
        let code = notInCache.code
        self.bridge.selectAddFoodSource(source: "SEARCH")
        self.bridge.setAddFoodQuery(query: code)
        self.bridge.submitSearch()
      }
    }
  }

  func confirm() {
    bridge.confirmAddFood()
  }

  deinit {
    for sub in subs { bridge.cancelSubscription(subscriptionId: sub) }
  }
}

struct AddFoodSourceView: View {
  let meal: String?
  let productCode: String?
  let bridge: IosAppBridge
  @EnvironmentObject private var navigator: AppNavigator
  @StateObject private var viewModel: AddFoodFlowViewModel

  @State private var query: String = ""
  @State private var isScanning: Bool = false
  @State private var selectedSource: Shared.AddFoodSource = .search

  init(meal: String?, productCode: String?, bridge: IosAppBridge) {
    self.meal = meal
    self.productCode = productCode
    self.bridge = bridge
    _viewModel = StateObject(wrappedValue: AddFoodFlowViewModel(bridge: bridge))
  }

  var body: some View {
    VStack(spacing: 12) {
      if let state = viewModel.addFoodState {
        Picker("Source", selection: $selectedSource) {
          Text("Search").tag(Shared.AddFoodSource.search)
          Text("Library").tag(Shared.AddFoodSource.library)
          Text("Scan").tag(Shared.AddFoodSource.scan)
        }
        .pickerStyle(.segmented)
        .onChange(of: selectedSource) { newValue in
          viewModel.selectSource(newValue.name)
        }
        .onChange(of: state.source) { newValue in
          selectedSource = newValue
        }

        if state.source == .search {
          HStack {
            TextField("Search foods", text: $query)
              .textFieldStyle(.roundedBorder)
            Button("Search") {
              viewModel.setQuery(query)
              viewModel.submitSearch()
            }
          }
          Text("Results: \((viewModel.searchState?.products.count ?? 0))")
          if let recent = viewModel.searchState?.recentSearches, !recent.isEmpty {
            Section("Recent") {
              ForEach(recent, id: \.normalizedTerm) { item in
                Button(item.displayTerm) {
                  viewModel.setQuery(item.displayTerm)
                  viewModel.submitSearch()
                }
              }
            }
          }
        } else if state.source == .library {
          List(viewModel.libraryState?.saved ?? [], id: \.code) { saved in
            Button(saved.name) {
              // Need product; open quantity with code
              navigator.openAddFood(meal: meal, productCode: saved.code)
            }
          }
        } else {
          BarcodeScannerView(
            onBarcodeScanned: { code in
              viewModel.scanBarcode(code)
            }, isScanning: $isScanning
          )
          .frame(height: 300)
        }
      }
    }
    .padding()
    .navigationTitle("Add food")
    .onAppear {
      viewModel.start(meal: meal, productCode: productCode)
    }
  }
}

struct AddFoodQuantityView: View {
  let meal: String?
  let productCode: String?
  let bridge: IosAppBridge
  @EnvironmentObject private var navigator: AppNavigator
  @StateObject private var viewModel: AddFoodFlowViewModel

  @State private var gramsText: String = ""

  init(meal: String?, productCode: String?, bridge: IosAppBridge) {
    self.meal = meal
    self.productCode = productCode
    self.bridge = bridge
    _viewModel = StateObject(wrappedValue: AddFoodFlowViewModel(bridge: bridge))
  }

  var body: some View {
    VStack(spacing: 12) {
      if let state = viewModel.addFoodState {
        if let product = state.product {
          Text(product.name).font(.headline)
        }
        TextField("Grams", text: $gramsText)
          .keyboardType(.decimalPad)
          .textFieldStyle(.roundedBorder)
          .onChange(of: gramsText) { newValue in
            if let value = Double(newValue) {
              viewModel.setQuantity(value)
            }
          }
        Text("Calories: \(Int(state.preview.energyKcal ?? 0)) kcal")
        Picker(
          "Meal",
          selection: Binding(
            get: { state.mealType?.name ?? "" },
            set: { newValue in
              viewModel.selectMeal(newValue)
            })
        ) {
          ForEach(MealType.allCases, id: \.self) { meal in
            Text(meal.displayName).tag(meal.name)
          }
        }
        Button("Confirm") {
          viewModel.confirm()
          navigator.back()
        }
        .disabled(!(viewModel.addFoodState?.canConfirm ?? false))
      }
    }
    .padding()
    .navigationTitle("Quantity")
    .onAppear {
      viewModel.start(meal: meal, productCode: productCode)
    }
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
}
