import SwiftUI
import ComposeApp
import FirebaseCore
import FirebaseAnalytics
import FirebaseCrashlytics

@main
struct iOSApp: App {

    init() {
        // Firebase n'est configuré que si le GoogleService-Info.plist est
        // présent dans le bundle (injecté par la CI depuis un secret, jamais
        // commité) : sans lui, l'app reste pleinement fonctionnelle, les
        // appels Kotlin retombent sur des no-op via FirebaseIosBridge.
        //
        // On valide le CONTENU et pas seulement la présence : FirebaseApp
        // .configure() lève une NSException non rattrapable en Swift sur un
        // plist illisible ou incomplet. Un secret CI mal encodé ferait alors
        // crasher 100 % des lancements, avant même que Crashlytics ne puisse
        // le rapporter.
        if let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
           let options = FirebaseOptions(contentsOfFile: path) {
            FirebaseApp.configure(options: options)
            Self.bindFirebaseBridge()
        }
    }

    /// Remplit le registre Kotlin (FirebaseIosBridge) avec des closures qui
    /// délèguent aux SDK Firebase — seul point de contact Swift ↔ Firebase.
    private static func bindFirebaseBridge() {
        let bridge = FirebaseIosBridge.shared

        bridge.logEvent = { name, params in
            Analytics.logEvent(name, parameters: params)
        }
        bridge.setCollectionEnabled = { enabled in
            Analytics.setAnalyticsCollectionEnabled(enabled.boolValue)
        }
        bridge.recordError = { context, message in
            // Crashlytics regroupe les non-fatales par (domain, code) : un
            // domaine constant ferait tomber les 24 sites d'appel Kotlin dans
            // une seule issue illisible. Le contexte entre donc dans le
            // domaine, ce qui donne une issue par site.
            let error = NSError(
                domain: "cg.mibeko.app.kotlin.\(context)",
                code: 0,
                userInfo: [NSLocalizedDescriptionKey: message]
            )
            // log() est attaché au rapport courant (pas d'écrasement entre
            // threads, contrairement à setCustomValue sur l'instance globale).
            Crashlytics.crashlytics().log("[\(context)] \(message)")
            Crashlytics.crashlytics().record(error: error)
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Transmet les deep links (mibeko:// et liens universels)
                    // à la navigation Compose.
                    ExternalUriHandler.shared.onNewUri(uri: url.absoluteString)
                }
        }
    }
}
