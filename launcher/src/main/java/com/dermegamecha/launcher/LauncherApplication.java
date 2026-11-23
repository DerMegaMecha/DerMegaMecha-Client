package com.dermegamecha.launcher;

import com.dermegamecha.launcher.auth.MicrosoftAuthService;
import com.dermegamecha.launcher.auth.SignedInAccount;
import com.dermegamecha.launcher.core.LauncherPaths;
import com.dermegamecha.launcher.ui.UiPreferences;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;

/** Touch-friendly, localized home screen. Long-running account work is always off the FX thread. */
public final class LauncherApplication extends Application {
    private final LauncherPaths paths = new LauncherPaths();
    private final MicrosoftAuthService auth = new MicrosoftAuthService(paths.launcher.resolve("config"));
    private Stage stage;
    private Label greeting, account, accountStatus, installStatus, actionHint;
    private Button accountButton, mainAction;
    private VBox root;
    private SignedInAccount signedIn;
    private boolean working;
    private ProgressIndicator spinner;

    @Override public void start(Stage primaryStage) {
        stage = primaryStage;
        buildScreen();
        stage.setTitle("DerMegaMecha Launcher");
        stage.setMinWidth(360); stage.setMinHeight(560);
        stage.setWidth(760); stage.setHeight(700);
        stage.setScene(new Scene(root));
        applyStyle();
        stage.show();
        restoreAccount();
    }

    private void buildScreen() {
        Label logo = new Label("◆  DERMEGAMECHA"); logo.getStyleClass().add("logo");
        Label edition = new Label("LAUNCHER  ·  1.0.0"); edition.getStyleClass().add("eyebrow");
        HBox top = new HBox(12, logo, new Region(), edition); HBox.setHgrow(top.getChildren().get(1), Priority.ALWAYS);
        top.setAlignment(Pos.CENTER_LEFT); top.getStyleClass().add("topbar");

        greeting = new Label(); greeting.getStyleClass().add("welcome");
        Label subtitle = new Label(UiPreferences.text("Dein Spiel. Einfach startklar.", "Your game. Ready when you are.")); subtitle.getStyleClass().add("subtitle");
        VBox hero = new VBox(10, greeting, subtitle); hero.setPadding(new Insets(30, 0, 20, 0));

        account = new Label(); account.getStyleClass().add("account-name");
        accountStatus = new Label(); accountStatus.getStyleClass().add("body-copy"); accountStatus.setWrapText(true);
        accountButton = new Button(); accountButton.getStyleClass().add("secondary-button"); accountButton.setOnAction(e -> signIn());
        VBox accountCard = card(UiPreferences.text("MICROSOFT-KONTO", "MICROSOFT ACCOUNT"), account, accountStatus, accountButton);

        Label version = new Label("Minecraft Java 1.21.1  ·  Forge 52.x"); version.getStyleClass().add("version");
        installStatus = new Label(); installStatus.getStyleClass().add("pill");
        VBox gameCard = card(UiPreferences.text("DEIN SPIEL", "YOUR GAME"), version, installStatus);

        mainAction = new Button(); mainAction.getStyleClass().add("play-button"); mainAction.setMaxWidth(Double.MAX_VALUE);
        mainAction.setOnAction(e -> explainSetupStatus());
        actionHint = new Label(); actionHint.getStyleClass().add("hint"); actionHint.setWrapText(true);
        spinner = new ProgressIndicator(); spinner.setMaxSize(28, 28); spinner.setVisible(false); spinner.setManaged(false);

        Label accessTitle = new Label(UiPreferences.text("Einfache Einstellungen", "Simple settings")); accessTitle.getStyleClass().add("section-title");
        Button language = new Button(); language.getStyleClass().add("small-button"); language.setOnAction(e -> { UiPreferences.setEnglish(!UiPreferences.english()); rebuild(); });
        Button contrast = new Button(); contrast.getStyleClass().add("small-button"); contrast.setOnAction(e -> { UiPreferences.setHighContrast(!UiPreferences.highContrast()); applyStyle(); updateText(); });
        HBox accessibility = new HBox(10, language, contrast); accessibility.setAlignment(Pos.CENTER_LEFT);
        Button openFolder = new Button(UiPreferences.text("Installationsordner öffnen", "Open game folder")); openFolder.getStyleClass().add("small-button");
        openFolder.setOnAction(e -> openFolder());
        Button diagnostics = new Button(UiPreferences.text("Hilfe & technische Infos", "Help & technical details")); diagnostics.getStyleClass().add("link-button");
        diagnostics.setOnAction(e -> showHelp());
        VBox footer = new VBox(12, accessTitle, accessibility, openFolder, diagnostics);

        VBox content = new VBox(16, hero, accountCard, gameCard, spinner, mainAction, actionHint, footer);
        content.setPadding(new Insets(0, 32, 24, 32)); content.setMaxWidth(620);
        root = new VBox(top, new Region(), content); VBox.setVgrow(root.getChildren().get(1), Priority.ALWAYS);
        root.setPadding(new Insets(20)); root.getStyleClass().add("app-root");
        root.setAlignment(Pos.TOP_CENTER);
        // Reorder content naturally for small phone-sized windows; the primary button remains prominent.
        updateText();
    }

