import Shared
import SwiftUI

struct ContentView: View {
  @EnvironmentObject private var appRoot: AppRoot
  @EnvironmentObject private var navigator: AppNavigator

  var body: some View {
    TabView(selection: $navigator.selectedTab) {
      NavigationStack(path: $navigator.todayPath) {
        TodayView(bridge: appRoot.bridge)
          .navigationDestination(for: Route.self) { route in
            routeView(route)
          }
      }
      .tabItem { Label("Today", systemImage: "calendar") }
      .tag(NutriLogTab.today)

      NavigationStack(path: $navigator.libraryPath) {
        LibraryView(bridge: appRoot.bridge)
          .navigationDestination(for: Route.self) { route in
            routeView(route)
          }
      }
      .tabItem { Label("Library", systemImage: "book") }
      .tag(NutriLogTab.library)

      NavigationStack(path: $navigator.morePath) {
        MoreView(bridge: appRoot.bridge)
          .navigationDestination(for: Route.self) { route in
            routeView(route)
          }
      }
      .tabItem { Label("More", systemImage: "ellipsis") }
      .tag(NutriLogTab.more)
    }
    .overlay(alignment: .bottomTrailing) {
      VStack(spacing: 12) {
        if navigator.selectedTab == .today || navigator.selectedTab == .library {
          Button {
            navigator.openAddFood()
          } label: {
            Image(systemName: "plus")
              .font(.title2)
              .padding(12)
              .background(.tint, in: Circle())
              .foregroundStyle(.white)
          }
          .accessibilityLabel("Add food")
        }
        Button {
          appRoot.showCoach = true
        } label: {
          Image(systemName: "face.smiling")
            .font(.title2)
            .padding(12)
            .background(.secondary, in: Circle())
            .foregroundStyle(.white)
        }
        .accessibilityLabel("Ask the Coach")
      }
      .padding(.bottom, 90)
      .padding(.trailing, 16)
    }
    .sheet(isPresented: $appRoot.showCoach) {
      CoachView(bridge: appRoot.bridge)
        .environmentObject(appRoot)
    }
    .alert("Privacy", isPresented: $appRoot.showPrivacyPolicy) {
      Button("Close", role: .cancel) {}
    } message: {
      Text(
        "Your diary, your goals and your library stay on this device. Only the assistant sends anything anywhere, and only the minimum it needs."
      )
    }
  }

  @ViewBuilder
  private func routeView(_ route: Route) -> some View {
    switch route {
    case .addFoodSource(let meal, let productCode):
      AddFoodSourceView(meal: meal, productCode: productCode, bridge: appRoot.bridge)
        .environmentObject(appRoot)
        .environmentObject(navigator)
    case .addFoodQuantity(let meal, let productCode):
      AddFoodQuantityView(meal: meal, productCode: productCode, bridge: appRoot.bridge)
        .environmentObject(appRoot)
        .environmentObject(navigator)
    case .productDetail(let code):
      ProductDetailView(code: code, bridge: appRoot.bridge)
        .environmentObject(appRoot)
        .environmentObject(navigator)
    case .weekly:
      WeeklyView(bridge: appRoot.bridge)
        .environmentObject(appRoot)
        .environmentObject(navigator)
    default:
      EmptyView()
    }
  }
}
