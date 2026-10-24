# iOS Parity Architecture Design

**Objective:** Bring the stale native SwiftUI iOS app to functional parity with the current Compose Android app, without duplicating business logic in Swift. Swift should only render state and forward actions through a shared bridge. 

**Author:** Architect Agent  
**Date:** 2026-10-24  
**Status:** Design Proposal (No Files Modified)

---

## Table of Contents

1. [Goals & Constraints](#goals--constraints)
2. [Existing Architecture to Preserve](#existing-architecture-to-preserve)
3. [High-Level Architecture](#high-level-architecture)
4. [Bridge API Design](#bridge-api-design)
5. [SwiftUI State & View Models](#swiftui-state--view-models)
6. [Navigation Design](#navigation-design)
7. [Lifecycle & One-Runtime Ownership](#lifecycle--one-runtime-ownership)
8. [Feature-Specific Designs](#feature-specific-designs)
9. [Platform Integrations](#platform-integrations)
10. [Kotlin/Native & Swift Interop Caveats](#kotlinnative--swift-interop-caveats)
11. [Component/File Split for Parallel Agents](#componentfile-split-for-parallel-agents)
12. [High-Risk APIs](#high-risk-apis)
13. [Test Strategy](#test-strategy)
14. [Implementation Checklist](#implementation-checklist)
15. [References](#references)

---

## Goals & Constraints

### Goals
- Achieve functional parity with Android app (Today/Diary, Library, More, Add Food flow, Product Detail, Weekly Summary, Coach/Assistant, Premium, Barcode Scanner)
- Keep native UI in SwiftUI (no Compose migration)
- Shared Kotlin runtime remains authoritative for all business logic
- Avoid duplicating business logic in Swift
- Use closure-based subscription pattern (compatible with Kotlin/Native)
- Support parallel implementation across multiple agents

### Constraints
- Do NOT modify existing files (per requirements)
- Must work within Kotlin/Native export limitations
- iOS StoreKit stays in Swift (`StoreKitPurchaseClient`) injected as `PurchaseClient`
- Product detail must remain route-scoped (not global shared state) per ADR-0019
- Coach is a modal over current screen (gated by premium), not a 4th tab
- Navigation route constants come from shared `core:domain` (`NutriLogRoutes`, `NutriLogDestination`)
- One shared runtime instance owned by the app composition root
- Add food flow: `AddFoodViewModel` outlives both steps, owned by app root (like Android `AppDependencies`)
- `Flow`/`StateFlow` not directly Swift-friendly - must use closure subscriptions

---

## Existing Architecture to Preserve

### Core Principles (from ADRs)
- **ADR-0013**: StoreKit integration in Swift, injected as shared `PurchaseClient`
- **ADR-0015**: Composition root pattern - all wiring in one place, screens are stateless
- **ADR-0016**: AI gateway configuration - assistant disabled if not configured
- **ADR-0019**: Navigation model - routes as contract, product detail route-scoped, meal sheet modal not in nav stack

### Patterns to Reuse
1. **Bridge pattern**: Closure subscriptions returning `Long` IDs, `cancelSubscription(id)`, `close()` 
2. **One runtime**: `ProductSearchRuntime` owns all repositories, DB, HTTP client
3. **Route-scoped VMs**: Product detail VM created per route with its own scope (like Android)
4. **App-level VMs**: Diary, AddFood, Summary, Library, Coach, Premium owned by composition root
5. **AddFoodViewModel lifecycle**: Outlives both steps, reset on new `sessionId`

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         iOS App (SwiftUI)                           │
├─────────────────────────────────────────────────────────────────────┤
│  AppRoot (Composition Root)                                          │
│  ├── IosAppBridge (extended bridge - NEW)                           │
│  ├── StoreKitPurchaseClient (PurchaseClient impl)                   │
│  ├── NavigationShell (TabView + NavigationStack per tab)            │
│  └── Modal presenters (Coach, Privacy Policy)                       │
├─────────────────────────────────────────────────────────────────────┤
│  SwiftUI Screens (stateless, receive state + callbacks)             │
│  ├── TodayView, LibraryView, MoreView                               │
│  ├── AddFoodSourceView, AddFoodQuantityView                         │
│  ├── ProductDetailView (route-scoped VM)                            │
│  ├── WeeklyView                                                     │
│  └── CoachView (modal)                                              │
├─────────────────────────────────────────────────────────────────────┤
│                    IosAppBridge (Kotlin/Native)                     │
│  ├── Wraps ProductSearchRuntime                                      │
│  ├── Manages all ViewModels (or exposes them)                       │
│  ├── Closure-based subscriptions                                     │
│  ├── Action forwarding                                               │
│  └── Lifecycle management                                            │
├─────────────────────────────────────────────────────────────────────┤
│                    ProductSearchRuntime (Shared)                    │
│  ├── All Repositories (Diary, Product, Saved, Purchase, etc.)       │
│  ├── SQLDelight DB, Ktor HTTP Client                                │
│  ├── AssistantClient                                                │
│  └── BarcodeResolver                                                │
└─────────────────────────────────────────────────────────────────────┘
```

### Key Architectural Decisions

1. **Extend bridge, don't replace pattern**: Create `IosAppBridge` (or extend `IosNutriLogBridge`) to expose all VMs/states needed for parity. Keep closure subscription pattern.
2. **AppRoot owns everything**: Like `AppDependencies` on Android, one object owns runtime, all VMs, barcode resolver.
3. **SwiftUI consumes snapshots**: Bridge converts `StateFlow` to closure callbacks on Main dispatcher. DTOs for complex types.
4. **Route-scoped VMs created by bridge**: `createProductDetailViewModel(code:)` returns a handle with its own subscription scope.
5. **Tab navigation + stack navigation**: Mirror Android's `AppShell` with 3 tabs (Today, Library, More) + pushed routes.

---

## Bridge API Design

### Current Limitations
`IosNutriLogBridge` only exposes saved products and purchases. Need full parity.

### Proposed: `IosAppBridge` (New/Extended)

**Location:** `shared/src/iosMain/kotlin/com/example/nutricionalregister/IosAppBridge.kt`

**Ownership:** Created once by Swift AppRoot, owns `ProductSearchRuntime` and all ViewModels (like `AppDependencies`).

```kotlin
public class IosAppBridge internal constructor(
    private val runtime: ProductSearchRuntime,
    private val purchaseClient: PurchaseClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val jobs = mutableMapOf<Long, Job>()
    private var nextSubscriptionId: Long = 0L
    
    // App-level VMs (owned here)
    private val diaryViewModel: DiaryViewModel
    private val addFoodViewModel: AddFoodViewModel
    private val summaryViewModel: SummaryViewModel
    private val libraryViewModel: SavedProductsViewModel // or use savedProductRepository
    private val coachViewModel: CoachViewModel
    private val premiumPurchaseViewModel: PremiumPurchaseViewModel
    private val searchViewModel: ProductSearchViewModel
    private val barcodeResolver: BarcodeResolver
    
    // Subscriptions...
    // Actions...
    // Route-scoped VM factories...
    // Lifecycle...
}
```

### Symbol-Level API Design

#### 1. Runtime Info
```kotlin
/** Whether assistant AI gateway is configured */
public val isAssistantConfigured: Boolean get() = runtime.isAssistantConfigured
```

#### 2. Diary/Today
```kotlin
public fun observeDiaryState(onChange: (DiaryStateSnapshot) -> Unit): Long
public fun refreshDiary()
public fun removeDiaryEntry(id: Long)
public fun updateGoal(goal: NutritionGoal) // NutritionGoal is data class - check export
```

**Note:** `NutritionGoal` is likely exported. `DiaryState` contains `MealType` keys - need flattening.

#### 3. Add Food Flow
```kotlin
public fun observeAddFoodState(onChange: (AddFoodStateSnapshot) -> Unit): Long
public fun startAddFood(sessionId: String, meal: String? = null, productCode: String? = null)
public fun selectAddFoodSource(source: String) // "SEARCH" or "LIBRARY" or "BARCODE"? Check AddFoodSource enum
public fun setAddFoodQuery(query: String)
public fun selectAddFoodProduct(product: Product) // or pass code and resolve?
public fun setAddFoodQuantity(grams: Double)
public fun openAddFoodMealSheet()
public fun closeAddFoodMealSheet()
public fun selectAddFoodMeal(meal: String) // MealType name
public fun confirmAddFood() // returns via callback for errors?
```

**Critical:** `AddFoodViewModel.confirm()` does fire-and-forget write then Android navigates back. For reliability on iOS, maybe add `confirmAddFood(onComplete: (Result<Unit>) -> Unit)` or observe completion state.

**AddFoodSource:** Check its definition - likely enum `SEARCH`, `LIBRARY`, `BARCODE`.

#### 4. Search
```kotlin
public fun observeSearchState(onChange: (ProductSearchStateSnapshot) -> Unit): Long
public fun setSearchQuery(query: String)
public fun submitSearch()
public fun clearSearch()
```

#### 5. Library/Saved Products
```kotlin
public fun observeLibraryState(onChange: (SavedProductsStateSnapshot) -> Unit): Long
public fun saveProduct(product: Product)
public fun removeSavedProduct(code: String)
public fun clearLibrary()
```

#### 6. Product Detail (Route-Scoped)
```kotlin
public class ProductDetailHandle {
    public fun observeState(onChange: (ProductDetailStateSnapshot) -> Unit): Long
    public fun load(code: String)
    public fun toggleSaved() // needs access to library state or handle internally
    public fun clear()
}

public fun createProductDetailHandle(code: String): ProductDetailHandle
```

**Note:** ProductDetailViewModel needs `savedProductRepository`. Must pass it. Also needs to know if saved - either observe library state in Swift or add method.

#### 7. Summary/Weekly
```kotlin
public fun observeWeeklySummary(onChange: (WeeklySummarySnapshot?) -> Unit): Long
public fun clearSummary()
```

#### 8. Coach/Assistant
```kotlin
public fun observeCoachState(onChange: (AssistantStateSnapshot) -> Unit): Long
public fun sendCoachMessage(text: String)
public fun grantCoachConsent()
public fun confirmCoachProposal()
public fun dismissCoachProposal()
```

**Coach gating:** Premium `isUnlocked` checked in UI (like Android OR with debug build). iOS needs same logic.

#### 9. Premium
```kotlin
public fun observePremiumState(onChange: (PremiumPurchaseStateSnapshot) -> Unit): Long
public fun startPremiumPurchase()
public fun retryPremiumPurchase()
public fun restorePremiumPurchases()
public fun queryPremiumProduct(onResult: (String?) -> Unit) // price label
```

#### 10. Barcode Scanning
```kotlin
public fun resolveBarcode(code: String, onResolved: (BarcodeResolutionSnapshot) -> Unit)
```

Need to expose `BarcodeResolution` as snapshot/enum-like strings.

#### 11. Utilities
```kotlin
public suspend fun getProduct(code: String): Product? // or bridge method
public fun productDetail(code: String): String = NutriLogRoutes.productDetail(code) // or use shared
```

#### 12. Lifecycle
```kotlin
public fun cancelSubscription(id: Long)
public fun clearAll() // clears all VMs like AppDependencies.close()
public fun close() // calls clearAll() + runtime.close()
```

### Factory Function
```kotlin
public fun createIosAppBridge(
    purchaseClient: PurchaseClient,
    aiGatewayBaseUrl: String = ""
): IosAppBridge
```

Need to update `createProductSearchRuntime` call to pass `aiGatewayBaseUrl` (currently not passed in old `createIosNutriLogBridge`).

### Snapshot DTOs

Swift can't easily consume complex Kotlin types with `Map`, sealed classes, nullable generics in some cases. Create flat snapshot classes/DTOs:

```kotlin
// Example snapshots
public data class DiaryStateSnapshot(
    val dayEpochMillis: Long,
    val entriesByMeal: Map<String, List<DiaryEntrySnapshot>>, // or array
    val totals: NutritionTotalsSnapshot,
    val mealTotals: Map<String, NutritionTotalsSnapshot>,
    val goal: NutritionGoal?,
    val isLoading: Boolean
)

public data class DiaryEntrySnapshot(
    val id: Long,
    val productCode: String,
    val productName: String,
    val mealType: String, // MealType.name
    val quantityGrams: Double,
    val loggedAtEpochMillis: Long,
    val dayEpochMillis: Long,
    val caloriesKcal: Double?,
    val proteinG: Double?,
    val fatG: Double?,
    val saturatedFatG: Double?,
    val carbsG: Double?,
    val fiberG: Double?,
    val saltG: Double?,
    val sugarG: Double?
)

public data class AddFoodStateSnapshot(
    val source: String?, // AddFoodSource.name
    val query: String,
    val product: Product?, // Product data class - check if fully exported
    val quantityGrams: Double,
    val preview: NutritionContributionSnapshot?,
    val mealType: String?,
    val isMealSheetOpen: Boolean,
    val isResolvingProduct: Boolean,
    val canConfirm: Boolean
)

public data class NutritionContributionSnapshot(
    val caloriesKcal: Double,
    val proteinG: Double,
    val fatG: Double,
    val saturatedFatG: Double,
    val carbsG: Double,
    val fiberG: Double,
    val saltG: Double,
    val sugarG: Double
)

public data class ProductSearchStateSnapshot(
    val query: String,
    val products: List<Product>,
    val recent: List<RecentSearch>,
    val isLoading: Boolean,
    val error: String?
)

public data class SavedProductsStateSnapshot(
    val saved: List<SavedProduct>,
    val savedCodes: List<String>, // easier for Swift Set conversion
    val isLoading: Boolean
)

public data class AssistantStateSnapshot(
    val messages: List<AssistantMessageSnapshot>,
    val pendingProposal: AssistantReplySnapshot?,
    val availability: String, // "READY", "NOT_CONFIGURED", "CONSENT_REQUIRED", "OFFLINE"
    val isSending: Boolean,
    val error: String?
)

public data class PremiumPurchaseStateSnapshot(
    val isUnlocked: Boolean,
    val priceLabel: String?,
    val isPurchasing: Boolean,
    val error: String?
)

public data class BarcodeResolutionSnapshot(
    val type: String, // "RESOLVED", "NOT_IN_CACHE", "UNKNOWN"
    val product: Product? = null,
    val code: String? = null
)
```

**Important Notes on Snapshots:**
- Convert `MealType` enum to string name
- Convert `Map` keys to strings or arrays (Swift Map with Kotlin types can be problematic)
- `Product`, `SavedProduct`, `NutritionGoal` are data classes - likely fine as-is if exported
- `StateFlow` collected on Main dispatcher in bridge, snapshots passed to Swift closures

---

## SwiftUI State & View Models

### Architecture Pattern
SwiftUI views should be **stateless** where possible, receiving state objects and callbacks. Use `@MainActor ObservableObject` view models that wrap bridge subscriptions.

### AppRoot / Composition Root

**Location:** `iosApp/iosApp/AppRoot.swift` (NEW)

```swift
@MainActor
final class AppRoot: ObservableObject {
    let bridge: IosAppBridge
    let storeKitClient: StoreKitPurchaseClient
    
    // Tab selection
    @Published var selectedTab: NutriLogDestination = .today
    
    // Modals
    @Published var showCoach: Bool = false
    @Published var showPrivacyPolicy: Bool = false
    
    // Navigation paths per tab or global
    @Published var todayPath = NavigationPath()
    @Published var libraryPath = NavigationPath()
    @Published var morePath = NavigationPath()
    
    init() {
        self.storeKitClient = StoreKitPurchaseClient()
        // Get AI gateway URL from Info.plist or build config
        let aiGatewayUrl = Bundle.main.object(forInfoDictionaryKey: "NUTRILOG_AI_PROXY_URL") as? String ?? ""
        self.bridge = IosAppBridgeKt.createIosAppBridge(purchaseClient: storeKitClient, aiGatewayBaseUrl: aiGatewayUrl)
    }
    
    deinit {
        bridge.close()
    }
}
```

### Screen View Models (per screen)

Each major screen gets its own `ObservableObject` that subscribes to bridge and exposes state/callbacks.

#### TodayViewModel
```swift
@MainActor
final class TodayViewModel: ObservableObject {
    @Published private(set) var diaryState: DiaryStateSnapshot?
    @Published private(set) var weeklySummary: WeeklySummarySnapshot?
    
    private let bridge: IosAppBridge
    private var subs: [Int64] = []
    
    init(bridge: IosAppBridge) {
        self.bridge = bridge
        subs.append(bridge.observeDiaryState { [weak self] in self?.diaryState = $0 })
        subs.append(bridge.observeWeeklySummary { [weak self] in self?.weeklySummary = $0 })
    }
    
    func refresh() { bridge.refreshDiary() }
    func removeEntry(id: Int64) { bridge.removeDiaryEntry(id: id) }
    func addFood(meal: String) { /* navigate */ }
    func openProduct(code: String) { /* navigate */ }
    func openWeekly() { /* navigate */ }
    
    deinit {
        subs.forEach { bridge.cancelSubscription(subscriptionId: $0) }
    }
}
```

#### AddFoodFlowViewModel
```swift
@MainActor
final class AddFoodFlowViewModel: ObservableObject {
    @Published private(set) var state: AddFoodStateSnapshot?
    @Published private(set) var searchState: ProductSearchStateSnapshot?
    @Published private(set) var libraryState: SavedProductsStateSnapshot?
    
    private let bridge: IosAppBridge
    private var subs: [Int64] = []
    private var sessionId: String = UUID().uuidString
    
    init(bridge: IosAppBridge) {
        self.bridge = bridge
        subs.append(bridge.observeAddFoodState { [weak self] in self?.state = $0 })
        subs.append(bridge.observeSearchState { [weak self] in self?.searchState = $0 })
        subs.append(bridge.observeLibraryState { [weak self] in self?.libraryState = $0 })
    }
    
    func start(meal: String? = nil, productCode: String? = nil) {
        sessionId = UUID().uuidString // new session each time flow opens
        bridge.startAddFood(sessionId: sessionId, meal: meal, productCode: productCode)
    }
    
    func selectSource(_ source: String) { bridge.selectAddFoodSource(source: source) }
    func setQuery(_ query: String) { bridge.setAddFoodQuery(query: query) }
    func submitSearch() { bridge.submitSearch() }
    func openProduct(code: String) { /* navigate */ }
    func scanBarcode(_ code: String) {
        bridge.resolveBarcode(code: code) { [weak self] resolution in
            self?.handleBarcodeResolution(resolution)
        }
    }
    func setQuantity(_ grams: Double) { bridge.setAddFoodQuantity(grams: grams) }
    func openMealSheet() { bridge.openAddFoodMealSheet() }
    func closeMealSheet() { bridge.closeAddFoodMealSheet() }
    func selectMeal(_ meal: String) { bridge.selectAddFoodMeal(meal: meal) }
    func confirm() { 
        bridge.confirmAddFood()
        // Navigation back handled by caller or observe state
    }
    func back() { /* navigate */ }
    
    private func handleBarcodeResolution(_ resolution: BarcodeResolutionSnapshot) {
        if resolution.type == "RESOLVED", let product = resolution.product {
            bridge.selectAddFoodProduct(product: product)
            // Navigate to quantity step
        } else if resolution.type == "NOT_IN_CACHE", let code = resolution.code {
            bridge.selectAddFoodSource(source: "SEARCH")
            bridge.setAddFoodQuery(query: code)
            bridge.submitSearch()
        }
        // UNKNOWN: no-op
    }
    
    deinit {
        subs.forEach { bridge.cancelSubscription(subscriptionId: $0) }
    }
}
```

#### ProductDetailViewModel (Route-Scoped)
```swift
@MainActor
final class ProductDetailViewModel: ObservableObject {
    @Published private(set) var state: ProductDetailStateSnapshot?
    @Published private(set) var isSaved: Bool = false
    
    private let bridge: IosAppBridge
    private let handle: ProductDetailHandle
    private var subs: [Int64] = []
    
    init(bridge: IosAppBridge, code: String, libraryState: SavedProductsStateSnapshot?) {
        self.bridge = bridge
        self.handle = bridge.createProductDetailHandle(code: code)
        subs.append(handle.observeState { [weak self] in self?.state = $0 })
        // Track saved status from library state passed in
        self.isSaved = libraryState?.savedCodes.contains(code) ?? false
    }
    
    func load(code: String) { handle.load(code: code) }
    func toggleSaved() { 
        // Need to add/remove via bridge - toggleSaved on handle or use bridge
        handle.toggleSaved()
    }
    func addToDiary() { /* navigate to add food with productCode */ }
    func back() { /* navigate */ }
    
    deinit {
        subs.forEach { handle.cancelSubscription(subscriptionId: $0) } // or bridge
        handle.clear()
    }
}
```

**Note:** Need to check if `ProductDetailViewModel` in shared has `toggleSaved()` method or if we need to go through library/saved repo.

#### CoachViewModel (Modal)
```swift
@MainActor
final class CoachViewModel: ObservableObject {
    @Published private(set) var state: AssistantStateSnapshot?
    private let bridge: IosAppBridge
    private var sub: Int64?
    
    init(bridge: IosAppBridge) {
        self.bridge = bridge
        sub = bridge.observeCoachState { [weak self] in self?.state = $0 }
    }
    
    func send(_ text: String) { bridge.sendCoachMessage(text: text) }
    func grantConsent() { bridge.grantCoachConsent() }
    func confirmProposal() { bridge.confirmCoachProposal() }
    func dismissProposal() { bridge.dismissCoachProposal() }
    
    deinit {
        if let sub { bridge.cancelSubscription(subscriptionId: sub) }
    }
}
```

#### PremiumViewModel
```swift
@MainActor
final class PremiumViewModel: ObservableObject {
    @Published private(set) var state: PremiumPurchaseStateSnapshot?
    private let bridge: IosAppBridge
    private var sub: Int64?
    
    init(bridge: IosAppBridge) {
        self.bridge = bridge
        sub = bridge.observePremiumState { [weak self] in self?.state = $0 }
    }
    
    func buy() { bridge.startPremiumPurchase() }
    func retry() { bridge.retryPremiumPurchase() }
    func restore() { bridge.restorePremiumPurchases() }
    
    deinit {
        if let sub { bridge.cancelSubscription(subscriptionId: sub) }
    }
}
```

---

## Navigation Design

### Route Contract
Use shared `NutriLogRoutes` from `core:domain`. Routes:
- `today`, `library`, `more` (tabs)
- `addfood/source?$ARG_MEAL={}&$ARG_PRODUCT={}`, `addfood/quantity?$ARG_MEAL={}&$ARG_PRODUCT={}`
- `product/{code}`
- `coach`, `weekly`

### Navigation Structure

Mirror Android's `AppShell` + `NutriLogNavigator`:

```swift
enum NutriLogTab: Hashable {
    case today, library, more
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
        // Navigate to add food in current tab's context or global
        let route = NutriLogRoutesKt.addFood(meal: meal, productCode: productCode)
        // Parse and navigate
    }
    
    func openProduct(_ code: String) {
        let route = NutriLogRoutesKt.productDetail(code: code)
        // Navigate within current tab
    }
    
    func openWeekly() {
        // Push weekly
    }
    
    func back() {
        // Pop current path
    }
}
```

### TabView Structure
```swift
struct ContentView: View {
    @EnvironmentObject var appRoot: AppRoot
    @StateObject private var navigator = AppNavigator()
    
    var body: some View {
        TabView(selection: $navigator.selectedTab) {
            NavigationStack(path: $navigator.todayPath) {
                TodayView()
                    .navigationDestination(for: String.self) { route in
                        // Route resolution
                    }
            }
            .tabItem { Label("Today", systemImage: "calendar") }
            .tag(NutriLogTab.today)
            
            NavigationStack(path: $navigator.libraryPath) {
                LibraryView()
            }
            .tabItem { Label("Library", systemImage: "book") }
            .tag(NutriLogTab.library)
            
            NavigationStack(path: $navigator.morePath) {
                MoreView()
            }
            .tabItem { Label("More", systemImage: "ellipsis") }
            .tag(NutriLogTab.more)
        }
        .sheet(isPresented: $appRoot.showCoach) {
            CoachView()
        }
        .alert("Privacy", isPresented: $appRoot.showPrivacyPolicy) {
            Button("Close", role: .cancel) {}
        } message: {
            Text("Your diary, your goals and your library stay on this device...")
        }
    }
}
```

### Route Resolution
Need to parse route strings or use navigation destinations. Options:
1. Use `navigationDestination(for:)` with route enum cases
2. Parse string routes manually
3. Use shared route builders

**Recommendation:** Define Swift route enum mirroring shared routes, use `navigationDestination(for: Route.self)`.

```swift
enum Route: Hashable {
    case today
    case library
    case more
    case addFoodSource(meal: String?, productCode: String?)
    case addFoodQuantity(meal: String?, productCode: String?)
    case productDetail(code: String)
    case weekly
}
```

### Add Food Flow Navigation
Like Android: route determines step (`ADD_FOOD_SOURCE` vs `ADD_FOOD_QUANTITY`). When `productCode` provided, route goes to quantity step. Step is part of route (ADR-0019).

```swift
// When opening add food
func openAddFood(meal: String? = nil, productCode: String? = nil) {
    if productCode?.isEmpty == false {
        todayPath.append(Route.addFoodQuantity(meal: meal, productCode: productCode))
    } else {
        todayPath.append(Route.addFoodSource(meal: meal, productCode: productCode))
    }
}
```

### Product Detail
Route-scoped, pushed onto current stack (not modal). Created fresh VM per navigation.

```swift
.navigationDestination(for: Route.self) { route in
    switch route {
    case .productDetail(let code):
        ProductDetailView(code: code)
    // ...
    }
}
```

### Coach Modal
Gated by premium - check `premiumState.isUnlocked || isDebugBuild`. Like Android.

```swift
// In MoreView or AppRoot
func openCoach() {
    let isDebug = Bundle.main.object(forInfoDictionaryKey: "DEBUG") as? Bool ?? false
    if premiumState.isUnlocked || isDebug {
        appRoot.showCoach = true
    } else {
        // Show upgrade prompt or navigate to More
    }
}
```

---

## Lifecycle & One-Runtime Ownership

### Ownership Model
Like Android `AppDependencies`:
- `AppRoot` (Swift) owns `IosAppBridge` (Kotlin)
- `IosAppBridge` owns `ProductSearchRuntime` and all ViewModels
- Runtime lives for app lifetime, closed on app termination

### Coroutine Scopes
- Bridge creates `CoroutineScope(SupervisorJob() + Dispatchers.Main)` for subscriptions
- All ViewModel scopes created in bridge (like Android)
- Route-scoped VMs (ProductDetail) get their own scope passed from bridge

### Subscription Management
- All subscriptions tracked in `jobs` map with `Long` IDs
- Swift must call `cancelSubscription(id:)` on deinit
- Bridge `close()` cancels all jobs, calls VM `clear()` methods, closes runtime

### ViewModel Cleanup
Mirror Android `AppDependencies.close()`:
```kotlin
public fun clearAll() {
    searchViewModel.clear()
    premiumPurchaseViewModel.clear()
    libraryViewModel.clear()
    summaryViewModel.clear()
    diaryViewModel.clear()
    coachViewModel.clear() // if has clear
    // addFoodViewModel doesn't have clear() in codebase? Check
}
```

**Note:** Check if `AddFoodViewModel` has `clear()`. `CoachViewModel` doesn't show `clear()` in what we read. `DiaryViewModel` has `clear()`. Need to verify all VMs.

### App Lifecycle
- Create `AppRoot` in `@main App` or as `@StateObject` in root view
- Don't recreate on view updates
- Call `bridge.close()` in `deinit` or `onDisappear` at app level

```swift
@main
struct NutriLogApp: App {
    @StateObject private var appRoot = AppRoot()
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appRoot)
        }
    }
}
```

---

## Feature-Specific Designs

### 1. Today/Diary
- Observe `DiaryState` via bridge
- Show meals (Breakfast, Lunch, Dinner, Snack?) from `entriesByMeal` map
- Display totals
- Show weekly summary card (from `SummaryViewModel`)
- Actions: add food per meal, remove entry, open product, open weekly

**MealType:** Exported as Kotlin class with constants (`MealType.breakfast`, etc.). Need to bridge as strings or get list.

### 2. Add Food Flow (Two Steps)
**Session management:** Generate new `sessionId` (UUID) each time flow opened. Pass to `startAddFood()`.
- Step 1 (Source): Choose SEARCH, LIBRARY, or BARCODE
- Step 2 (Quantity): Select quantity, meal, confirm

**Flow behavior (from Android):**
- If `productCode` passed initially → goes to quantity step
- `AddFoodViewModel` outlives both steps
- Meal chosen via modal sheet (not nav step)
- Confirm writes entry via `DiaryRepository.addEntry()` then navigates back
- Search results from `ProductSearchViewModel`, library from `SavedProductsViewModel`

**Barcode flow:**
1. User scans → `AVFoundation`/`Vision` gets code
2. Call `bridge.resolveBarcode(code:)` 
3. Handle resolution:
   - `RESOLVED`: Get product, call `bridge.selectAddFoodProduct(product:)`, navigate to quantity step
   - `NOT_IN_CACHE`: Set source to SEARCH, set query to code, submit search
   - `UNKNOWN`: No-op or show error

### 3. Library
- Observe `SavedProductsState` 
- List saved products
- Actions: open product detail, add to diary (opens add food with productCode), remove

### 4. Product Detail (Route-Scoped)
- Created via `bridge.createProductDetailHandle(code:)` 
- Load product details
- Toggle saved (check if in library)
- Add to diary navigates to add food flow with productCode
- Must be route-scoped, disposed on back

### 5. Weekly Summary
- Observe `weeklySummary` from `SummaryViewModel`
- Simple read-only view

### 6. More Screen
- Show goals (from diary state), allow editing
- Premium purchase section
- Coach button (gated by premium or debug)
- Privacy policy button
- About? (not visible in Android App.kt, but might exist)

### 7. Coach/Assistant (Modal)
- Observe `AssistantState`
- Check `availability`: NOT_CONFIGURED, CONSENT_REQUIRED, OFFLINE, READY
- If NOT_CONFIGURED: Show message, disable input
- If CONSENT_REQUIRED: Show consent UI, call `grantConsent()`
- If OFFLINE: Show message
- If READY: Show chat, allow sending
- Handle proposals: show pending proposal, confirm/dismiss
- Gate access: premium unlocked OR debug build (check in Swift like Android)

**Android logic:**
```kotlin
val isDebugBuild = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
CoachDialog(
    state = coachState,
    isUnlocked = purchaseState.isUnlocked || isDebugBuild,
    ...
)
```

### 8. Premium
- Observe `PremiumPurchaseState`
- Show price from `queryProduct(PremiumProduct.CODE)` or from state
- Buy via `startPurchase()` 
- Restore via `restore()`
- Product code is `PremiumProduct.CODE = "nutrilog_pro"` (from domain)

**StoreKit mapping:** `StoreKitPurchaseClient` must map to premium product ID. Current fallback is `com.example.nutricionalregister.product.` + code. Need to check and update mapping for `nutrilog_pro`.

### 9. Barcode Scanning
- Use `AVFoundation` for camera capture
- Use `AVCaptureMetadataOutput` for barcode detection (or Vision framework)
- Request camera permission: add `NSCameraUsageDescription` to `Info.plist`
- Pass detected code to bridge's `resolveBarcode()`
- Handle resolution like Android

---

## Platform Integrations

### 1. StoreKit (Already Exists)
`StoreKitPurchaseClient.swift` exists and implements `PurchaseClient`. 
- Needs to handle premium product `nutrilog_pro`
- Current fallback logic may need adjustment
- Must be injected into bridge

### 2. AI Gateway URL
Need to pass to bridge. Options:
- Add to `Info.plist` as `NUTRILOG_AI_PROXY_URL`
- Or build configuration
- iOS equivalent to Android's `R.string.ai_gateway_base_url`

In `AppRoot`:
```swift
let aiGatewayUrl = Bundle.main.object(forInfoDictionaryKey: "NUTRILOG_AI_PROXY_URL") as? String ?? ""
```

### 3. Camera Permissions
Add to `Info.plist`:
```xml
<key>NSCameraUsageDescription</key>
<string>This app uses the camera to scan barcodes and add foods to your diary.</string>
```

### 4. Connectivity (Optional)
Android has `AndroidConnectivityStatusProvider`. iOS might need one or pass null. Coach handles null connectivity provider.

### 5. Day Boundary
Shared `SystemDayBoundaryProvider` works on iOS.

---

## Kotlin/Native & Swift Interop Caveats

### Critical Issues to Address

#### 1. Flow/StateFlow
**Problem:** `Flow` and `StateFlow` not directly consumable by Swift
**Solution:** Bridge collects on `Dispatchers.Main` and calls Swift closures. Never expose Flow to Swift.

#### 2. Default Parameters
**Problem:** Kotlin default args not exported as Swift overloads
**Solution:** Bridge methods must have all parameters explicit (no defaults)

#### 3. Sealed Classes/Interfaces
**Problem:** Export as protocols/classes, not Swift enums
**Solution:** Use flat snapshot DTOs with status strings. For `PurchaseState`, `BarcodeResolution`, convert to snapshots/strings.

#### 4. Enums
**Problem:** Kotlin enums export, but better to use strings for simplicity
**Solution:** Convert `MealType`, `AddFoodSource`, `AssistantAvailability` to strings in snapshots

#### 5. Maps
**Problem:** `Map<MealType, List<DiaryEntry>>` problematic in Swift
**Solution:** Flatten to arrays or use `Dictionary<String, [...]>` with string keys

#### 6. Nullable Primitives
**Problem:** `Double?` becomes `KotlinDouble?` in Swift, needs unwrapping
**Solution:** Use `Double?` carefully or convert to optionals in snapshots

#### 7. Data Classes
**Problem:** Generally fine, but need to check all exports
**Solution:** `Product`, `SavedProduct`, `NutritionGoal` should be fine as data classes

#### 8. Suspending Functions
**Problem:** Kotlin coroutines need bridging
**Solution:** Use `suspend` functions carefully or expose callback-based versions

#### 9. Collections
**Problem:** Kotlin collections map to Swift collections
**Solution:** Usually works, but be careful with mutability

#### 10. Long
**Problem:** Kotlin `Long` maps to Swift `Int` on 64-bit platforms, but better to use `Int64` explicitly

#### 11. Unit/Function Types
**Problem:** Closures need proper Swift types
**Solution:** Use `@escaping` closures, `((T) -> Void)`

#### 12. Package Names
**Problem:** Kotlin `package` becomes module prefix in some cases
**Solution:** Use `import Shared` and call with proper names (`IosAppBridgeKt.createIosAppBridge`)

### Specific Type Mappings

| Kotlin | Swift | Notes |
|--------|-------|-------|
| `String` | `String` | OK |
| `Int`, `Long` | `Int`, `Int64` | Use `Int64` for subscription IDs |
| `Double` | `Double` | OK |
| `Boolean` | `Bool` | OK |
| `List<T>` | `[T]` | OK |
| `Map<K,V>` | `[K: V]` | Can be problematic with Kotlin types as keys |
| `Flow<T>` | N/A | Don't expose, use closures |
| `StateFlow<T>` | N/A | Don't expose, use closures |
| `data class` | Class/struct | Usually exported as classes |
| `sealed class/interface` | Protocol/class hierarchy | Hard to pattern match in Swift |
| `enum class` | Enum | Usually exported |
| `suspend fun` | Completion handlers or async/await | Complex, prefer callbacks |

### Best Practices
1. **Minimize cross-language calls** - batch operations where possible
2. **Use snapshots** - convert complex state to flat DTOs
3. **Main thread** - bridge already uses `Dispatchers.Main`
4. **Weak references** - Swift must use `[weak self]` in closures
5. **Subscription cleanup** - Always cancel in `deinit`
6. **Avoid passing lambdas with captures across boundaries** - be careful

---

## Component/File Split for Parallel Agents

To enable parallel implementation, split work into independent components.

### Agent 1: Bridge Extension (Kotlin/Native)
**Files:** `shared/src/iosMain/kotlin/com/example/nutricionalregister/IosAppBridge.kt` (NEW)  
**Scope:**
- Extend/create bridge with all APIs needed for parity
- Create snapshot DTO classes
- Wire all ViewModels like AppDependencies
- Add factory function
- Handle subscription management
- Add VM clear/close logic
- **Dependencies:** Read all ViewModels, ProductSearchRuntime, routes
- **Can work independently** once understand APIs

### Agent 2: Platform Factories & Config (Kotlin/Native)
**Files:** `core/data/src/iosMain/kotlin/com/example/nutricionalregister/data/PlatformDataFactories.kt` (may need updates)  
**Scope:**
- Ensure `createProductSearchRuntime` accepts `aiGatewayBaseUrl` properly
- Add support for passing connectivity provider if needed
- Check if any iOS-specific factory changes needed
- **Dependencies:** Agent 1 will use factory

### Agent 3: Swift AppRoot & Navigation (Swift)
**Files:** 
- `iosApp/iosApp/AppRoot.swift` (NEW)
- `iosApp/iosApp/AppNavigator.swift` (NEW)
- `iosApp/iosApp/ContentView.swift` (REPLACE/UPDATE)
- `iosApp/iosApp/NutriLogApp.swift` (NEW or UPDATE @main)

**Scope:**
- Create composition root
- Navigation structure with TabView + NavigationStack
- Route enum and resolution
- Modal presenters
- **Dependencies:** Bridge API from Agent 1

### Agent 4: Today & Diary UI (Swift)
**Files:**
- `iosApp/iosApp/Views/Today/TodayView.swift` (NEW)
- `iosApp/iosApp/Views/Today/TodayViewModel.swift` (NEW)
- `iosApp/iosApp/Views/Components/MealSectionView.swift` (NEW)
- `iosApp/iosApp/Views/Components/DiaryEntryRow.swift` (NEW)

**Scope:**
- Today screen UI
- Meal sections, entries, totals
- Weekly summary card
- Actions wired to navigator/bridge

### Agent 5: Add Food Flow UI (Swift)
**Files:**
- `iosApp/iosApp/Views/AddFood/AddFoodFlowView.swift` (NEW)
- `iosApp/iosApp/Views/AddFood/AddFoodSourceView.swift` (NEW)
- `iosApp/iosApp/Views/AddFood/AddFoodQuantityView.swift` (NEW)
- `iosApp/iosApp/Views/AddFood/MealPickerSheet.swift` (NEW)
- `iosApp/iosApp/ViewModels/AddFoodFlowViewModel.swift` (NEW)

**Scope:**
- Two-step flow
- Source selection (Search/Library/Barcode)
- Quantity input, meal selection modal
- Barcode scanner integration
- **Dependencies:** Agent 1 bridge API

### Agent 6: Library & Product Detail UI (Swift)
**Files:**
- `iosApp/iosApp/Views/Library/LibraryView.swift` (NEW)
- `iosApp/iosApp/Views/Library/LibraryViewModel.swift` (NEW)
- `iosApp/iosApp/Views/ProductDetail/ProductDetailView.swift` (NEW)
- `iosApp/iosApp/Views/ProductDetail/ProductDetailViewModel.swift` (NEW)

**Scope:**
- Library screen
- Route-scoped product detail
- Save/unsave, add to diary

### Agent 7: More, Coach, Premium UI (Swift)
**Files:**
- `iosApp/iosApp/Views/More/MoreView.swift` (NEW)
- `iosApp/iosApp/Views/More/MoreViewModel.swift` (NEW)
- `iosApp/iosApp/Views/Coach/CoachView.swift` (NEW)
- `iosApp/iosApp/Views/Coach/CoachViewModel.swift` (NEW)
- `iosApp/iosApp/Views/Premium/PremiumPurchaseSection.swift` (NEW)

**Scope:**
- More screen (goals, premium, privacy, coach)
- Coach modal
- Premium purchase UI
- Gate logic

### Agent 8: Weekly Summary & Scanner (Swift)
**Files:**
- `iosApp/iosApp/Views/Weekly/WeeklyView.swift` (NEW)
- `iosApp/iosApp/Views/Weekly/WeeklyViewModel.swift` (NEW)
- `iosApp/iosApp/Views/Scanner/BarcodeScannerView.swift` (NEW)
- `iosApp/iosApp/Views/Scanner/BarcodeScannerRepresentable.swift` (NEW)

**Scope:**
- Weekly summary screen
- AVFoundation barcode scanner
- Camera permissions handling

### Agent 9: Config, Permissions, Polish (Swift)
**Files:**
- `iosApp/iosApp/Info.plist` (UPDATE - add permissions, config)
- `iosApp/iosApp/Resources/` (if needed)
- Theme/styles
- **Scope:**
- Add `NSCameraUsageDescription`
- Add `NUTRILOG_AI_PROXY_URL` (or build config)
- Ensure StoreKitPurchaseClient works with premium

### Coordination Notes
- **Agent 1 must complete first** or be mocked - others depend on bridge API
- Use interface/protocol definitions to allow parallel work
- Snapshot DTOs need to be agreed upon
- Route strings come from shared (can use `NutriLogRoutesKt`)

---

## High-Risk APIs

### Kotlin/Native Export Risks

| API/Type | Risk Level | Issue | Mitigation |
|----------|------------|-------|------------|
| `StateFlow<T>` / `Flow<T>` | **CRITICAL** | Not directly consumable by Swift | Bridge collects on Main, passes snapshots via closures |
| `sealed class/interface` | **HIGH** | Exports as protocol hierarchy, hard to switch/match | Use status strings in snapshots (e.g., `BarcodeResolutionSnapshot.type`) |
| `Map<K,V>` with non-String keys | **HIGH** | Swift dictionary key compatibility issues | Flatten to arrays or use `Map<String, V>` with string keys |
| Default parameters | **HIGH** | Not exported as overloads | Make all bridge params explicit |
| `suspend fun` | **HIGH** | Coroutine bridging complex | Use callback-based APIs, avoid exposing suspend to Swift |
| `@JvmName`, expect/actual | **MEDIUM** | Can cause naming issues | Check exported names |
| Generic types | **MEDIUM** | Some generics problematic | Use concrete types or Any in snapshots |
| Nullable primitives (`Double?`) | **MEDIUM** | Becomes `KotlinDouble?` | Handle carefully, prefer non-null with defaults |
| `Long` subscription IDs | **MEDIUM** | Type mapping | Use `Int64` in Swift |
| Function types in data classes | **HIGH** | Not exportable well | Avoid lambdas in snapshots, only in bridge methods |
| `object` singletons | **LOW** | Usually fine | `NutriLogRoutes` is object - accessible as `NutriLogRoutesKt` or via properties |
| Data classes | **LOW** | Generally fine | Test export |

### Runtime/API Risks

| Component | Risk Level | Issue | Mitigation |
|-----------|------------|-------|------------|
| **AddFoodViewModel lifecycle** | **HIGH** | Must outlive both steps, reset only on new sessionId | Bridge must own it like AppDependencies, generate sessionId correctly |
| **ProductDetail route-scoped VM** | **HIGH** | Must be created per route, not shared | Bridge factory creates with new scope, Swift disposes on back |
| **Barcode resolution** | **HIGH** | Complex flow with 3 states, needs UI navigation | Bridge resolves in shared, Swift handles navigation based on snapshot |
| **Coach premium gating** | **HIGH** | Must match Android logic (unlocked OR debug) | Swift checks both; debug build detection needed |
| **StoreKit premium mapping** | **HIGH** | Must use `nutrilog_pro` code, not per-food pattern | Check StoreKitPurchaseClient mapping, may need updates |
| **Assistant config** | **HIGH** | Needs AI gateway URL from config | Add to Info.plist/build config, pass to bridge |
| **Camera permissions** | **HIGH** | Missing in Info.plist | Add NSCameraUsageDescription before testing scanner |
| **Main thread callbacks** | **HIGH** | SwiftUI needs MainActor updates | Bridge uses Dispatchers.Main, Swift uses @MainActor |
| **Memory leaks** | **HIGH** | Subscriptions not cancelled | Swift must cancel all subscriptions in deinit, bridge tracks jobs |
| **AddFood confirm navigation** | **MEDIUM** | Android navigates back after confirm; iOS needs same | Swift calls confirm() then navigates back, or bridge signals completion |
| **Weekly summary timing** | **MEDIUM** | May need refresh | Observe state, trigger refresh if needed |
| **Goal updates** | **MEDIUM** | Need to update diary state | Call bridge method, observe diary state |

### iOS-Specific Risks

| Issue | Risk Level | Mitigation |
|-------|------------|------------|
| **No test target in Xcode project** | **MEDIUM** | Adding tests requires project changes (PBXProject modifications) |
| **Synchronized root group** | **LOW** | New files under `iosApp/iosApp` auto-included (confirmed) |
| **Kotlin/Native framework linking** | **MEDIUM** | Must rebuild Shared framework after bridge changes |
| **Swift 6 concurrency** | **MEDIUM** | Use @MainActor, Sendable where needed |
| **NavigationPath with complex types** | **MEDIUM** | Use Hashable route enum or strings |

---

## Test Strategy

### Shared/Kotlin Tests (commonTest/iosTest)
**Location:** `shared/src/commonTest/`, `shared/src/iosTest/`

Focus on bridge logic if added there:
- Subscription lifecycle
- Snapshot conversion correctness
- State mapping
- No business logic in bridge (business logic stays in VMs/repositories)

### Android Tests (Reference)
Use Android behavior as oracle for parity testing.

### iOS Unit Tests
**Challenge:** No test target exists in project. Options:
1. Add test target to Xcode project (requires PBXProject edits)
2. Test via SwiftUI previews
3. Use XCTest in new target

**Recommendation:** Create `iosApp/iosAppTests` target. Files can be added but target membership needs project config.

**Tests to write:**
- Bridge subscription/cancel/close lifecycle
- Snapshot mapping
- Navigation route parsing
- ViewModel state transitions
- Premium gating logic

### Integration Tests
- Full flow: Add food from search → quantity → confirm
- Barcode scan flow (all 3 resolutions)
- Coach flow with consent/proposals
- Premium purchase flow (mock StoreKit)
- Save/unsave product
- Diary entry CRUD

### Manual Testing Checklist
- [ ] Tab navigation works
- [ ] Today screen shows diary, add food per meal
- [ ] Add food flow: search → select → quantity → confirm → back to today
- [ ] Add food from library
- [ ] Add food with preselected meal/product
- [ ] Barcode scanner (camera permission, all resolutions)
- [ ] Product detail route-scoped, save/unsave
- [ ] Library screen
- [ ] Weekly summary
- [ ] More screen: goals, premium, privacy, coach
- [ ] Coach modal: not configured, consent required, ready states
- [ ] Premium: buy, restore
- [ ] Meal sheet modal
- [ ] Back navigation
- [ ] State persistence across navigation
- [ ] App lifecycle (background/foreground)
- [ ] No business logic duplication in Swift

### Parity Validation
Compare with Android:
- Same screens, same flows
- Same route behavior
- Same coach gating logic
- Same add food behavior
- Same barcode resolution
- Shared runtime = same data

---

## Implementation Checklist

### Phase 1: Bridge & Core (Kotlin) - Agent 1
- [ ] Create `IosAppBridge.kt` with full API
- [ ] Define all snapshot DTOs
- [ ] Wire all ViewModels like AppDependencies
- [ ] Add barcode resolver
- [ ] Add route-scoped product detail handle
- [ ] Add factory `createIosAppBridge(aiGatewayBaseUrl:)`
- [ ] Update to pass aiGatewayBaseUrl to runtime
- [ ] Implement subscription management
- [ ] Implement clearAll() and close()
- [ ] Test Kotlin compilation for iosMain

### Phase 2: Platform Config (Kotlin) - Agent 2
- [ ] Review/update PlatformDataFactories if needed
- [ ] Ensure all dependencies available
- [ ] Check exports

### Phase 3: App Shell & Navigation (Swift) - Agent 3
- [ ] Create AppRoot
- [ ] Create AppNavigator
- [ ] Update/create NutriLogApp @main
- [ ] Update ContentView with TabView
- [ ] Create Route enum
- [ ] Implement navigation
- [ ] Add modal presenters

### Phase 4: Core Screens (Swift) - Agents 4,6
- [ ] TodayView + ViewModel
- [ ] LibraryView + ViewModel
- [ ] ProductDetailView + ViewModel (route-scoped)
- [ ] WeeklyView + ViewModel

### Phase 5: Add Food Flow (Swift) - Agent 5
- [ ] AddFoodFlowViewModel
- [ ] AddFoodSourceView
- [ ] AddFoodQuantityView
- [ ] MealPickerSheet
- [ ] Navigation integration

### Phase 6: More/Coach/Premium (Swift) - Agent 7
- [ ] MoreView + ViewModel
- [ ] CoachView + ViewModel (modal)
- [ ] PremiumPurchaseSection
- [ ] Gating logic

### Phase 7: Scanner & Polish (Swift) - Agents 8,9
- [ ] BarcodeScannerView with AVFoundation
- [ ] Camera permissions
- [ ] Info.plist updates
- [ ] AI gateway URL config
- [ ] StoreKit premium mapping check
- [ ] UI polish
- [ ] Error handling

### Phase 8: Testing & Validation
- [ ] Build Shared framework
- [ ] Build iOS app
- [ ] Manual parity testing
- [ ] Fix interop issues
- [ ] Memory leak check
- [ ] Subscription cleanup verification

---

## References

### ADRs
- ADR-0013: StoreKit bridge pattern
- ADR-0015: Composition root, stateless screens
- ADR-0016: AI gateway configuration
- ADR-0019: Navigation model

### Key Files
- `shared/src/iosMain/kotlin/com/example/nutricionalregister/IosNutriLogBridge.kt` - current bridge
- `core/data/src/commonMain/kotlin/com/example/nutricionalregister/data/ProductSearchRuntime.kt` - runtime
- `androidApp/src/main/kotlin/com/example/nutricionalregister/AppDependencies.kt` - composition root reference
- `androidApp/src/main/kotlin/com/example/nutricionalregister/App.kt` - app wiring reference
- `core/domain/src/commonMain/kotlin/com/example/nutricionalregister/domain/navigation/NutriLogRoutes.kt` - routes
- `iosApp/iosApp/StoreKitPurchaseClient.swift` - StoreKit impl
- `iosApp/iosApp/NutriLogViewModel.swift` - current VM (to replace)

### Feature Modules (Shared)
- `feature/diary` - DiaryViewModel, AddFoodViewModel
- `feature/search` - ProductSearchViewModel, ProductDetailViewModel
- `feature/savedproducts` - SavedProductsViewModel, PremiumPurchaseViewModel
- `feature/summary` - SummaryViewModel
- `feature/assistant` - CoachViewModel
- `feature/scan` - BarcodeResolver

---

**END OF DESIGN DOCUMENT**

*This document provides a complete, actionable design for implementing iOS parity. No files have been modified as requested.*