import AVFoundation
import SwiftUI
import UIKit

/// A native SwiftUI wrapper for AVFoundation metadata barcode scanning.
/// Supports common grocery codes (EAN-13, EAN-8, UPC-A, UPC-E where supported, Code 128).
/// Designed for parity with native iOS camera scanning without third-party dependencies.
struct BarcodeScannerView: View {
  /// Callback invoked when a barcode is successfully scanned.
  let onBarcodeScanned: (String) -> Void

  /// Optional binding to control scanning state. When set to `false`, scanning stops.
  @Binding var isScanning: Bool

  /// Whether to stop scanning after the first successful detection. Defaults to `true`.
  let stopAfterFirstDetection: Bool

  /// Optional callback for permission/state changes.
  let onStatusChange: ((BarcodeScannerStatus) -> Void)?

  @StateObject private var viewModel = BarcodeScannerViewModel()
  @State private var hasDeliveredScan = false

  init(
    onBarcodeScanned: @escaping (String) -> Void,
    isScanning: Binding<Bool> = .constant(true),
    stopAfterFirstDetection: Bool = true,
    onStatusChange: ((BarcodeScannerStatus) -> Void)? = nil
  ) {
    self.onBarcodeScanned = onBarcodeScanned
    self._isScanning = isScanning
    self.stopAfterFirstDetection = stopAfterFirstDetection
    self.onStatusChange = onStatusChange
  }

  var body: some View {
    ZStack {
      if viewModel.shouldShowCamera {
        BarcodeScannerRepresentable(
          viewModel: viewModel,
          onBarcodeScanned: { code in
            guard !hasDeliveredScan else { return }
            if stopAfterFirstDetection {
              hasDeliveredScan = true
              DispatchQueue.main.async {
                isScanning = false
              }
            }
            onBarcodeScanned(code)
          }
        )
        .accessibilityLabel("Barcode scanner camera view")
      } else {
        fallbackContent
      }

      overlayContent
    }
    .onAppear {
      viewModel.configureIfNeeded()
      viewModel.onStatusChange = { status in
        onStatusChange?(status)
      }
      updateScanningState()
    }
    .onChange(of: isScanning) { _ in
      updateScanningState()
    }
    .onDisappear {
      viewModel.stopSession()
    }
    .onChange(of: viewModel.status) { status in
      onStatusChange?(status)
    }
    .accessibilityElement(children: .contain)
  }

  private func updateScanningState() {
    if isScanning && viewModel.shouldShowCamera && !hasDeliveredScan {
      viewModel.startSession()
    } else {
      viewModel.stopSession()
    }
  }

  @ViewBuilder
  private var fallbackContent: some View {
    VStack(spacing: 16) {
      Image(systemName: "barcode.viewfinder")
        .font(.system(size: 48))
        .foregroundStyle(.secondary)
        .accessibilityHidden(true)

      Text(fallbackTitle)
        .font(.headline)
        .multilineTextAlignment(.center)

      Text(fallbackMessage)
        .font(.body)
        .foregroundStyle(.secondary)
        .multilineTextAlignment(.center)
        .padding(.horizontal)
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(Color(.systemBackground))
    .accessibilityElement(children: .combine)
  }

  private var fallbackTitle: String {
    switch viewModel.status {
    case .denied:
      return "Camera Access Denied"
    case .unavailable:
      return "Camera Unavailable"
    case .error:
      return "Scanner Error"
    case .loading, .authorized, .scanning, .idle:
      return "Camera Unavailable"
    }
  }

  private var fallbackMessage: String {
    switch viewModel.status {
    case .denied:
      return "Please enable camera access in Settings to scan barcodes."
    case .unavailable:
      return "Camera is not available on this device. Simulator does not have a camera."
    case .error(let message):
      return message
    case .loading:
      return "Checking camera availability..."
    case .authorized, .scanning, .idle:
      return "Camera is not available on this device."
    }
  }

  @ViewBuilder
  private var overlayContent: some View {
    if viewModel.status == .loading {
      VStack {
        ProgressView()
          .progressViewStyle(.circular)
        Text("Preparing scanner...")
          .font(.footnote)
          .foregroundStyle(.secondary)
          .padding(.top, 8)
      }
      .padding()
      .background(.ultraThinMaterial)
      .clipShape(RoundedRectangle(cornerRadius: 12))
      .accessibilityElement(children: .combine)
      .accessibilityLabel("Preparing barcode scanner")
    }
  }
}

/// Status states for the barcode scanner.
enum BarcodeScannerStatus: Equatable {
  case idle
  case loading
  case authorized
  case scanning
  case denied
  case unavailable
  case error(String)
}

/// View model managing AVFoundation capture session lifecycle.
@MainActor
final class BarcodeScannerViewModel: NSObject, ObservableObject {
  @Published var status: BarcodeScannerStatus = .idle

