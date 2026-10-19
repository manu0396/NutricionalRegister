import Shared
import SwiftUI

struct ContentView: View {
  var body: some View {
    VStack {
      Text("NutriLog")
      Text(Greeting().greet())
    }
  }
}
