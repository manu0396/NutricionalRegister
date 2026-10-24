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

  init(bridge: IosAppBridge) {
    self.bridge = bridge
    _viewModel = StateObject(wrappedValue: CoachViewModel(bridge: bridge))
  }

  var body: some View {
    NavigationStack {
      VStack {
        if let state = viewModel.state {
          Text("Status: \\(String(describing: state.availability))")
            .font(.caption)
            .foregroundStyle(.secondary)
          Text("Configured: \\(appRoot.bridge.isAssistantConfigured)")
            .font(.caption)
            .foregroundStyle(.secondary)
          availabilityView(state)
          ScrollView {
            VStack(alignment: .leading, spacing: 8) {
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
            }
            .padding()
          }
          if state.isSending {
            ProgressView()
          }
          if let state = viewModel.state, state.availability is AssistantAvailabilityReady {
            HStack {
              TextField("Message", text: $input)
              Button("Send") {
                viewModel.send(input)
                input = ""
              }
            }
            .padding()
          } else if let state = viewModel.state, let error = state.error {
            Text(error)
              .foregroundStyle(.secondary)
              .padding()
          }
        } else {
          ProgressView()
        }
      }
      .navigationTitle("Coach")
      .toolbar {
        Button("Close") { appRoot.showCoach = false }
      }
    }
  }

  @ViewBuilder
  private func availabilityView(_ state: AssistantState) -> some View {
    if state.availability is AssistantAvailabilityNotConfigured {
      Text("Assistant is not configured for this build.")
        .foregroundStyle(.secondary)
        .padding()
    } else if state.availability is AssistantAvailabilityConsentRequired {
      VStack {
        Text("Consent required before sending messages.")
        Button("Grant consent") { viewModel.grantConsent() }
      }
      .padding()
    } else if state.availability is AssistantAvailabilityOffline {
      Text("Assistant needs a connection.")
        .foregroundStyle(.secondary)
        .padding()
    }
  }
}