    private VBox card(String heading, javafx.scene.Node... nodes) {
        Label title = new Label(heading); title.getStyleClass().add("eyebrow");
        VBox box = new VBox(10); box.getChildren().add(title); box.getChildren().addAll(nodes);
        box.setPadding(new Insets(20)); box.getStyleClass().add("card"); return box;
    }

    private void updateText() {
        greeting.setText(UiPreferences.text("Willkommen bei DerMegaMecha!", "Welcome to DerMegaMecha!"));
        if (signedIn == null) {
            account.setText(UiPreferences.text("Noch nicht angemeldet", "Not signed in yet"));
            accountStatus.setText(UiPreferences.text("Melde dich mit deinem Microsoft-Konto an, um Minecraft zu spielen.", "Sign in with your Microsoft account to play Minecraft."));
            accountButton.setText(UiPreferences.text("Mit Microsoft anmelden", "Sign in with Microsoft"));
            accountButton.setDisable(working);
        } else {
            account.setText(signedIn.name());
            accountStatus.setText(UiPreferences.text("Konto verbunden. Minecraft-Besitz wurde bestätigt.", "Account connected. Minecraft ownership is verified."));
            accountButton.setText(UiPreferences.text("Konto wechseln", "Switch account"));
            accountButton.setDisable(working);
        }
        installStatus.setText(UiPreferences.text("Einrichtung noch nicht abgeschlossen", "Setup is not complete yet"));
        mainAction.setText(UiPreferences.text("MINECRAFT EINRICHTEN", "SET UP MINECRAFT"));
        mainAction.setDisable(working || signedIn == null);
        actionHint.setText(signedIn == null
                ? UiPreferences.text("Melde dich zuerst an. Danach helfen wir dir bei der Einrichtung.", "Sign in first. Then we can help prepare the game.")
                : UiPreferences.text("Die automatische Einrichtung und der Spielstart werden noch fertiggestellt. Der Button startet das Spiel erst, wenn alle Dateien geprüft sind.", "Automatic setup and game launch are still being completed. Play will only be enabled after all game files are verified."));
        for (var node : ((HBox)((VBox)root.getChildren().get(2)).getChildren().get(5)).getChildren()) { /* layout-safe no-op */ }
        Button language = (Button)((HBox)((VBox)root.getChildren().get(2)).getChildren().get(7).getChildren().get(1)).getChildren().get(0);
        language.setText(UiPreferences.text("English", "Deutsch"));
        Button contrast = (Button)((HBox)((VBox)root.getChildren().get(2)).getChildren().get(7).getChildren().get(1)).getChildren().get(1);
        contrast.setText(UiPreferences.text(UiPreferences.highContrast() ? "Hoher Kontrast: an" : "Hoher Kontrast: aus", UiPreferences.highContrast() ? "High contrast: on" : "High contrast: off"));
    }

    private void rebuild() {
        buildScreen(); stage.getScene().setRoot(root); applyStyle();
    }