  var shouldShowCamera: Bool {
    switch status {
    case .authorized, .scanning:
      return true
    case .idle, .loading, .denied, .unavailable, .error:
      return false
    }
  }

  var onStatusChange: ((BarcodeScannerStatus) -> Void)?

  let session = AVCaptureSession()
  private var videoPreviewLayer: AVCaptureVideoPreviewLayer?
  private let sessionQueue = DispatchQueue(
    label: "com.nutricionalregister.barcodescanner.session", qos: .userInitiated)
  private var isConfigured = false

  func configureIfNeeded() {
    guard !isConfigured else { return }
    isConfigured = true
    status = .loading
    onStatusChange?(status)

    sessionQueue.async { [weak self] in
      guard let self else { return }
      self.checkPermissionsAndConfigure()
    }
  }

  private func checkPermissionsAndConfigure() {
    switch AVCaptureDevice.authorizationStatus(for: .video) {
    case .authorized:
      DispatchQueue.main.async {
        self.configureSession()
      }
    case .notDetermined:
      AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
        guard let self else { return }
        if granted {
          DispatchQueue.main.async {
            self.configureSession()
          }
        } else {
          DispatchQueue.main.async {
            self.status = .denied
            self.onStatusChange?(self.status)
          }
        }
      }
    case .denied, .restricted:
      DispatchQueue.main.async {
        self.status = .denied
        self.onStatusChange?(self.status)
      }
    @unknown default:
      DispatchQueue.main.async {
        self.status = .unavailable
        self.onStatusChange?(self.status)
      }
    }
  }

  private func configureSession() {
    session.beginConfiguration()

    guard let videoDevice = AVCaptureDevice.default(for: .video) else {
      session.commitConfiguration()
      status = .unavailable
      onStatusChange?(status)
      return
    }

    do {
      let videoInput = try AVCaptureDeviceInput(device: videoDevice)
      if session.canAddInput(videoInput) {
        session.addInput(videoInput)
      } else {
        session.commitConfiguration()
        status = .unavailable
        onStatusChange?(status)
        return
      }
    } catch {
      session.commitConfiguration()
      status = .error("Failed to configure camera input.")
      onStatusChange?(status)
      return
    }

    let metadataOutput = AVCaptureMetadataOutput()
    if session.canAddOutput(metadataOutput) {
      session.addOutput(metadataOutput)
      metadataOutput.setMetadataObjectsDelegate(self, queue: sessionQueue)
      let types: [AVMetadataObject.ObjectType] = [
        .ean13,
        .ean8,
        .upce,
        .code128,
      ]
      let availableTypes = types.filter { metadataOutput.availableMetadataObjectTypes.contains($0) }
      if availableTypes.isEmpty {
        metadataOutput.metadataObjectTypes = types
      } else {
        metadataOutput.metadataObjectTypes = availableTypes
      }
    } else {
      session.commitConfiguration()
      status = .error("Failed to configure barcode output.")
      onStatusChange?(status)
      return
    }

    session.commitConfiguration()
    status = .authorized
    onStatusChange?(status)
  }

