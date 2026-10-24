import SwiftUI

@main
struct NutriLogApp: App {
  @StateObject private var appRoot = AppRoot()
  @StateObject private var navigator = AppNavigator()

  var body: some Scene {
    WindowGroup {
      ContentView()
        .environmentObject(appRoot)
        .environmentObject(navigator)
    }
  }
}