    private void applyStyle() {
        boolean high = UiPreferences.highContrast();
        root.setStyle("-fx-font-family: 'Segoe UI', Arial; -fx-background-color: " + (high ? "#000000" : "#11151c") + "; -fx-font-size: " + (14 * UiPreferences.textScale()) + "px;");
        if (!stage.getScene().getStylesheets().contains(getClass().getResource("/styles/launcher.css").toExternalForm()))
            stage.getScene().getStylesheets().add(getClass().getResource("/styles/launcher.css").toExternalForm());
        root.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("contrast"), high);
    }

    private void restoreAccount() {
        runBackground(UiPreferences.text("Prüfe deine Anmeldung…", "Checking your sign-in…"), auth::restore,
                accountValue -> { if (accountValue != null) signedIn = accountValue; updateText(); });
    }

    private void signIn() {
        if (!Desktop.isDesktopSupported()) { showFriendlyError(UiPreferences.text("Dieser Computer kann die sichere Anmeldung nicht öffnen.", "This computer cannot open secure sign-in.")); return; }
        runBackground(UiPreferences.text("Microsoft-Anmeldung wird geöffnet…", "Opening Microsoft sign-in…"),
                () -> auth.signIn(Desktop.getDesktop()), value -> { signedIn = value; updateText(); });
    }

    private void runBackground(String message, Work<SignedInAccount> work, java.util.function.Consumer<SignedInAccount> success) {
        working = true; actionHint.setText(message); spinner.setVisible(true); spinner.setManaged(true); updateText();
        Task<SignedInAccount> task = new Task<>() { @Override protected SignedInAccount call() throws Exception { return work.run(); } };
        task.setOnSucceeded(e -> { working = false; spinner.setVisible(false); spinner.setManaged(false); success.accept(task.getValue()); });
        task.setOnFailed(e -> { working = false; spinner.setVisible(false); spinner.setManaged(false); updateText(); showFriendlyError(friendly(task.getException())); });
        Thread thread = new Thread(task, "dermegamecha-background-work"); thread.setDaemon(true); thread.start();
    }

    private String friendly(Throwable error) {
        String message = error == null ? "" : error.getMessage();
        if (message == null || message.isBlank()) return UiPreferences.text("Das hat nicht geklappt. Prüfe deine Internetverbindung und versuche es erneut.", "That did not work. Check your internet connection and try again.");
        String lower = message.toLowerCase();
        if (lower.contains("clientid") || lower.contains("app registration")) return UiPreferences.text("Die sichere Microsoft-Anmeldung ist noch nicht für diese App eingerichtet. Bitte versuche es später erneut.", "Secure Microsoft sign-in has not been configured for this app yet. Please try again later.");
        if (lower.contains("entitlement") || lower.contains("owns minecraft")) return UiPreferences.text("Dieses Konto besitzt Minecraft Java Edition nicht. Bitte wähle das richtige Microsoft-Konto.", "This account does not own Minecraft Java Edition. Please choose the correct Microsoft account.");
        if (lower.contains("cancel") || lower.contains("denied")) return UiPreferences.text("Anmeldung nicht abgeschlossen. Bitte versuche es erneut.", "Login unsuccessful. Please try again.");
        return UiPreferences.text("Das hat nicht geklappt. Bitte versuche es erneut. Wenn es weiter passiert, öffne Hilfe & technische Infos.", "Something went wrong. Please try again. If it keeps happening, open Help & technical details.");
    }

    private void showFriendlyError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR); alert.setTitle(UiPreferences.text("Anmeldung", "Sign-in"));
        alert.setHeaderText(UiPreferences.text("Das hat nicht geklappt", "That did not work")); alert.setContentText(message); alert.showAndWait();
    }

    private void explainSetupStatus() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(UiPreferences.text("Minecraft einrichten", "Set up Minecraft"));
        alert.setHeaderText(UiPreferences.text("Wir bereiten die einfache Einrichtung vor", "We are preparing the easy setup"));
        alert.setContentText(UiPreferences.text("Die Installation ist noch nicht bereit. Bitte installiere Minecraft nicht manuell in einem anderen Ordner – diese App wird dich informieren, sobald die automatische Einrichtung verfügbar ist.", "Installation is not ready yet. Please do not install Minecraft manually into another folder; this app will let you know when automatic setup is available."));
        alert.showAndWait();
    }

    private void openFolder() {
        try { Files.createDirectories(paths.defaultInstance()); if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(paths.defaultInstance().toFile()); }
        catch (Exception ex) { showFriendlyError(UiPreferences.text("Der Spielordner konnte nicht geöffnet werden.", "The game folder could not be opened.")); }
    }

    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(UiPreferences.text("Hilfe", "Help")); alert.setHeaderText(UiPreferences.text("Hilfe & technische Infos", "Help & technical details"));
        alert.setContentText(UiPreferences.text("Minecraft Java 1.21.1 · Forge 52.x\n\nAndroid: Minecraft Java ist dort nur über separate Community-Laufzeitumgebungen verfügbar. Forge 52, Java 21 und Grafik-Mods sind nicht verlässlich für Android bestätigt. Dieser Windows-Launcher kann nicht als Android-App verwendet werden.\n\nTechnische Protokolle: %s\nSpielordner: %s".formatted(paths.logs, paths.defaultInstance()), "Minecraft Java 1.21.1 · Forge 52.x\n\nAndroid: Minecraft Java is only available there through separate community runtimes. Forge 52, Java 21, and graphics mods are not reliably confirmed for Android. This Windows launcher cannot be used as an Android app.\n\nTechnical logs: %s\nGame folder: %s".formatted(paths.logs, paths.defaultInstance())));
        alert.showAndWait();
    }

    @FunctionalInterface private interface Work<T> { T run() throws Exception; }
    public static void main(String[] args) { launch(args); }
}
