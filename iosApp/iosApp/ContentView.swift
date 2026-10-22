import SwiftUI
import UIKit
import ComposeApp

/// Hosts the shared Compose Multiplatform UI (`MainViewController` in composeApp/iosMain).
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        // Compose handles safe-area insets and the keyboard itself.
        ComposeView().ignoresSafeArea()
    }
}
