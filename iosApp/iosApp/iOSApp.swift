import SwiftUI
import FirebaseCore
import GoogleMobileAds
import AppTrackingTransparency
import AdSupport
import UserMessagingPlatform
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate {

    /// Guards against calling MobileAds.shared.start() more than once.
    private var mobileAdsStarted = false

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        return true
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Re-check consent status on every foreground (required by Google UMP policy).
        // MobileAds.start() is guarded by mobileAdsStarted so it only fires once.
        gatherConsentThenStartAds()
    }

    // MARK: - UMP consent → ATT → MobileAds

    private func gatherConsentThenStartAds() {
        let params = RequestParameters()
        params.isTaggedForUnderAgeOfConsent = false

        ConsentInformation.shared.requestConsentInfoUpdate(with: params) { [weak self] _ in
            guard let self else { return }

            // Resolve the root view controller for presenting the consent form.
            let rootVC = UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .flatMap { $0.windows }
                .first { $0.isKeyWindow }?.rootViewController

            DispatchQueue.main.async {
                // loadAndPresentIfRequired is a no-op when consent is not required
                // or has already been obtained — safe to call on every foreground.
                ConsentForm.loadAndPresentIfRequired(from: rootVC) { [weak self] _ in
                    guard let self else { return }
                    // Consent form dismissed (or wasn't needed) — now safe to request ATT.
                    self.requestATT()
                    self.startMobileAdsIfAllowed()
                }

                // For users where consent is already obtained / not required,
                // canRequestAds is true immediately after requestConsentInfoUpdate.
                self.startMobileAdsIfAllowed()
            }
        }

        // Handle users whose consent was already gathered in a prior session
        // (canRequestAds is already true before the async callback returns).
        startMobileAdsIfAllowed()
    }

    private func startMobileAdsIfAllowed() {
        guard !mobileAdsStarted,
              ConsentInformation.shared.canRequestAds else { return }
        mobileAdsStarted = true
        MobileAds.shared.requestConfiguration.maxAdContentRating = .general
        MobileAds.shared.start(completionHandler: nil)
    }

    private func requestATT() {
        if #available(iOS 14, *) {
            // The system shows this dialog only once; subsequent calls return the
            // cached authorisation status. No guard needed.
            ATTrackingManager.requestTrackingAuthorization { _ in
                // AdMob reads the authorisation status internally — no action needed.
            }
        }
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    init() {
        MainViewControllerKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
