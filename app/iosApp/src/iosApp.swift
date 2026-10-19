import SwiftUI
import KotlinModules

struct ComposeView: UIViewControllerRepresentable {
    /// Called with true when the status bar should be hidden (the visitor is reading a project).
    var onStatusBarHiddenChanged: (Bool) -> Void

    func makeUIViewController(context: Context) -> some UIViewController {
        ViewControllerKt.ViewController(onSystemBarsVisibleChanged: { visible in
            onStatusBarHiddenChanged(!visible.boolValue)
        })
    }
    func updateUIViewController(_ uiViewController: UIViewControllerType, context: Context) {}
}

struct ContentView: View {
    @State private var statusBarHidden = false

    var body: some View {
        ComposeView(onStatusBarHiddenChanged: { hidden in
            withAnimation { statusBarHidden = hidden }
        })
        .ignoresSafeArea()
        .statusBarHidden(statusBarHidden)
    }
}

@main
struct iosApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