  func startSession() {
    guard status == .authorized || status == .scanning else { return }
    if session.isRunning { return }
    sessionQueue.async { [weak self] in
      guard let self else { return }
      self.session.startRunning()
      DispatchQueue.main.async {
        if self.session.isRunning {
          self.status = .scanning
          self.onStatusChange?(self.status)
        }
      }
    }
  }

  func stopSession() {
    if !session.isRunning { return }
    sessionQueue.async { [weak self] in
      guard let self else { return }
      self.session.stopRunning()
      DispatchQueue.main.async {
        if self.status == .scanning {
          self.status = .authorized
          self.onStatusChange?(self.status)
        }
      }
    }
  }
}

extension BarcodeScannerViewModel: AVCaptureMetadataOutputObjectsDelegate {
  func metadataOutput(
    _ output: AVCaptureMetadataOutput,
    didOutput metadataObjects: [AVMetadataObject],
    from connection: AVCaptureConnection
  ) {
    for object in metadataObjects {
      guard let readable = object as? AVMetadataMachineReadableCodeObject,
        let stringValue = readable.stringValue,
        !stringValue.isEmpty
      else {
        continue
      }
      // Deliver on main thread to avoid UI thread violations
      DispatchQueue.main.async { [weak self] in
        guard let self else { return }
        // Find the representable to deliver callback
        NotificationCenter.default.post(
          name: .barcodeScanned,
          object: nil,
          userInfo: ["code": stringValue]
        )
      }
      break
    }
  }
}

extension Notification.Name {
  static let barcodeScanned = Notification.Name("BarcodeScannerView.barcodeScanned")
}

/// UIViewRepresentable that hosts the AVCaptureVideoPreviewLayer.
struct BarcodeScannerRepresentable: UIViewRepresentable {
  @ObservedObject var viewModel: BarcodeScannerViewModel
  let onBarcodeScanned: (String) -> Void

  func makeUIView(context: Context) -> ScannerPreviewView {
    let view = ScannerPreviewView()
    view.session = viewModel.session
    context.coordinator.onBarcodeScanned = onBarcodeScanned
    return view
  }

  func updateUIView(_ uiView: ScannerPreviewView, context: Context) {
    uiView.session = viewModel.session
  }

  func makeCoordinator() -> Coordinator {
    Coordinator(onBarcodeScanned: onBarcodeScanned)
  }

  final class Coordinator: NSObject {
    var onBarcodeScanned: ((String) -> Void)?
    private var observer: NSObjectProtocol?

    init(onBarcodeScanned: @escaping (String) -> Void) {
      self.onBarcodeScanned = onBarcodeScanned
      super.init()
      observer = NotificationCenter.default.addObserver(
        forName: .barcodeScanned,
        object: nil,
        queue: .main
      ) { [weak self] notification in
        guard let self, let code = notification.userInfo?["code"] as? String else { return }
        self.onBarcodeScanned?(code)
      }
    }

    deinit {
      if let observer {
        NotificationCenter.default.removeObserver(observer)
      }
    }
  }
}

/// UIView subclass that sets up the preview layer.
final class ScannerPreviewView: UIView {
  var session: AVCaptureSession? {
    didSet {
      if let session {
        previewLayer.session = session
      }
    }
  }

  override class var layerClass: AnyClass {
    AVCaptureVideoPreviewLayer.self
  }

  var previewLayer: AVCaptureVideoPreviewLayer {
    guard let layer = layer as? AVCaptureVideoPreviewLayer else {
      return AVCaptureVideoPreviewLayer()
    }
    return layer
  }

  override init(frame: CGRect) {
    super.init(frame: frame)
    previewLayer.videoGravity = .resizeAspectFill
    backgroundColor = .black
    isAccessibilityElement = true
    accessibilityLabel = "Camera preview for barcode scanning"
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }
}
