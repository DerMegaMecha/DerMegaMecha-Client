package com.dermegamecha.launcher;

import com.dermegamecha.launcher.core.LauncherPaths;
import com.dermegamecha.launcher.integration.modpack.ModrinthPackService;
import com.dermegamecha.launcher.integration.prism.PrismLauncherIntegration;
import com.dermegamecha.launcher.ui.BlockWorldArtwork;
import com.dermegamecha.launcher.ui.UiPreferences;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class LauncherApplication extends Application {
    private static final String PRISM_DOWNLOAD_PAGE = "https://prismlauncher.org/download/";

    private final LauncherPaths paths = new LauncherPaths();
    private final PrismLauncherIntegration prism = new PrismLauncherIntegration();
    private final ModrinthPackService packs = new ModrinthPackService(paths.cache.resolve("modpack-validation"));
    private Stage stage;
    private BorderPane root;
    private VBox sidebar;
    private StackPane pageHost;
    private Label pageTitle;
    private Label headerPrismStatus;
    private Label headerBuild;
    private Label heroTitle;
    private Label heroMeta;
    private Label setupStatus;
    private Label launchSummary;
    private Label instanceSummary;
    private Label sidebarWordmark;
    private Label sidebarSubmark;
    private Label versionMetric;
    private Label versionDetail;
    private Label prismMetric;
    private Label prismDetail;
    private Label packMetric;
    private Label packDetail;
    private Label serverMetric;
    private Label serverDetail;
    private TextField prismPathField;
    private TextField instanceIdField;
    private ComboBox<String> profileChoice;
    private TextField serverNameField;
    private TextField serverAddressField;
    private TextField serverMinecraftVersionField;
    private TextField serverPackVersionField;
    private CheckBox importConfirmed;
    private Label profileDetails;
    private Label prismStatus;
    private Label packStatus;
    private Label compatibilityStatus;
    private Label activity;
    private Button downloadPackButton;
    private Button importPackButton;
    private Button playButton;
    private Button playPageButton;
    private Button joinButton;
    private Button joinPageButton;
    private Button setupAction;
    private ProgressIndicator spinner;
    private final Map<String, Node> pages = new LinkedHashMap<>();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private final List<Label> navCaptions = new ArrayList<>();
    private final List<Button> playButtons = new ArrayList<>();
    private final List<Button> joinButtons = new ArrayList<>();
    private final List<Button> prismOpenButtons = new ArrayList<>();
    private String previousProfile;
    private String currentPage = "home";
    private String prismStatusMessage = "";
    private boolean changingProfileProgrammatically;
    private boolean prismDetected;
    private String officialPackUrl;
    private boolean packConfigurationError;
    private ModrinthPackService.PackInfo loadedPack;
    private boolean working;

    @Override public void start(Stage primaryStage) {
        stage = primaryStage;
        try {
            officialPackUrl = ModrinthPackService.configuredPackUrl();
        } catch (IOException ex) {
            officialPackUrl = "";
            packConfigurationError = true;
        }
        buildScreen();
        stage.setTitle("DerMegaMecha Launcher");
        stage.setMinWidth(480);
        stage.setMinHeight(560);
        stage.setWidth(1240);
        stage.setHeight(850);
        stage.setScene(new Scene(root));
        applyStyle();
        stage.widthProperty().addListener((observable, oldValue, newValue) ->
                updateResponsiveLayout(newValue.doubleValue()));
        stage.show();
        updateResponsiveLayout(stage.getWidth());
        refreshPrismStatus();
    }

    private String friendlyFailure(Throwable error) {
        String details = error == null || error.getMessage() == null ? "" : error.getMessage();
        String normalized = details.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("prism launcher was not found") || normalized.contains("prism launcher executable")
                || normalized.contains("prismlauncher.exe")) {
            return UiPreferences.text("Prism Launcher wurde nicht gefunden. Installiere Prism oder wähle die richtige Anwendung aus.",
                    "Prism Launcher was not found. Install Prism or select the correct application.");
        }
        if (normalized.contains("hash") || normalized.contains("file-size mismatch")) {
            return UiPreferences.text("Eine Modpack-Datei stimmt nicht mit ihrer Prüfsumme überein. Lade das Pack erneut herunter oder wähle eine vertrauenswürdige Datei.",
                    "A modpack file did not match its published checksum. Download the pack again or choose a trusted file.");
        }
        if (normalized.contains("unsafe archive path") || normalized.contains("conflicting file and directory")) {
            return UiPreferences.text("Das Modpack enthält unsichere oder widersprüchliche Dateipfade und wurde abgelehnt.",
                    "The modpack contains unsafe or conflicting file paths and was rejected.");
        }
        if (normalized.contains("unsupported or insecure source")) {
            return UiPreferences.text("Das Modpack verweist auf eine nicht zugelassene Downloadquelle. Verwende ein offizielles Modrinth- oder unterstütztes Repository-Pack.",
                    "The modpack references an unapproved download source. Use an official Modrinth or supported repository pack.");
        }
        if (normalized.contains("manifest") || normalized.contains("modrinth modpack")) {
            return UiPreferences.text("Die ausgewählte Datei ist kein gültiges unterstütztes Modrinth-Modpack.",
                    "The selected file is not a valid supported Modrinth modpack.");
        }
        if (normalized.contains("http ") || normalized.contains("download") || normalized.contains("network")) {
            return UiPreferences.text("Das Modpack konnte nicht geladen werden. Prüfe deine Internetverbindung und versuche es erneut.",
                    "The modpack could not be downloaded. Check your internet connection and try again.");
        }
        if (normalized.contains("supports minecraft") || normalized.contains("supported loader")
                || normalized.contains("modpack format")) {
            return UiPreferences.text("Dieses Modpack verwendet eine Minecraft-Version oder einen Modloader, den diese Version des Launchers nicht unterstützt.",
                    "This pack uses a Minecraft version or mod loader that this launcher version does not support.");
        }
        if (normalized.contains("instance folder") || normalized.contains("instance id")) {
            return UiPreferences.text("Gib den tatsächlichen Instanzordnernamen aus Prism ein.",
                    "Enter the actual Prism instance folder name.");
        }
        if (normalized.contains("server address")) {
            return UiPreferences.text("Die Serveradresse ist ungültig. Verwende einen Hostnamen oder eine Adresse, optional mit :Port.",
                    "The server address is invalid. Use a host name or address, optionally followed by :port.");
        }
        if (normalized.contains("version") && normalized.contains("prism")) {
            return UiPreferences.text("Prism konnte nicht gestartet oder seine Version nicht ermittelt werden. Prüfe die ausgewählte Prism-Anwendung.",
                    "Prism could not start or return its version. Check the selected Prism application.");
        }
        if (normalized.contains("configured server requirements") || normalized.contains("server minecraft")
                || normalized.contains("server pack")) {
            return UiPreferences.text("Die eingetragenen Serverversionen passen nicht zum geprüften Modpack. Prüfe Minecraft- und Modpack-Version.",
                    "The configured server versions do not match the validated pack. Check the Minecraft and modpack versions.");
        }
        if (normalized.contains("confirm that you completed") || normalized.contains("selected prism profile")) {
            return UiPreferences.text("Schließe den Import in Prism ab und bestätige, dass Instanz, Minecraft- und Loader-Versionen zum Pack passen.",
                    "Finish the import in Prism and confirm that the instance, Minecraft, and loader versions match the pack.");
        }
        if (normalized.contains("prism could not be started") || normalized.contains("could not start prism")) {
            return UiPreferences.text("Prism konnte nicht gestartet werden. Prüfe die ausgewählte Prism-Installation.",
                    "Prism could not be started. Check the selected Prism installation.");
        }
        return UiPreferences.text("Der Vorgang konnte nicht abgeschlossen werden. Prüfe die Auswahl und versuche es erneut.",
                "The operation could not be completed. Check your selection and try again.");
    }

    private void buildScreen() {
        pages.clear();
        navButtons.clear();
        navCaptions.clear();
        playButtons.clear();
        joinButtons.clear();
        prismOpenButtons.clear();

        root = new BorderPane();
        root.getStyleClass().add("app-root");
        sidebar = buildSidebar();
        root.setLeft(sidebar);
        pageHost = new StackPane();
        pageHost.setAlignment(Pos.TOP_LEFT);
        pageHost.getStyleClass().add("page-host");
        ScrollPane scroll = new ScrollPane(pageHost);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("page-scroll");
        BorderPane content = new BorderPane();
        content.setTop(buildTopbar());
        content.setCenter(scroll);
        root.setCenter(content);

        spinner = new ProgressIndicator();
        spinner.setMaxSize(20, 20);
        spinner.setVisible(false);
        spinner.setManaged(false);
        activity = new Label(UiPreferences.text(
                "ANMELDUNG UND SPIELSTART ERFOLGEN IN PRISM",
                "AUTHENTICATION AND GAME START ARE HANDLED IN PRISM"));
        activity.getStyleClass().add("activity-copy");
        activity.setWrapText(true);
        HBox activityBar = new HBox(12, spinner, activity);
        activityBar.setAlignment(Pos.CENTER_LEFT);
        activityBar.getStyleClass().add("activity-bar");
        root.setBottom(activityBar);

        prismStatus = new Label();
        prismStatus.getStyleClass().add("status-copy");
        prismStatus.setWrapText(true);
        prismPathField = new TextField(UiPreferences.prismExecutable());
        prismPathField.setPromptText(UiPreferences.text("Pfad zu prismlauncher.exe (optional)", "Path to prismlauncher.exe (optional)"));
        prismPathField.textProperty().addListener((observable, oldValue, newValue) -> {
            prismStatusMessage = "";
            updateControls();
        });
        Button browsePrism = smallButton(UiPreferences.text("Datei auswählen", "Browse"));
        browsePrism.setOnAction(event -> browsePrism());
        Button installPrism = smallButton(UiPreferences.text("Prism installieren", "Install Prism"));
        installPrism.setOnAction(event -> openPrismDownload());
        Button openPrismSettings = smallButton(UiPreferences.text("Prism öffnen", "Open Prism"));
        openPrismSettings.setOnAction(event -> openPrism());
        prismOpenButtons.add(openPrismSettings);
        FlowPane prismActions = new FlowPane(10, 10, openPrismSettings, browsePrism, installPrism);
        prismActions.setAlignment(Pos.CENTER_LEFT);
        VBox prismCard = card(UiPreferences.text("PRISM LAUNCHER", "PRISM LAUNCHER"),
                prismStatus, prismPathField, prismActions,
                hint(UiPreferences.text("Melde dich in Prism an. DerMegaMecha liest keine Prism-Kontodaten und zeigt keinen Anmeldestatus an.",
                        "Sign in inside Prism. DerMegaMecha does not read Prism account data or display a sign-in status.")));
        prismCard.getChildren().get(prismCard.getChildren().size() - 1).getStyleClass().add("hint");

        profileChoice = new ComboBox<>();
        profileChoice.getItems().setAll("Forge 1.21.1", "Fabric 1.21.1");
        profileChoice.setValue("fabric".equals(UiPreferences.prismProfile()) ? "Fabric 1.21.1" : "Forge 1.21.1");
        previousProfile = selectedProfileKey();
        instanceIdField = new TextField(UiPreferences.prismInstance(previousProfile));
        instanceIdField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 100 ? change : null));
        instanceIdField.setPromptText(UiPreferences.text("Instanzordner-ID aus Prism", "Instance folder ID from Prism"));
        Label instanceHelp = new Label(UiPreferences.text(
                "Schließe den Import in Prism ab. Rechtsklick auf die Instanz → „Instanzordner“. Der Ordnername ist die ID. Öffne in Prism die Instanzeinstellungen und prüfe Minecraft- und Loader-Version; DerMegaMecha kann sie nicht auslesen.",
                "Finish the import in Prism. Right-click the instance → “Instance Folder”; its folder name is the ID. Check Minecraft and loader versions in Prism's instance settings; DerMegaMecha cannot read them."));
        instanceHelp.getStyleClass().add("hint");
        instanceHelp.setWrapText(true);
        profileDetails = new Label();
        profileDetails.getStyleClass().add("hint");
        profileDetails.setWrapText(true);
        profileChoice.setOnAction(event -> {
            if (changingProfileProgrammatically) return;
            if (previousProfile != null) UiPreferences.setPrismInstance(previousProfile, instanceIdField.getText());
            previousProfile = selectedProfileKey();
            UiPreferences.setPrismProfile(previousProfile);
            instanceIdField.setText(UiPreferences.prismInstance(previousProfile));
            clearImportConfirmation();
            updateControls();
        });
        instanceIdField.textProperty().addListener((observable, oldValue, newValue) -> {
            clearImportConfirmation();
            updateControls();
        });
        importConfirmed = new CheckBox(UiPreferences.text(
                "Ich habe den Import in Prism abgeschlossen und diese Instanz ausgewählt.",
                "I finished the import in Prism and selected this instance."));
        importConfirmed.setWrapText(true);
        importConfirmed.setSelected(false);
        importConfirmed.setOnAction(event -> {
            UiPreferences.setPrismImportConfirmed(importConfirmed.isSelected());
            updateControls();
        });
        Label importConfirmationHelp = new Label(UiPreferences.text(
                "Bestätige erst, wenn diese konkrete Prism-Instanz aus dem geprüften Pack importiert wurde und Minecraft- sowie Loader-Version exakt zu den Packdaten passen.",
                "Confirm only after this exact Prism instance was imported from the validated pack and its Minecraft and loader versions match the pack metadata."));
        importConfirmationHelp.getStyleClass().add("hint");
        importConfirmationHelp.setWrapText(true);
        VBox instanceCard = card(UiPreferences.text("MINECRAFT-INSTANZ AUSWÄHLEN", "SELECT MINECRAFT INSTANCE"),
                profileChoice, profileDetails, instanceIdField, instanceHelp, importConfirmed, importConfirmationHelp);

        packStatus = new Label();
        packStatus.getStyleClass().add("status-copy");
        packStatus.setWrapText(true);
        packStatus.setText(UiPreferences.text(
                "Noch kein Modrinth-Modpack geprüft. Wähle eine lokale .mrpack-Datei oder lade das konfigurierte offizielle Pack herunter.",
                "No Modrinth modpack has been validated yet. Choose a local .mrpack file or download the configured official pack."));
        if (packConfigurationError) {
            packStatus.setText(UiPreferences.text(
                    "Die Herausgeber-Konfiguration für den offiziellen Modpack-Link konnte nicht gelesen werden. Du kannst weiterhin eine lokale .mrpack-Datei wählen.",
                    "The publisher configuration for the official modpack link could not be read. You can still choose a local .mrpack file."));
        }
        importPackButton = new Button(UiPreferences.text("Modpack-Datei prüfen und importieren", "Validate and import pack file"));
        importPackButton.getStyleClass().add("secondary-button");
        importPackButton.setOnAction(event -> chooseAndImportPack());
        downloadPackButton = new Button(UiPreferences.text("Offizielles Modpack herunterladen", "Download official modpack"));
        downloadPackButton.getStyleClass().add("secondary-button");
        downloadPackButton.setOnAction(event -> downloadAndImportPack());
        FlowPane packActions = new FlowPane(10, 10, importPackButton, downloadPackButton);
        packActions.setAlignment(Pos.CENTER_LEFT);
        VBox packCard = card(UiPreferences.text("DERMEGAMECHA-MODPACK", "DERMEGAMECHA MODPACK"),
                packStatus, packActions,
                hint(UiPreferences.text("Die geprüfte Modrinth-Datei wird an Prism übergeben. Schließe den Import dort ab. Verwende nur vertrauenswürdige Packs: Mod-JARs können mit deinen Benutzerrechten Code ausführen.",
                        "The validated Modrinth archive is handed to Prism; finish the import there. Use trusted packs only: mod JARs can run code with your user permissions.")));
        packCard.getChildren().get(packCard.getChildren().size() - 1).getStyleClass().add("hint");

        serverNameField = new TextField(UiPreferences.serverName());
        serverNameField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 80 ? change : null));
        serverNameField.setPromptText(UiPreferences.text("Servername", "Server name"));
        serverAddressField = new TextField(UiPreferences.serverAddress());
        serverAddressField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 300 ? change : null));
        serverAddressField.setPromptText(UiPreferences.text("Adresse, z. B. play.example.net:25565", "Address, e.g. play.example.net:25565"));
        serverMinecraftVersionField = new TextField(UiPreferences.serverMinecraftVersion());
        serverMinecraftVersionField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 64 ? change : null));
        serverMinecraftVersionField.setPromptText(UiPreferences.text("Minecraft-Version laut Server", "Minecraft version required by server"));
        serverPackVersionField = new TextField(UiPreferences.serverPackVersion());
        serverPackVersionField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 128 ? change : null));
        serverPackVersionField.setPromptText(UiPreferences.text("Erforderliche Modpack-Version", "Required modpack version"));
        serverMinecraftVersionField.textProperty().addListener((observable, oldValue, newValue) -> updateControls());
        serverPackVersionField.textProperty().addListener((observable, oldValue, newValue) -> updateControls());
        serverAddressField.textProperty().addListener((observable, oldValue, newValue) -> updateControls());
        serverNameField.textProperty().addListener((observable, oldValue, newValue) -> updateControls());
        compatibilityStatus = new Label();
        compatibilityStatus.getStyleClass().add("hint");
        compatibilityStatus.setWrapText(true);
        VBox serverCard = card(UiPreferences.text("SERVER", "SERVER"),
                serverNameField, serverAddressField, serverMinecraftVersionField, serverPackVersionField,
                compatibilityStatus);

        StackPane hero = createHeroPanel();

        playPageButton = createPlayButton();
        playButtons.add(playPageButton);
        joinButton = createJoinButton();
        joinButtons.add(joinButton);
        joinPageButton = createJoinButton();
        joinButtons.add(joinPageButton);
        launchSummary = new Label();
        launchSummary.getStyleClass().add("launch-summary");
        launchSummary.setWrapText(true);
        VBox launchCard = card(UiPreferences.text("STARTBEREIT?", "READY TO PLAY?"),
                launchSummary, playPageButton, joinPageButton,
                hint(UiPreferences.text("Prism übernimmt Anmeldung und Spielstart. DerMegaMecha bestätigt weder Anmeldung noch tatsächlichen Spielstart.",
                        "Prism handles sign-in and game execution. DerMegaMecha does not confirm authentication or actual game startup.")));

        versionMetric = metricValue();
        versionDetail = metricDetail();
        prismMetric = metricValue();
        prismDetail = metricDetail();
        packMetric = metricValue();
        packDetail = metricDetail();
        serverMetric = metricValue();
        serverDetail = metricDetail();
        Button openPrismHome = smallButton(UiPreferences.text("PRISM ÖFFNEN", "OPEN PRISM"));
        openPrismHome.setOnAction(event -> openPrism());
        prismOpenButtons.add(openPrismHome);
        Button openPackPage = smallButton(UiPreferences.text("MODPACKS", "MODPACKS"));
        openPackPage.setOnAction(event -> showPage("modpacks"));
        Button openInstancesPage = smallButton(UiPreferences.text("INSTANZEN", "INSTANCES"));
        openInstancesPage.setOnAction(event -> showPage("instances"));
        VBox serverMetricCard = metricCard("⌁", "SERVER", serverMetric, serverDetail, joinButton);
        FlowPane dashboardCards = new FlowPane(14, 14,
                metricCard("◈", "GAME VERSION", versionMetric, versionDetail, openInstancesPage),
                metricCard("◉", "PRISM LAUNCHER", prismMetric, prismDetail, openPrismHome),
                metricCard("▧", "MODPACK", packMetric, packDetail, openPackPage),
                serverMetricCard);
        dashboardCards.getStyleClass().add("dashboard-cards");

        VBox homePage = new VBox(18, hero, dashboardCards);
        homePage.getStyleClass().add("page-container");
        VBox playPage = page(
                UiPreferences.text("LAUNCH CONTROL", "LAUNCH CONTROL"),
                UiPreferences.text("Dein Spielstart", "Your game launch"),
                UiPreferences.text("Wähle die bestätigte Prism-Instanz und prüfe die Pack-Version. Prism übernimmt die Microsoft-Anmeldung.",
                        "Choose the confirmed Prism instance and check the pack version. Prism handles Microsoft sign-in."),
                launchCard, instanceSummaryCard(), serverCard);
        VBox modpacksPage = page(
                UiPreferences.text("CONTENT BAY", "CONTENT BAY"),
                UiPreferences.text("Modpack verwalten", "Manage modpack"),
                UiPreferences.text("Prüfe ein lokales Modrinth-Pack oder lade das vom Herausgeber konfigurierte Pack herunter.",
                        "Validate a local Modrinth pack or download the publisher-configured pack."),
                packCard);
        VBox instancesPage = page(
                UiPreferences.text("INSTANCE CONTROL", "INSTANCE CONTROL"),
                UiPreferences.text("Minecraft-Instanz", "Minecraft instance"),
                UiPreferences.text("Wähle in Prism die importierte Instanz. Der Ordnername ist ihre ID; Versionen werden dort manuell geprüft.",
                        "Select the imported instance in Prism. Its folder name is the ID; verify versions there manually."),
                instanceCard);
        Button saveSettings = smallButton(UiPreferences.text("EINSTELLUNGEN SPEICHERN", "SAVE SETTINGS"));
        saveSettings.getStyleClass().add("primary-action");
        saveSettings.setOnAction(event -> saveSettings());
        Label settingsTitle = new Label(UiPreferences.text("SYSTEMEINSTELLUNGEN", "SYSTEM SETTINGS"));
        settingsTitle.getStyleClass().add("section-title");
        Button languageButton = smallButton(UiPreferences.text("English", "Deutsch"));
        languageButton.setOnAction(event -> {
            saveForm();
            UiPreferences.setEnglish(!UiPreferences.english());
            rebuild();
        });
        Button contrastButton = smallButton(UiPreferences.text(
                UiPreferences.highContrast() ? "Hoher Kontrast: an" : "Hoher Kontrast: aus",
                UiPreferences.highContrast() ? "High contrast: on" : "High contrast: off"));
        contrastButton.setOnAction(event -> {
            UiPreferences.setHighContrast(!UiPreferences.highContrast());
            applyStyle();
        });
        HBox accessibility = new HBox(10, languageButton, contrastButton);
        accessibility.setAlignment(Pos.CENTER_LEFT);
        Button openFolder = smallButton(UiPreferences.text("Datenordner öffnen", "Open data folder"));
        openFolder.setOnAction(event -> openFolder(paths.root));
        Button help = new Button(UiPreferences.text("Hilfe & technische Infos", "Help & technical details"));
        help.getStyleClass().add("link-button");
        help.setOnAction(event -> showHelp());
        VBox settingsCard = card(settingsTitle.getText(), prismCard, accessibility, saveSettings, openFolder, help);
        Button aboutHelp = new Button(UiPreferences.text("Hilfe & technische Infos", "Help & technical details"));
        aboutHelp.getStyleClass().add("link-button");
        aboutHelp.setOnAction(event -> showHelp());
        VBox settingsPage = page(
                UiPreferences.text("PREFERENCES", "PREFERENCES"),
                UiPreferences.text("Einstellungen", "Settings"),
                UiPreferences.text("Prism-Pfad, Sprache und Darstellung. Microsoft-Anmeldung bleibt ausschließlich in Prism.",
                        "Prism path, language, and appearance. Microsoft sign-in remains exclusively in Prism."),
                settingsCard);
        VBox aboutCard = card("DERMEGAMECHA",
                hint(UiPreferences.text("Ein unabhängiger Launcher für Modpack-Verwaltung und Prism-Integration.",
                        "An independent launcher for modpack management and Prism integration.")),
                hint(UiPreferences.text("Prism übernimmt Anmeldung, Kontospeicherung und Minecraft-Ausführung. DerMegaMecha liest keine Zugangsdaten.",
                        "Prism handles sign-in, account storage, and Minecraft execution. DerMegaMecha does not access credentials.")),
                aboutHelp);
        VBox aboutPage = page(
                UiPreferences.text("SYSTEM INFO", "SYSTEM INFO"),
                UiPreferences.text("Über DerMegaMecha", "About DerMegaMecha"),
                UiPreferences.text("Dein Launcher. Deine Instanzen. Anmeldung ausschließlich in Prism.",
                        "Your launcher. Your instances. Authentication stays in Prism."),
                aboutCard);

        pages.put("home", homePage);
        pages.put("play", playPage);
        pages.put("modpacks", modpacksPage);
        pages.put("instances", instancesPage);
        pages.put("settings", settingsPage);
        pages.put("about", aboutPage);
        showPage(pages.containsKey(currentPage) ? currentPage : "home", false);
        updateControls();
    }

    private VBox card(String heading, javafx.scene.Node... nodes) {
        Label title = new Label(heading);
        title.getStyleClass().add("card-title");
        VBox box = new VBox(10);
        box.getChildren().add(title);
        box.getChildren().addAll(nodes);
        box.getStyleClass().add("card");
        return box;
    }

    private VBox page(String eyebrow, String title, String description, Node... content) {
        Label section = new Label(eyebrow);
        section.getStyleClass().add("eyebrow");
        Label heading = new Label(title);
        heading.getStyleClass().add("page-heading");
        heading.setWrapText(true);
        Label copy = new Label(description);
        copy.getStyleClass().add("page-copy");
        copy.setWrapText(true);
        VBox page = new VBox(16, section, heading, copy);
        page.getChildren().addAll(content);
        page.getStyleClass().add("page-container");
        return page;
    }

    private VBox metricCard(String iconText, String title, Label value, Label detail, Node action) {
        Label icon = new Label(iconText);
        icon.getStyleClass().add("metric-icon");
        Label kicker = new Label(title);
        kicker.getStyleClass().add("metric-kicker");
        HBox header = new HBox(9, icon, kicker);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(10, header, value, detail, action);
        card.getStyleClass().add("metric-card");
        card.setPrefWidth(340);
        card.setMinWidth(245);
        card.setMaxWidth(440);
        return card;
    }

    private Label metricValue() {
        Label value = new Label();
        value.getStyleClass().add("metric-value");
        value.setWrapText(true);
        return value;
    }

    private Label metricDetail() {
        Label detail = new Label();
        detail.getStyleClass().add("metric-detail");
        detail.setWrapText(true);
        return detail;
    }

    private Label hint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("hint");
        label.setWrapText(true);
        return label;
    }

    private StackPane createHeroPanel() {
        Label eyebrow = new Label("DERMEGAMECHA  /  WORLD SYSTEM");
        eyebrow.getStyleClass().add("hero-eyebrow");
        heroTitle = new Label(UiPreferences.text("BETRITT DEINE WELT", "ENTER YOUR WORLD"));
        heroTitle.getStyleClass().add("hero-title");
        heroTitle.setWrapText(true);
        heroMeta = new Label();
        heroMeta.getStyleClass().add("hero-meta");
        heroMeta.setWrapText(true);
        setupStatus = new Label();
        setupStatus.getStyleClass().add("setup-status");
        setupStatus.setWrapText(true);
        playButton = createPlayButton();
        playButtons.add(playButton);
        setupAction = smallButton(UiPreferences.text("EINRICHTUNG ÖFFNEN", "OPEN SETUP"));
        setupAction.getStyleClass().add("setup-button");
        setupAction.setOnAction(event -> showPage(setupDestination()));
        VBox actions = new VBox(11, setupStatus, new HBox(12, playButton, setupAction));
        VBox copy = new VBox(13, eyebrow, heroTitle, heroMeta, actions);
        copy.getStyleClass().add("hero-copy");
        StackPane.setAlignment(copy, Pos.CENTER_LEFT);
        StackPane.setMargin(copy, new Insets(30, 34, 30, 36));
        StackPane hero = new StackPane(new BlockWorldArtwork(), new Region(), copy);
        hero.getChildren().get(1).getStyleClass().add("hero-scrim");
        hero.getStyleClass().add("hero-panel");
        hero.setMinHeight(330);
        hero.setPrefHeight(380);
        hero.setMaxHeight(430);
        return hero;
    }

    private Button createPlayButton() {
        Button button = new Button(UiPreferences.text("▶  SPIELEN", "▶  PLAY"));
        button.getStyleClass().add("play-button");
        button.setMinHeight(56);
        button.setOnAction(event -> launch(false));
        installHoverAnimation(button);
        return button;
    }

    private Button createJoinButton() {
        Button button = smallButton(UiPreferences.text("SERVER BEITRETEN", "JOIN SERVER"));
        button.getStyleClass().add("join-button");
        button.setOnAction(event -> launch(true));
        return button;
    }

    private VBox instanceSummaryCard() {
        instanceSummary = new Label();
        instanceSummary.getStyleClass().add("launch-summary");
        instanceSummary.setWrapText(true);
        Label note = hint(UiPreferences.text(
                "Prism-Instanzen und deren tatsächliche Versionen werden nicht ausgelesen.",
                "Prism instances and their actual versions are not inspected."));
        return card(UiPreferences.text("AUSGEWÄHLTES PROFIL", "SELECTED PROFILE"), instanceSummary, note);
    }

    private VBox buildSidebar() {
        Label mark = new Label("◆");
        mark.getStyleClass().add("brand-mark");
        sidebarWordmark = new Label("DERMEGAMECHA");
        sidebarWordmark.getStyleClass().add("brand-wordmark");
        sidebarSubmark = new Label("MINECRAFT SYSTEMS");
        sidebarSubmark.getStyleClass().add("brand-submark");
        VBox brand = new VBox(4, new HBox(9, mark, sidebarWordmark), sidebarSubmark);
        brand.getStyleClass().add("brand-lockup");
        VBox nav = new VBox(7);
        nav.getStyleClass().add("sidebar-nav");
        nav.getChildren().addAll(
                navButton("home", "⌂", UiPreferences.text("Start", "Home")),
                navButton("play", "▶", UiPreferences.text("Spielen", "Play")),
                navButton("modpacks", "▧", UiPreferences.text("Modpacks", "Modpacks")),
                navButton("instances", "◈", UiPreferences.text("Instanzen", "Instances")),
                navButton("settings", "⚙", UiPreferences.text("Einstellungen", "Settings")),
                navButton("about", "ⓘ", UiPreferences.text("Über", "About")));
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label footer = new Label("BUILD  /  1.0.0");
        footer.getStyleClass().add("sidebar-footer");
        VBox result = new VBox(24, brand, nav, spacer, footer);
        result.getStyleClass().add("sidebar");
        result.setMinWidth(212);
        result.setPrefWidth(212);
        result.setMaxWidth(212);
        return result;
    }

    private Button navButton(String key, String symbol, String text) {
        Label icon = new Label(symbol);
        icon.getStyleClass().add("nav-icon");
        Label caption = new Label(text);
        caption.getStyleClass().add("nav-caption");
        navCaptions.add(caption);
        HBox content = new HBox(13, icon, caption);
        content.setAlignment(Pos.CENTER_LEFT);
        Button button = new Button();
        button.setGraphic(content);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAccessibleText(text);
        button.getStyleClass().add("nav-item");
        button.setOnAction(event -> showPage(key));
        navButtons.put(key, button);
        installHoverAnimation(button);
        return button;
    }

    private HBox buildTopbar() {
        pageTitle = new Label();
        pageTitle.getStyleClass().add("topbar-title");
        headerBuild = new Label("CLIENT  /  1.0.0");
        headerBuild.getStyleClass().add("topbar-build");
        headerPrismStatus = new Label();
        headerPrismStatus.getStyleClass().addAll("status-chip", "status-pending");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button settings = smallButton("⚙");
        settings.getStyleClass().add("icon-button");
        settings.setAccessibleText(UiPreferences.text("Einstellungen öffnen", "Open settings"));
        settings.setOnAction(event -> showPage("settings"));
        HBox result = new HBox(18, pageTitle, spacer, headerBuild, headerPrismStatus, settings);
        result.setAlignment(Pos.CENTER_LEFT);
        result.getStyleClass().add("topbar");
        return result;
    }

    private void showPage(String key) {
        showPage(key, true);
    }

    private void showPage(String key, boolean animate) {
        Node page = pages.get(key);
        if (page == null || pageHost == null) return;
        currentPage = key;
        pageTitle.setText(pageTitleFor(key));
        navButtons.forEach((pageKey, button) -> {
            button.getStyleClass().remove("active");
            if (pageKey.equals(key)) button.getStyleClass().add("active");
        });
        pageHost.getChildren().setAll(page);
        StackPane.setAlignment(page, Pos.TOP_LEFT);
        page.setOpacity(animate ? 0 : 1);
        if (animate) {
            FadeTransition transition = new FadeTransition(Duration.millis(170), page);
            transition.setFromValue(0);
            transition.setToValue(1);
            transition.play();
        }
    }

    private String pageTitleFor(String key) {
        return switch (key) {
            case "play" -> UiPreferences.text("SPIELEN", "PLAY");
            case "modpacks" -> UiPreferences.text("MODPACKS", "MODPACKS");
            case "instances" -> UiPreferences.text("INSTANZEN", "INSTANCES");
            case "settings" -> UiPreferences.text("EINSTELLUNGEN", "SETTINGS");
            case "about" -> UiPreferences.text("ÜBER", "ABOUT");
            default -> UiPreferences.text("STARTÜBERSICHT", "HOME DASHBOARD");
        };
    }

    private void installHoverAnimation(Node node) {
        node.setOnMouseEntered(event -> {
            ScaleTransition transition = new ScaleTransition(Duration.millis(120), node);
            transition.setToX(1.015);
            transition.setToY(1.015);
            transition.play();
        });
        node.setOnMouseExited(event -> {
            ScaleTransition transition = new ScaleTransition(Duration.millis(150), node);
            transition.setToX(1);
            transition.setToY(1);
            transition.play();
        });
    }

    private void updateResponsiveLayout(double width) {
        if (sidebar == null) return;
        boolean compact = width < 890;
        root.getStyleClass().remove("compact");
        if (compact) root.getStyleClass().add("compact");
        sidebar.setMinWidth(compact ? 76 : 212);
        sidebar.setPrefWidth(compact ? 76 : 212);
        sidebar.setMaxWidth(compact ? 76 : 212);
        sidebar.getStyleClass().remove("compact");
        if (compact) sidebar.getStyleClass().add("compact");
        navCaptions.forEach(caption -> {
            caption.setVisible(!compact);
            caption.setManaged(!compact);
        });
        sidebarWordmark.setVisible(!compact);
        sidebarWordmark.setManaged(!compact);
        sidebarSubmark.setVisible(!compact);
        sidebarSubmark.setManaged(!compact);
        headerBuild.setVisible(!compact);
        headerBuild.setManaged(!compact);
    }

    private Button smallButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("small-button");
        installHoverAnimation(button);
        return button;
    }

    private void rebuild() {
        buildScreen();
        stage.getScene().setRoot(root);
        applyStyle();
        updateResponsiveLayout(stage.getWidth());
        refreshPrismStatus();
    }

    private void applyStyle() {
        boolean high = UiPreferences.highContrast();
        root.setStyle("-fx-font-family: 'Segoe UI', Arial; -fx-background-color: "
                + (high ? "#000000" : "#070B14") + "; -fx-font-size: "
                + (14 * UiPreferences.textScale()) + "px;");
        String stylesheet = getClass().getResource("/styles/launcher.css").toExternalForm();
        if (!stage.getScene().getStylesheets().contains(stylesheet)) stage.getScene().getStylesheets().add(stylesheet);
        root.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("contrast"), high);
    }

    private void refreshPrismStatus() {
        Optional<Path> detected = prism.detect(UiPreferences.prismExecutable(), System.getenv());
        if (detected.isEmpty()) {
            prismDetected = false;
            prismStatusMessage = UiPreferences.text(
                    "Prism Launcher wurde nicht gefunden. Installiere Prism oder wähle prismlauncher.exe aus.",
                    "Prism Launcher was not found. Install Prism or browse to prismlauncher.exe.");
            prismStatus.setText(prismStatusMessage);
            updateControls();
            restorePack();
            return;
        }
        prismDetected = true;
        prismPathField.setText(detected.get().toString());
        prismStatusMessage = UiPreferences.text("Prism gefunden · Versionsprüfung läuft…", "Prism found · checking version…");
        prismStatus.setText(prismStatusMessage);
        updateControls();
        runBackground(UiPreferences.text("Prism-Version wird geprüft…", "Checking Prism version…"),
                progress -> prism.version(detected.get()), version -> {
                    prismPathField.setText(detected.get().toString());
                    prismStatusMessage = UiPreferences.text("Gefunden · " + version, "Detected · " + version);
                    prismStatus.setText(prismStatusMessage);
                    updateControls();
                    restorePack();
                });
    }

    private void browsePrism() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(UiPreferences.text("Prism Launcher auswählen", "Select Prism Launcher"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Prism Launcher", "prismlauncher.exe"));
        var selected = chooser.showOpenDialog(stage);
        if (selected != null) {
            prismPathField.setText(selected.getAbsolutePath());
            saveForm();
            refreshPrismStatus();
        }
    }

    private void openPrismDownload() {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new IOException("A web browser is not available.");
            }
            Desktop.getDesktop().browse(URI.create(PRISM_DOWNLOAD_PAGE));
        } catch (Exception ex) {
            showError(UiPreferences.text("Prism-Downloadseite konnte nicht geöffnet werden.", "Could not open the Prism download page."));
        }
    }

    private void openPrism() {
        final Path executable;
        try {
            executable = requirePrism();
        } catch (IOException ex) {
            showError(friendlyFailure(ex));
            return;
        }
        runBackground(UiPreferences.text("Prism wird geöffnet…", "Opening Prism…"),
                progress -> {
                    prism.open(executable);
                    return Boolean.TRUE;
                },
                started -> activity.setText(UiPreferences.text(
                        "Prism wurde gestartet. Melde dich dort an; DerMegaMecha kann den Anmeldestatus nicht bestätigen.",
                        "Prism was started. Sign in there; DerMegaMecha cannot confirm sign-in status.")));
    }

    private void chooseAndImportPack() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(UiPreferences.text("Modrinth-Modpack auswählen", "Choose Modrinth modpack"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Modrinth modpack", "*.mrpack"));
        var selected = chooser.showOpenDialog(stage);
        if (selected != null) validateAndImport(selected.toPath(), false);
    }

    private void downloadAndImportPack() {
        if (officialPackUrl == null || officialPackUrl.isBlank()) {
            showError(UiPreferences.text(
                    "Der Herausgeber hat die offizielle Modpack-Downloadadresse für diesen Build noch nicht konfiguriert. Du kannst stattdessen eine vorhandene .mrpack-Datei auswählen.",
                    "The publisher has not configured the official modpack URL for this build. You can choose an existing .mrpack file instead."));
            return;
        }
        validateAndImport(null, true);
    }

    private void validateAndImport(Path localArchive, boolean download) {
        final Path executable;
        try {
            executable = requirePrism();
        } catch (IOException ex) {
            showError(friendlyFailure(ex));
            return;
        }
        loadedPack = null;
        profileDetails.setText("");
        clearImportConfirmation();
        updateControls();
        runBackground(UiPreferences.text("Modpack wird geprüft…", "Validating modpack…"), progress -> {
            ModrinthPackService.PackInfo info;
            Path archive = localArchive;
            var listener = (com.dermegamecha.launcher.core.DownloadListener) update ->
                    progress.accept(update.phase() + " — " + update.file());
            if (download) {
                archive = packs.downloadAndVerify(URI.create(officialPackUrl), listener);
            } else {
                archive = packs.stageLocalArchive(archive);
            }
            archive = packs.sanitizeArchive(archive);
            info = packs.validateAndVerify(archive, listener);
            String archiveHash = packs.archiveSha512(archive);
            prism.importPack(executable, archive);
            return new ImportedPack(archive, info, archiveHash);
        }, imported -> {
            loadedPack = imported.info();
            UiPreferences.setPackArchivePath(imported.archive().toString());
            UiPreferences.setPackArchiveSha512(imported.archiveSha512());
            clearImportConfirmation();
            if (serverPackVersionField.getText().isBlank()) serverPackVersionField.setText(loadedPack.versionId());
            changingProfileProgrammatically = true;
            profileChoice.setValue(loadedPack.loader().equals("fabric") ? "Fabric 1.21.1" : "Forge 1.21.1");
            changingProfileProgrammatically = false;
            UiPreferences.setPrismProfile(loadedPack.loader());
            previousProfile = selectedProfileKey();
            instanceIdField.setText(UiPreferences.prismInstance(previousProfile));
            profileDetails.setText(profileRequirementText(loadedPack));
            saveForm();
            packStatus.setText(UiPreferences.text(
                    "Prüfung abgeschlossen: %s · Minecraft %s · %s %s. Prism wurde zum Import geöffnet; der Importstatus wird nicht automatisch bestätigt."
                            .formatted(loadedPack.name(), loadedPack.minecraftVersion(), loadedPack.loader(), loadedPack.loaderVersion()),
                    "Validated: %s · Minecraft %s · %s %s. Prism was opened to import it; import completion is not automatically confirmed."
                            .formatted(loadedPack.name(), loadedPack.minecraftVersion(), loadedPack.loader(), loadedPack.loaderVersion())));
            activity.setText(UiPreferences.text(
                    "Prism-Import angefordert. Schließe den Import in Prism ab und trage danach oben die Instanzordner-ID ein.",
                    "Prism import requested. Finish the import in Prism, then enter its instance folder ID above."));
            updateControls();
        });
    }

    private void restorePack() {
        String saved = UiPreferences.packArchivePath();
        if (saved.isBlank()) return;
        Path archive;
        try {
            archive = Path.of(saved);
        } catch (RuntimeException ex) {
            clearImportConfirmation();
            loadedPack = null;
            profileDetails.setText("");
            packStatus.setText(UiPreferences.text("Der gespeicherte Modpack-Pfad ist ungültig. Wähle die Datei erneut aus.",
                    "The saved modpack path is invalid. Choose the file again."));
            return;
        }
        if (!Files.isRegularFile(archive)) {
            clearImportConfirmation();
            loadedPack = null;
            profileDetails.setText("");
            packStatus.setText(UiPreferences.text("Das zuvor geprüfte Modpack wurde nicht gefunden. Wähle die Datei erneut aus.",
                    "The previously validated modpack was not found. Choose the file again."));
            return;
        }
        runBackground(UiPreferences.text("Gespeicherte Modpack-Metadaten werden erneut geprüft…",
                        "Revalidating saved modpack metadata…"),
                progress -> new RestoredPack(
                        packs.validateAndVerify(archive, update -> progress.accept(update.phase() + " — " + update.file())),
                        packs.archiveSha512(archive)),
                restored -> {
                    ModrinthPackService.PackInfo info = restored.info();
                    boolean samePack = archive.toString().equals(UiPreferences.packArchivePath())
                            && restored.archiveSha512().equalsIgnoreCase(UiPreferences.packArchiveSha512());
                    if (!samePack) {
                        clearImportConfirmation();
                        loadedPack = null;
                        profileDetails.setText("");
                        packStatus.setText(UiPreferences.text(
                                "Die Modpack-Datei hat sich seit dem Prism-Import geändert. Prüfe sie erneut und starte den Import in Prism noch einmal.",
                                "The modpack archive changed since the Prism import. Validate it again and request a new import in Prism."));
                        updateControls();
                        return;
                    }
                    loadedPack = info;
                    importConfirmed.setSelected(UiPreferences.prismImportConfirmed());
                    UiPreferences.setPrismProfile(info.loader());
                    changingProfileProgrammatically = true;
                    profileChoice.setValue(info.loader().equals("fabric") ? "Fabric 1.21.1" : "Forge 1.21.1");
                    changingProfileProgrammatically = false;
                    if (previousProfile != null && !previousProfile.equals(info.loader())) {
                        previousProfile = info.loader();
                        instanceIdField.setText(UiPreferences.prismInstance(previousProfile));
                    }
                    profileDetails.setText(profileRequirementText(info));
                    packStatus.setText(UiPreferences.text(
                            "Metadaten erneut geprüft: %s · Minecraft %s · %s %s. Ob Prism den Import abgeschlossen hat, wird nicht bestätigt."
                                    .formatted(info.name(), info.minecraftVersion(), info.loader(), info.loaderVersion()),
                            "Metadata revalidated: %s · Minecraft %s · %s %s. Prism import completion is not confirmed."
                                    .formatted(info.name(), info.minecraftVersion(), info.loader(), info.loaderVersion())));
                    if (serverPackVersionField.getText().isBlank()) serverPackVersionField.setText(info.versionId());
                    updateControls();
                });
    }

    private void launch(boolean joinServer) {
        saveForm();
        final Path executable;
        final String instanceId;
        final String address;
        try {
            executable = requirePrism();
            instanceId = PrismLauncherIntegration.validateInstanceId(instanceIdField.getText());
            address = joinServer ? com.dermegamecha.launcher.integration.prism.ServerAddressValidator.validate(serverAddressField.getText()) : "";
            if (loadedPack == null) throw new IOException("Import and validate the DerMegaMecha modpack before launching.");
            if (!selectedProfileKey().equals(loadedPack.loader())) {
                throw new IOException("The selected Prism profile does not match the verified modpack loader.");
            }
            if (!importConfirmed.isSelected()) {
                throw new IOException("Confirm that you completed the Prism import and selected the matching instance.");
            }
            if (joinServer && !serverRequirementsMatch()) {
                throw new IOException("The configured server Minecraft/modpack requirements do not match the validated modpack metadata.");
            }
        } catch (IOException ex) {
            showError(friendlyFailure(ex));
            return;
        }
        runBackground(joinServer
                        ? UiPreferences.text("Prism wird für den Serverstart geöffnet…", "Opening Prism to join the server…")
                        : UiPreferences.text("Prism wird geöffnet…", "Opening Prism…"),
                progress -> {
                    prism.launch(executable, instanceId, address);
                    return Boolean.TRUE;
                },
                started -> activity.setText(joinServer
                        ? UiPreferences.text("Prism-Start angefordert. Prism und Minecraft bestätigen den tatsächlichen Start und die Serverkompatibilität.",
                                "Launch requested. Prism and Minecraft will determine actual startup and server compatibility.")
                        : UiPreferences.text("Prism-Start angefordert. Der tatsächliche Spielstart wird nicht von DerMegaMecha bestätigt.",
                                "Launch requested. DerMegaMecha does not confirm that the game actually started.")));
    }

    private Path requirePrism() throws IOException {
        String configured = prismPathField.getText() == null ? "" : prismPathField.getText().trim();
        if (!configured.isBlank()) {
            Path path;
            try {
                path = Path.of(configured);
            } catch (RuntimeException ex) {
                throw new IOException("The Prism executable path is invalid.");
            }
            if (!Files.isRegularFile(path)) throw new IOException("Prism Launcher executable was not found at the selected path.");
            return path.toAbsolutePath().normalize();
        }
        return prism.detect("", System.getenv()).orElseThrow(() ->
                new IOException("Prism Launcher was not found. Install Prism or browse to prismlauncher.exe."));
    }

    private void saveSettings() {
        saveForm();
        activity.setText(UiPreferences.text("Einstellungen gespeichert.", "Settings saved."));
        updateControls();
        refreshPrismStatus();
    }

    private void saveForm() {
        if (prismPathField == null) return;
        UiPreferences.setPrismExecutable(prismPathField.getText());
        UiPreferences.setPrismInstance(selectedProfileKey(), instanceIdField.getText());
        UiPreferences.setPrismProfile(selectedProfileKey());
        UiPreferences.setServerName(serverNameField.getText());
        UiPreferences.setServerAddress(serverAddressField.getText());
        UiPreferences.setServerMinecraftVersion(serverMinecraftVersionField.getText());
        UiPreferences.setServerPackVersion(serverPackVersionField.getText());
        if (importConfirmed != null) UiPreferences.setPrismImportConfirmed(importConfirmed.isSelected());
    }

    private void updateControls() {
        if (profileChoice == null) return;
        prismDetected = prism.detect(prismPathField.getText(), System.getenv()).isPresent();
        boolean idValid;
        try {
            PrismLauncherIntegration.validateInstanceId(instanceIdField.getText());
            idValid = true;
        } catch (IOException ex) {
            idValid = false;
        }
        boolean profileMatches = loadedPack != null && loadedPack.loader().equals(selectedProfileKey());
        boolean addressValid = com.dermegamecha.launcher.integration.prism.ServerAddressValidator
                .isValid(serverAddressField.getText());
        boolean importIsConfirmed = importConfirmed != null && importConfirmed.isSelected();
        boolean ready = prismDetected && idValid && loadedPack != null && profileMatches && importIsConfirmed;
        playButtons.forEach(button -> button.setDisable(working || !ready));
        boolean canJoin = ready && serverRequirementsMatch() && addressValid;
        joinButtons.forEach(button -> button.setDisable(working || !canJoin));
        prismOpenButtons.forEach(button -> button.setDisable(working || !prismDetected));
        if (importConfirmed != null) importConfirmed.setDisable(working || loadedPack == null || !idValid);
        downloadPackButton.setDisable(working);
        importPackButton.setDisable(working);

        headerPrismStatus.setText(prismDetected
                ? UiPreferences.text("PRISM · GEFUNDEN", "PRISM · DETECTED")
                : UiPreferences.text("PRISM · FEHLT", "PRISM · NOT FOUND"));
        headerPrismStatus.getStyleClass().removeAll("status-ready", "status-pending");
        headerPrismStatus.getStyleClass().add(prismDetected ? "status-ready" : "status-pending");
        prismMetric.setText(prismDetected
                ? UiPreferences.text("GEFUNDEN", "DETECTED")
                : UiPreferences.text("NICHT GEFUNDEN", "NOT DETECTED"));
        prismDetail.setText(prismDetected
                ? UiPreferences.text("Anmeldung bleibt in Prism.", "Sign-in stays inside Prism.")
                : UiPreferences.text("Installiere Prism oder wähle den Programm-Pfad.", "Install Prism or select its executable."));
        prismStatus.setText(prismStatusMessage.isBlank()
                ? (prismDetected
                    ? UiPreferences.text("Prism ist verfügbar.", "Prism is available.")
                    : UiPreferences.text("Prism wurde nicht gefunden.", "Prism was not found."))
                : prismStatusMessage);

        if (loadedPack == null) {
            versionMetric.setText(UiPreferences.text("NICHT GEPRÜFT", "NOT VALIDATED"));
            versionDetail.setText(UiPreferences.text("Minecraft- und Loader-Version erscheinen nach dem Pack-Check.",
                    "Minecraft and loader versions appear after pack validation."));
            packMetric.setText(UiPreferences.text("KEIN PACK", "NO PACK"));
            packDetail.setText(UiPreferences.text("Importiere ein vertrauenswürdiges .mrpack.", "Import a trusted .mrpack."));
            heroTitle.setText(UiPreferences.text("BETRITT DEINE WELT", "ENTER YOUR WORLD"));
            heroMeta.setText(UiPreferences.text("NOCH KEIN MODPACK VERIFIZIERT", "NO MODPACK VALIDATED"));
        } else {
            String loader = loadedPack.loader().equals("fabric") ? "FABRIC" : "FORGE";
            versionMetric.setText("MINECRAFT " + loadedPack.minecraftVersion());
            versionDetail.setText(loader + " " + loadedPack.loaderVersion());
            packMetric.setText(loadedPack.name());
            packDetail.setText(UiPreferences.text("PACK-VERSION · ", "PACK VERSION · ") + loadedPack.versionId());
            String name = loadedPack.name();
            heroTitle.setText(name.length() > 34 ? name.substring(0, 31) + "…" : name);
            heroMeta.setText("MINECRAFT " + loadedPack.minecraftVersion() + "  /  " + loader
                    + " " + loadedPack.loaderVersion() + "  /  "
                    + (idValid ? "INSTANCE " + instanceIdField.getText() : "INSTANCE NOT SELECTED"));
        }

        String serverName = serverNameField.getText().isBlank()
                ? UiPreferences.text("Kein Servername", "No server name")
                : serverNameField.getText().trim();
        serverMetric.setText(serverName);
        serverDetail.setText(serverAddressField.getText().isBlank()
                ? UiPreferences.text("Keine Adresse · Status nicht geprüft", "No address · status not checked")
                : serverAddressField.getText().trim() + "  ·  "
                    + UiPreferences.text("Status nicht geprüft", "Status not checked"));
        String profile = selectedProfileKey().equals("fabric") ? "Fabric" : "Forge";
        String instance = idValid ? instanceIdField.getText() : UiPreferences.text("nicht gewählt", "not selected");
        String requiredVersion = loadedPack == null ? "" : " · Minecraft " + loadedPack.minecraftVersion()
                + " · " + profile + " " + loadedPack.loaderVersion();
        instanceSummary.setText(profile + "  /  " + instance + requiredVersion);
        launchSummary.setText(ready
                ? UiPreferences.text("INSTANZ BESTÄTIGT · ", "INSTANCE CONFIRMED · ") + instance
                    + UiPreferences.text(". Prism bleibt für die Anmeldung zuständig.", ". Prism handles authentication.")
                : UiPreferences.text("EINRICHTUNG ERFORDERLICH · ", "SETUP REQUIRED · ")
                    + setupReason(prismDetected, loadedPack, idValid, profileMatches, importIsConfirmed));

        if (!ready) {
            setupStatus.setText(setupReason(prismDetected, loadedPack, idValid, profileMatches, importIsConfirmed));
            setupAction.setText(setupActionLabel(prismDetected, loadedPack));
            setupAction.setVisible(true);
            setupAction.setManaged(true);
        } else {
            setupStatus.setText(UiPreferences.text(
                    "SYSTEM BEREIT · Prism übernimmt die Anmeldung. Der tatsächliche Spielstart wird nicht vorweggenommen.",
                    "SYSTEM READY · Prism handles sign-in. Actual game startup is not assumed."));
            setupAction.setVisible(false);
            setupAction.setManaged(false);
        }

        if (loadedPack == null) {
            compatibilityStatus.setText(UiPreferences.text(
                    "Prüfe zuerst ein Modpack, um dessen Minecraft- und Loader-Version mit den Serverangaben abzugleichen.",
                    "Validate a modpack first to compare its Minecraft and loader metadata with the configured server requirements."));
        } else if (!profileMatches) {
            compatibilityStatus.setText(UiPreferences.text(
                    "Der ausgewählte Profiltyp passt nicht zum Loader des geprüften Modpacks.",
                    "The selected profile type does not match the verified modpack loader."));
        } else if (!addressValid) {
            compatibilityStatus.setText(UiPreferences.text(
                    "Gib eine gültige Serveradresse ein. Der Launcher prüft nicht, ob der Server online ist.",
                    "Enter a valid server address. The launcher does not check whether the server is online."));
        } else if (!serverRequirementsMatch()) {
            compatibilityStatus.setText(UiPreferences.text(
                    "Die eingetragenen Serveranforderungen stimmen nicht mit den Modpack-Metadaten überein.",
                    "The configured server requirements do not match the modpack metadata."));
        } else {
            compatibilityStatus.setText(UiPreferences.text(
                    "Serverangaben stimmen mit den geprüften Pack-Metadaten überein. Die Prism-Instanz selbst wird nicht ausgelesen; dies ist keine Online- oder Kompatibilitätsbestätigung.",
                    "Configured server requirements match the validated pack metadata. The Prism instance is not inspected; this is not an online or compatibility confirmation."));
        }
    }

    private String setupDestination() {
        if (!prismDetected) return "settings";
        if (loadedPack == null) return "modpacks";
        try {
            PrismLauncherIntegration.validateInstanceId(instanceIdField.getText());
        } catch (IOException ex) {
            return "instances";
        }
        if (!selectedProfileKey().equals(loadedPack.loader()) || !importConfirmed.isSelected()) return "instances";
        return "play";
    }

    private String setupReason(boolean prismFound, ModrinthPackService.PackInfo pack, boolean idValid,
                               boolean profileMatches, boolean confirmed) {
        if (!prismFound) return UiPreferences.text(
                "Prism Launcher fehlt. Installiere Prism oder wähle seine Anwendung in den Einstellungen.",
                "Prism Launcher is missing. Install it or select its executable in Settings.");
        if (pack == null) return UiPreferences.text(
                "Kein Modpack geprüft. Importiere zuerst eine vertrauenswürdige .mrpack-Datei.",
                "No modpack is validated. Import a trusted .mrpack first.");
        if (!idValid) return UiPreferences.text(
                "Die Prism-Instanz-ID fehlt. Wähle die importierte Instanz in Prism und trage ihren Ordnernamen ein.",
                "The Prism instance ID is missing. Select the imported instance in Prism and enter its folder name.");
        if (!profileMatches) return UiPreferences.text(
                "Das gewählte Profil passt nicht zum Loader des Modpacks.",
                "The selected profile does not match the modpack loader.");
        if (!confirmed) return UiPreferences.text(
                "Bestätige, dass Import und Versionsprüfung in Prism abgeschlossen sind.",
                "Confirm that the Prism import and version check are complete.");
        return UiPreferences.text("Die Einrichtung ist vollständig.", "Setup is complete.");
    }

    private String setupActionLabel(boolean prismFound, ModrinthPackService.PackInfo pack) {
        if (!prismFound) return UiPreferences.text("PRISM EINRICHTEN", "SET UP PRISM");
        if (pack == null) return UiPreferences.text("MODPACK IMPORTIEREN", "IMPORT MODPACK");
        return UiPreferences.text("INSTANZ PRÜFEN", "CHECK INSTANCE");
    }

    private boolean serverRequirementsMatch() {
        return ModrinthPackService.matchesServerRequirements(
                loadedPack, serverMinecraftVersionField.getText(), serverPackVersionField.getText())
                && serverAddressField.getText() != null
                && !serverAddressField.getText().isBlank();
    }

    private String profileRequirementText(ModrinthPackService.PackInfo pack) {
        String loaderLabel = pack.loader().equals("forge") ? "Forge" : "Fabric Loader";
        return UiPreferences.text(
                "Geprüftes Pack benötigt Minecraft %s + %s %s. Prüfe dieselben Versionen in Prism; der angegebene Instanzname allein bestätigt keine Kompatibilität."
                        .formatted(pack.minecraftVersion(), loaderLabel, pack.loaderVersion()),
                "Validated pack requires Minecraft %s + %s %s. Verify those versions in Prism; the instance name alone does not confirm compatibility."
                        .formatted(pack.minecraftVersion(), loaderLabel, pack.loaderVersion()));
    }

    private String selectedProfileKey() {
        return profileChoice != null && profileChoice.getValue() != null
                && profileChoice.getValue().toLowerCase().startsWith("fabric") ? "fabric" : "forge";
    }

    private <T> void runBackground(String message, Work<T> operation, Consumer<T> success) {
        working = true;
        activity.setText(message);
        spinner.setVisible(true);
        spinner.setManaged(true);
        updateControls();
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception {
                return operation.run(this::updateMessage);
            }
        };
        task.messageProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) activity.setText(newValue);
        });
        task.setOnSucceeded(event -> {
            working = false;
            spinner.setVisible(false);
            spinner.setManaged(false);
            success.accept(task.getValue());
            updateControls();
        });
        task.setOnFailed(event -> {
            working = false;
            spinner.setVisible(false);
            spinner.setManaged(false);
            String operationDescription = message.toLowerCase(java.util.Locale.ROOT);
            if (operationDescription.contains("prism-version") || operationDescription.contains("prism version")) {
                prismStatusMessage = UiPreferences.text(
                        "Prism wurde gefunden, aber die Versionsprüfung ist fehlgeschlagen.",
                        "Prism was detected, but its version check failed.");
                prismStatus.setText(prismStatusMessage);
            }
            updateControls();
            Throwable error = task.getException();
            showError(friendlyFailure(error));
        });
        Thread thread = new Thread(task, "dermegamecha-prism-work");
        thread.setDaemon(true);
        thread.start();
    }

    private void openFolder(Path path) {
        try {
            Files.createDirectories(path);
            if (!Desktop.isDesktopSupported()) throw new IOException("Desktop file browsing is not available.");
            Desktop.getDesktop().open(path.toFile());
        } catch (Exception ex) {
            showError(UiPreferences.text("Der Ordner konnte nicht geöffnet werden.", "Could not open the folder."));
        }
    }

    private void showHelp() {
        showInfo(UiPreferences.text("Öffne Prism Launcher und melde dich dort mit deinem Microsoft-Konto an. DerMegaMecha liest keine Kontodaten und kann deinen Anmeldestatus nicht bestätigen. Modpack-Importe müssen in Prism abgeschlossen und eine Instanzordner-ID muss anschließend ausgewählt werden.\n\nDatenordner: %s\nProtokolle: %s"
                .formatted(paths.root, paths.logs),
                "Open Prism Launcher and sign in to your Microsoft account there. DerMegaMecha does not read account data or confirm your sign-in status. Finish modpack imports in Prism, then select the instance folder ID.\n\nData folder: %s\nLogs: %s"
                        .formatted(paths.root, paths.logs)));
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(UiPreferences.text("Der Vorgang ist fehlgeschlagen", "Operation failed"));
        alert.setHeaderText(UiPreferences.text("Bitte prüfe deine Auswahl", "Check your selection"));
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(UiPreferences.text("Hilfe", "Help"));
        alert.setHeaderText(UiPreferences.text("DerMegaMecha Launcher", "DerMegaMecha Launcher"));
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FunctionalInterface private interface Work<T> {
        T run(Consumer<String> progress) throws Exception;
    }

    private void clearImportConfirmation() {
        if (importConfirmed != null) importConfirmed.setSelected(false);
        UiPreferences.setPrismImportConfirmed(false);
    }

    private record ImportedPack(Path archive, ModrinthPackService.PackInfo info, String archiveSha512) {}
    private record RestoredPack(ModrinthPackService.PackInfo info, String archiveSha512) {}

    @Override public void stop() {
        saveForm();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
