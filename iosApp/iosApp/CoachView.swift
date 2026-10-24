import Shared
import SwiftUI

@MainActor
final class CoachViewModel: ObservableObject {
  @Published private(set) var state: AssistantState?

  private let bridge: IosAppBridge
  private var sub: Int64?

  init(bridge: IosAppBridge) {
    self.bridge = bridge
    sub = bridge.observeCoachState { [weak self] state in
      self?.state = state
    }
  }

  func send(_ text: String) { bridge.sendCoachMessage(text: text) }
  func grantConsent() { bridge.grantCoachConsent() }
  func confirmProposal() { bridge.confirmCoachProposal() }
  func dismissProposal() { bridge.dismissCoachProposal() }

  deinit {
    if let sub { bridge.cancelSubscription(subscriptionId: sub) }
  }
}

struct CoachView: View {
  let bridge: IosAppBridge
  @EnvironmentObject private var appRoot: AppRoot
  @StateObject private var viewModel: CoachViewModel
  @State private var input: String = ""
  @FocusState private var isInputFocused: Bool

  init(bridge: IosAppBridge) {
    self.bridge = bridge
    _viewModel = StateObject(wrappedValue: CoachViewModel(bridge: bridge))
  }

  var body: some View {
    VStack {
      ScrollView {
        VStack(alignment: .leading, spacing: 8) {
          if let state = viewModel.state {
            if state.availability is AssistantAvailabilityNotConfigured {
              Text("Assistant is not configured for this build.")
                .foregroundStyle(.secondary)
            } else if state.availability is AssistantAvailabilityConsentRequired {
              Text("Consent required before sending messages.")
                .foregroundStyle(.secondary)
              Button("Grant consent") { viewModel.grantConsent() }
            } else if state.availability is AssistantAvailabilityOffline {
              Text("Assistant needs a connection.")
                .foregroundStyle(.secondary)
            }
            ForEach(Array(state.messages.enumerated()), id: \.offset) { _, message in
              Text(message.text)
                .padding(8)
                .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 8))
            }
            if let proposal = state.pendingProposal {
              VStack(alignment: .leading, spacing: 4) {
                Text("Proposed action")
                  .font(.headline)
                Text(proposal.text)
                HStack {
                  Button("Confirm") { viewModel.confirmProposal() }
                  Button("Dismiss") { viewModel.dismissProposal() }
                }
              }
              .padding()
              .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 12))
            }
            if state.isSending {
              ProgressView()
            }
            if let error = state.error {
              Text(error)
                .foregroundStyle(.secondary)
            }
          } else {
            ProgressView()
          }
        }
        .padding()
      }

      HStack {
        TextField("Message", text: $input)
          .textFieldStyle(.roundedBorder)
          .focused($isInputFocused)
          .submitLabel(.send)
        Button("Send") {
          viewModel.send(input)
          input = ""
        }
      }
      .padding()
      .task {
        try? await Task.sleep(nanoseconds: 300_000_000)
        isInputFocused = true
      }
    }
    .padding()
  }
}
