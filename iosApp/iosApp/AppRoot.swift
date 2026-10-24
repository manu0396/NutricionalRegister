import Shared
import SwiftUI

enum NutriLogTab: Hashable {
  case today
  case library
  case more
}

enum Route: Hashable {
  case today
  case library
  case more
  case addFoodSource(meal: String?, productCode: String?)
  case addFoodQuantity(meal: String?, productCode: String?)
  case productDetail(code: String)
  case weekly
}

@MainActor
final class AppRoot: ObservableObject {
  let bridge: IosAppBridge
  let storeKitClient: StoreKitPurchaseClient

  @Published var selectedTab: NutriLogTab = .today
  @Published var todayPath = NavigationPath()
  @Published var libraryPath = NavigationPath()
  @Published var morePath = NavigationPath()

  @Published var showCoach = false
  @Published var showPrivacyPolicy = false

  init() {
    self.storeKitClient = StoreKitPurchaseClient(productIdsByCode: ["nutrilog_pro": "nutrilog_pro"])
    let rawAiGatewayUrl =
      Bundle.main.object(forInfoDictionaryKey: "NUTRILOG_AI_PROXY_URL") as? String ?? ""
    let aiGatewayUrl = rawAiGatewayUrl.hasPrefix("https://") ? rawAiGatewayUrl : ""
    self.bridge = IosAppBridgeKt.createIosAppBridge(
      purchaseClient: storeKitClient, aiGatewayBaseUrl: aiGatewayUrl)
  }

  deinit {
    bridge.close()
  }

}

@MainActor
final class AppNavigator: ObservableObject {
  @Published var selectedTab: NutriLogTab = .today
  @Published var todayPath = NavigationPath()
  @Published var libraryPath = NavigationPath()
  @Published var morePath = NavigationPath()

  func switchTo(_ tab: NutriLogTab) {
    selectedTab = tab
  }

  func openAddFood(meal: String? = nil, productCode: String? = nil) {
    let route: Route
    if let code = productCode, !code.isEmpty {
      route = .addFoodQuantity(meal: meal, productCode: code)
    } else {
      route = .addFoodSource(meal: meal, productCode: productCode)
    }
    switch selectedTab {
    case .today:
      todayPath.append(route)
    case .library:
      libraryPath.append(route)
    case .more:
      morePath.append(route)
    }
  }

  func openProduct(_ code: String) {
    let route = Route.productDetail(code: code)
    switch selectedTab {
    case .today:
      todayPath.append(route)
    case .library:
      libraryPath.append(route)
    case .more:
      morePath.append(route)
    }
  }

  func openWeekly() {
    switch selectedTab {
    case .today:
      todayPath.append(Route.weekly)
    case .library:
      libraryPath.append(Route.weekly)
    case .more:
      morePath.append(Route.weekly)
    }
  }

  func back() {
    switch selectedTab {
    case .today:
      if !todayPath.isEmpty { todayPath.removeLast() }
    case .library:
      if !libraryPath.isEmpty { libraryPath.removeLast() }
    case .more:
      if !morePath.isEmpty { morePath.removeLast() }
    }
  }
}
