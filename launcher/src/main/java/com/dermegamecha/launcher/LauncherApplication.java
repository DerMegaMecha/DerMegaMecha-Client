package com.dermegamecha.launcher;

import com.dermegamecha.launcher.auth.MicrosoftAuthService;
import com.dermegamecha.launcher.auth.SignedInAccount;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.nio.file.Path;

public final class LauncherApplication extends Application {
    private final MicrosoftAuthService auth = new MicrosoftAuthService(Path.of(System.getProperty("user.home"), "AppData", "Roaming", "DerMegaMechaLauncher"));
    private Label account = new Label("Not signed in");
    private Label status = new Label("Sign in with Microsoft to verify Minecraft ownership.");
    private Button login;
    private Button play;

    @Override public void start(Stage stage) {
        Label brand = new Label("DERMEGAMECHA"); brand.getStyleClass().add("brand");
        Label edition = new Label("LAUNCHER  /  1.0.0"); edition.getStyleClass().add("eyebrow");
        VBox header = new VBox(7, brand, edition); header.getStyleClass().add("header");

        Label title = new Label("Your Minecraft.\nYour Performance.\nYour Experience."); title.getStyleClass().add("hero");
        Label subtitle = new Label("Minecraft Java Edition 1.21.1  ·  Forge 52.x  ·  Java 21"); subtitle.getStyleClass().add("subtitle");
        VBox hero = new VBox(16, title, subtitle); hero.setPadding(new Insets(38, 40, 30, 40));

        Label profileLabel = new Label("SELECTED PROFILE"); profileLabel.getStyleClass().add("eyebrow");
        Label profile = new Label("DerMegaMecha PvP"); profile.getStyleClass().add("profile-name");
        Label state = new Label("INSTALLATION NOT VERIFIED"); state.getStyleClass().add("pill");
        VBox details = new VBox(10, profileLabel, profile, state); details.getStyleClass().add("card");
        details.setPadding(new Insets(22));

        account.getStyleClass().add("account");
        status.getStyleClass().add("status"); status.setWrapText(true);
        login = new Button("Connect Microsoft account"); login.getStyleClass().add("secondary-button"); login.setOnAction(e -> signIn());
        play = new Button("INSTALLATION NOT READY"); play.getStyleClass().add("play-button"); play.setDisable(true);
        Label unavailable = new Label("Play will become available after authenticated installation and file verification are implemented.");
        unavailable.getStyleClass().add("subtle"); unavailable.setWrapText(true);
        VBox accountCard = new VBox(12, new Label("ACCOUNT"), account, login, status); accountCard.getStyleClass().add("card"); accountCard.setPadding(new Insets(22));
        VBox bottom = new VBox(18, details, accountCard, play, unavailable); bottom.setPadding(new Insets(0, 40, 32, 40));

        HBox nav = new HBox(12, navButton("HOME"), navButton("MODS"), navButton("PROFILES"), navButton("SETTINGS")); nav.getStyleClass().add("nav"); nav.setAlignment(Pos.CENTER_LEFT); nav.setPadding(new Insets(0, 40, 20, 40));
        BorderPane root = new BorderPane(); root.getStyleClass().add("root"); root.setTop(header); root.setCenter(new VBox(hero, bottom)); root.setBottom(nav);
        Scene scene = new Scene(root, 920, 760); scene.getStylesheets().add(getClass().getResource("/styles/launcher.css").toExternalForm());
        stage.setTitle("DerMegaMecha Launcher"); stage.setMinWidth(780); stage.setMinHeight(680); stage.setScene(scene); stage.show();
        restoreAccount();
    }

    private Button navButton(String text) { Button b = new Button(text); b.getStyleClass().add("nav-button"); b.setDisable(!text.equals("HOME")); return b; }

    private void signIn() {
        login.setDisable(true); status.setText("Opening Microsoft sign-in in your system browser…");
        Task<SignedInAccount> task = new Task<>() { @Override protected SignedInAccount call() throws Exception { return auth.signIn(Desktop.getDesktop()); } };
        task.setOnSucceeded(e -> showAccount(task.getValue()));
        task.setOnFailed(e -> { status.setText(message(task.getException())); login.setDisable(false); });
        Thread thread = new Thread(task, "dermegamecha-auth"); thread.setDaemon(true); thread.start();
    }

    private void restoreAccount() {
        Task<SignedInAccount> task = new Task<>() { @Override protected SignedInAccount call() throws Exception { return auth.restore(); } };
        task.setOnSucceeded(e -> { if (task.getValue() != null) showAccount(task.getValue()); });
        task.setOnFailed(e -> status.setText("Saved sign-in could not be refreshed. Sign in again."));
        Thread thread = new Thread(task, "dermegamecha-token-refresh"); thread.setDaemon(true); thread.start();
    }

    private void showAccount(SignedInAccount value) {
        account.setText(value.name() + "  ·  " + value.id());
        status.setText("Microsoft account authenticated. Minecraft ownership and profile verified.");
        login.setText("Switch Microsoft account"); login.setDisable(false);
    }

    private String message(Throwable t) { return t == null || t.getMessage() == null ? "Sign-in failed. Check your connection and launcher setup." : t.getMessage(); }

    @Override public void stop() { Platform.exit(); }
    public static void main(String[] args) { launch(args); }
}
