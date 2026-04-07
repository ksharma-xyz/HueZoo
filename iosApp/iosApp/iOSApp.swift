import SwiftUI
import FirebaseCore
import GoogleMobileAds
import AppTrackingTransparency
import AdSupport
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        MobileAds.shared.requestConfiguration.maxAdContentRating = .general
        MobileAds.shared.start(completionHandler: nil)
        return true
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Request ATT permission once the app is fully active.
        // AdMob requires IDFA access to serve personalised ads;
        // without this dialog the fill rate on iOS 14+ is near zero.
        if #available(iOS 14, *) {
            ATTrackingManager.requestTrackingAuthorization { _ in
                // No action needed — AdMob reads the authorisation status internally.
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
