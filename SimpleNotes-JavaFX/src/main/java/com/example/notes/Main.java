package com.example.notes;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main extends Application {

  private final Database db = new Database(AppPaths.databasePath());
  private final NoteRepository repo = new NoteRepository(db);

  private final ObservableList<NoteSummary> allNotes = FXCollections.observableArrayList();
  private final FilteredList<NoteSummary> filteredNotes = new FilteredList<>(allNotes);

  private final ListView<NoteSummary> noteList = new ListView<>(filteredNotes);
  private final TextField titleField = new TextField();
  private final TextArea contentArea = new TextArea();
  private final TextField searchField = new TextField();

  private final Label statusLabel = new Label("Ready");
  private final Label statsLabel = new Label();

  private Stage stage;
  private Note currentNote = null;
  private boolean dirty = false;
  private boolean loading = false;
  private java.util.Timer autoSaveTimer;
  private boolean isDarkMode = true;

  @Override
  public void start(Stage primaryStage) {
    this.stage = primaryStage;
    stage.setTitle("myNote");
    stage.setMinWidth(800);   // Normal Notepad size
    stage.setMinHeight(600);
    stage.setWidth(900);      // Initial size
    stage.setHeight(650);

    try {
      db.init();
    } catch (SQLException e) {
      showError("Database Error", e.getMessage());
    }

    setupUI();
    refreshNotes();

    if (!allNotes.isEmpty()) {
      noteList.getSelectionModel().select(0);
      openSelectedNote();
    } else {
      newNote();
    }

    startAutoSave();
    applyTheme();
    stage.show();
  }

  private void setupUI() {
    BorderPane root = new BorderPane();
    root.setTop(createMenuBar());
    root.setLeft(createLeftPanel());
    root.setCenter(createEditorPanel());
    root.setBottom(createStatusBar());

    Scene scene = new Scene(root);
    stage.setScene(scene);

    // Keyboard Shortcuts
    scene.setOnKeyPressed(e -> {
      if (e.isControlDown()) {
        if (e.getCode() == KeyCode.N) newNote();
        if (e.getCode() == KeyCode.S) saveNote(true);
        if (e.getCode() == KeyCode.F) searchField.requestFocus();
        if (e.getCode() == KeyCode.I) importNote();
        if (e.getCode() == KeyCode.E) exportCurrentNote();
      }
    });

    stage.setOnCloseRequest(e -> {
      if (!confirmLeaveIfDirty()) e.consume();
    });
  }

  private MenuBar createMenuBar() {
    MenuBar bar = new MenuBar();
    Menu file = new Menu("File");
    file.getItems().addAll(
            createMenuItem("New Note", this::newNote),
            createMenuItem("Save", () -> saveNote(true)),
            new SeparatorMenuItem(),
            createMenuItem("Import from TXT", this::importNote),
            createMenuItem("Export as TXT", this::exportCurrentNote),
            new SeparatorMenuItem(),
            createMenuItem("Delete", this::deleteNote),
            createMenuItem("Exit", () -> { if (confirmLeaveIfDirty()) Platform.exit(); })
    );

    Menu view = new Menu("View");                    // ← Added back
    view.getItems().addAll(
            createMenuItem("Toggle Dark/Light Theme", this::toggleTheme)   // ← Added back
    );

    bar.getMenus().addAll(file, view);
    return bar;
  }

  private MenuItem createMenuItem(String text, Runnable action) {
    MenuItem item = new MenuItem(text);
    item.setOnAction(e -> action.run());
    return item;
  }

  private VBox createLeftPanel() {
    VBox vbox = new VBox(12);
    vbox.setPadding(new Insets(15));
    vbox.setPrefWidth(360);
    vbox.setStyle("-fx-background-color: #2a2a2a;");

    searchField.setPromptText("🔍 Search notes...");
    searchField.setStyle("-fx-background-radius: 15;");

    HBox top = new HBox(10);
    top.setAlignment(Pos.CENTER_LEFT);
    top.getChildren().addAll(new Label("📚 My Notes"), searchField);

    noteList.setCellFactory(lv -> new ListCell<>() {
      @Override
      protected void updateItem(NoteSummary note, boolean empty) {
        super.updateItem(note, empty);
        if (empty || note == null) {
          setText(null);
        } else {
          setText(" " + note.title() + "\n   " + formatDate(note.updatedAt()));
        }
      }
    });

    noteList.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
      if (loading || newVal == null) return;
      if (currentNote != null && currentNote.id() != null && currentNote.id().equals(newVal.id())) return;

      if (!confirmLeaveIfDirty()) {
        loading = true;
        Platform.runLater(() -> noteList.getSelectionModel().select(old));
        loading = false;
        return;
      }
      openSelectedNote();
    });

    Button newBtn = new Button("➕ New Note");
    newBtn.setMaxWidth(Double.MAX_VALUE);
    newBtn.setOnAction(e -> newNote());

    vbox.getChildren().addAll(top, noteList, newBtn);
    VBox.setVgrow(noteList, Priority.ALWAYS);
    return vbox;
  }

  private VBox createEditorPanel() {
    VBox vbox = new VBox(12);
    vbox.setPadding(new Insets(20));

    HBox titleBar = new HBox(10);
    titleField.setPromptText("Note Title");
    titleField.setStyle("-fx-font-size: 16px; -fx-padding: 8;");
    titleBar.getChildren().addAll(new Label("Title:"), titleField);

    contentArea.setWrapText(true);
    contentArea.setFont(javafx.scene.text.Font.font("Consolas", 16));
    contentArea.setStyle("-fx-padding: 15;");

    contentArea.textProperty().addListener((obs, old, newText) -> { markDirty(); updateStats(); });
    titleField.textProperty().addListener((obs, old, newText) -> markDirty());

    vbox.getChildren().addAll(titleBar, contentArea);
    VBox.setVgrow(contentArea, Priority.ALWAYS);
    return vbox;
  }

  private HBox createStatusBar() {
    HBox bar = new HBox(20);
    bar.setPadding(new Insets(12, 20, 12, 20));
    bar.setAlignment(Pos.CENTER_LEFT);
    bar.getChildren().addAll(statusLabel, statsLabel);
    return bar;
  }

  private void toggleTheme() {
    isDarkMode = !isDarkMode;
    applyTheme();
  }

  private void applyTheme() {
    String rootStyle = isDarkMode ?
            "-fx-base: #1e1e1e; -fx-accent: #00d4ff; -fx-text-fill: white;" :
            "-fx-base: #f0f2f5; -fx-accent: #0066cc;";

    stage.getScene().getRoot().setStyle(rootStyle);
  }

  private void importNote() {
    FileChooser fc = new FileChooser();
    fc.setTitle("Import Text File");
    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
    File file = fc.showOpenDialog(stage);

    if (file != null) {
      try {
        String content = Files.readString(file.toPath());
        String fileName = file.getName().replace(".txt", "");

        newNote();
        titleField.setText(fileName);
        contentArea.setText(content);
        markDirty();
        statusLabel.setText("Imported: " + fileName);
      } catch (IOException e) {
        showError("Import Failed", e.getMessage());
      }
    }
  }

  private void exportCurrentNote() {
    if (currentNote == null) return;

    FileChooser fc = new FileChooser();
    fc.setTitle("Export Note");
    fc.setInitialFileName(titleField.getText().replaceAll("[\\\\/:*?\"<>|]", "_") + ".txt");
    File file = fc.showSaveDialog(stage);

    if (file != null) {
      try (PrintWriter pw = new PrintWriter(file)) {
        pw.println("Title: " + titleField.getText());
        pw.println("Date: " + formatDate(System.currentTimeMillis()));
        pw.println("=".repeat(60));
        pw.println(contentArea.getText());
        statusLabel.setText("✅ Exported successfully");
      } catch (IOException e) {
        showError("Export Failed", e.getMessage());
      }
    }
  }

  // ==================== Core Logic ====================
  private void filterNotes(String query) {
    if (query == null || query.trim().isEmpty()) {
      filteredNotes.setPredicate(null);
    } else {
      String q = query.toLowerCase();
      filteredNotes.setPredicate(note -> note.title().toLowerCase().contains(q));
    }
  }

  private void refreshNotes() {
    try {
      allNotes.setAll(repo.listNotes());
    } catch (SQLException e) {
      showError("Load Error", e.getMessage());
    }
  }

  private void openSelectedNote() {
    NoteSummary selected = noteList.getSelectionModel().getSelectedItem();
    if (selected == null) return;
    try {
      repo.getNote(selected.id()).ifPresent(this::setCurrentNote);
    } catch (SQLException e) {
      showError("Open Error", e.getMessage());
    }
  }

  private void setCurrentNote(Note note) {
    loading = true;
    try {
      currentNote = note;
      titleField.setText(note.title());
      contentArea.setText(note.content());
      dirty = false;
      updateStatus();
      updateStats();
    } finally {
      loading = false;
    }
  }

  private void newNote() {
    if (!confirmLeaveIfDirty()) return;
    setCurrentNote(new Note(null, "Untitled", "", System.currentTimeMillis(), System.currentTimeMillis()));
    noteList.getSelectionModel().clearSelection();
  }

  private void saveNote(boolean showMessage) {
    if (currentNote == null) return;

    String title = titleField.getText().trim();
    if (title.isEmpty()) title = "Untitled";

    try {
      Note saved = repo.upsert(new Note(
              currentNote.id(), title, contentArea.getText(),
              currentNote.createdAt(), System.currentTimeMillis()
      ));

      setCurrentNote(saved);
      refreshNotes();

      if (showMessage) statusLabel.setText("✅ Saved");
    } catch (SQLException e) {
      showError("Save Failed", e.getMessage());
    }
  }

  private void deleteNote() {
    if (currentNote == null || currentNote.id() == null) {
      newNote();
      return;
    }

    Alert alert = new Alert(AlertType.CONFIRMATION, "Delete this note?");
    alert.setTitle("Confirm Delete");
    if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
      try {
        repo.delete(currentNote.id());
        currentNote = null;
        refreshNotes();
        if (!allNotes.isEmpty()) {
          noteList.getSelectionModel().select(0);
          openSelectedNote();
        } else newNote();
      } catch (SQLException e) {
        showError("Delete Failed", e.getMessage());
      }
    }
  }

  private boolean confirmLeaveIfDirty() {
    if (!dirty) return true;

    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setTitle("Unsaved Changes");
    alert.setHeaderText("You have unsaved changes");
    alert.setContentText("Save before continuing?");

    ButtonType save = new ButtonType("Save");
    ButtonType discard = new ButtonType("Don't Save");
    ButtonType cancel = ButtonType.CANCEL;

    Optional<ButtonType> result = alert.showAndWait();
    if (result.isEmpty() || result.get() == cancel) return false;
    if (result.get() == save) {
      saveNote(false);
      return !dirty;
    }
    return true;
  }

  private void markDirty() {
    if (loading) return;
    dirty = true;
    updateStatus();
  }

  private void updateStatus() {
    statusLabel.setText(dirty ? "● Unsaved" : "✓ Saved");
    stage.setTitle((dirty ? "● " : "") +
            (currentNote != null ? currentNote.title() : "Untitled") + " - myNote");
  }

  private void updateStats() {
    String text = contentArea.getText();
    int chars = text.length();
    int words = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
    int readTime = Math.max(1, words / 200);
    statsLabel.setText(chars + " chars | " + words + " words | ~" + readTime + " min read");
  }

  private void startAutoSave() {
    autoSaveTimer = new java.util.Timer(true);
    autoSaveTimer.scheduleAtFixedRate(new java.util.TimerTask() {
      @Override
      public void run() {
        if (dirty && currentNote != null) {
          Platform.runLater(() -> saveNote(false));
        }
      }
    }, 3000, 3000);
  }

  private String formatDate(long timestamp) {
    LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    return date.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
  }

  private void showError(String header, String content) {
    Alert alert = new Alert(AlertType.ERROR);
    alert.setTitle("Error");
    alert.setHeaderText(header);
    alert.setContentText(content);
    alert.showAndWait();
  }

  public static void main(String[] args) {
    launch(args);
  }

  // ====================== DATABASE CLASSES ======================
  private static final class AppPaths {
    static Path databasePath() {
      String appData = System.getenv("APPDATA");
      Path home = (appData != null && !appData.isBlank())
              ? Paths.get(appData, "myNote")
              : Paths.get(System.getProperty("user.home"), ".mynote");
      return home.resolve("notes.db");
    }
  }

  private static final class Database {
    private final String jdbcUrl;

    Database(Path dbPath) {
      ensureParentDir(dbPath);
      this.jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
    }

    Connection openConnection() throws SQLException {
      return DriverManager.getConnection(jdbcUrl);
    }

    void init() throws SQLException {
      try (Connection c = openConnection(); Statement s = c.createStatement()) {
        s.executeUpdate(
                "CREATE TABLE IF NOT EXISTS notes (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "title TEXT NOT NULL," +
                        "content TEXT NOT NULL," +
                        "created_at INTEGER NOT NULL," +
                        "updated_at INTEGER NOT NULL" +
                        ")"
        );
        s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_notes_updated ON notes(updated_at DESC)");
      }
    }

    private static void ensureParentDir(Path filePath) {
      Path parent = filePath.toAbsolutePath().getParent();
      if (parent != null) {
        try {
          Files.createDirectories(parent);
        } catch (java.io.IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    }
  }

  private static final class Note {
    private final Long id;
    private final String title;
    private final String content;
    private final long createdAt;
    private final long updatedAt;

    Note(Long id, String title, String content, long createdAt, long updatedAt) {
      this.id = id; this.title = title; this.content = content;
      this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    Long id() { return id; }
    String title() { return title; }
    String content() { return content; }
    long createdAt() { return createdAt; }
    long updatedAt() { return updatedAt; }
  }

  private static final class NoteSummary {
    private final Long id;
    private final String title;
    private final long updatedAt;

    NoteSummary(Long id, String title, long updatedAt) {
      this.id = id;
      this.title = title;
      this.updatedAt = updatedAt;
    }

    Long id() { return id; }
    String title() { return title != null ? title : "Untitled"; }
    long updatedAt() { return updatedAt; }
  }

  private static final class NoteRepository {
    private final Database db;
    NoteRepository(Database db) { this.db = db; }

    List<NoteSummary> listNotes() throws SQLException {
      String sql = "SELECT id, title, updated_at FROM notes ORDER BY updated_at DESC";
      List<NoteSummary> list = new ArrayList<>();
      try (Connection c = db.openConnection();
           PreparedStatement ps = c.prepareStatement(sql);
           ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          list.add(new NoteSummary(rs.getLong("id"), rs.getString("title"), rs.getLong("updated_at")));
        }
      }
      return list;
    }

    Optional<Note> getNote(Long id) throws SQLException {
      if (id == null) return Optional.empty();
      String sql = "SELECT * FROM notes WHERE id = ?";
      try (Connection c = db.openConnection();
           PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setLong(1, id);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            return Optional.of(new Note(
                    rs.getLong("id"), rs.getString("title"), rs.getString("content"),
                    rs.getLong("created_at"), rs.getLong("updated_at")
            ));
          }
        }
      }
      return Optional.empty();
    }

    Note upsert(Note note) throws SQLException {
      if (note.id() == null) return insert(note.title(), note.content());
      update(note.id(), note.title(), note.content());
      return getNote(note.id()).orElseThrow();
    }

    private Note insert(String title, String content) throws SQLException {
      long now = System.currentTimeMillis();
      String sql = "INSERT INTO notes(title, content, created_at, updated_at) VALUES(?,?,?,?)";
      try (Connection c = db.openConnection();
           PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        ps.setString(1, title);
        ps.setString(2, content);
        ps.setLong(3, now);
        ps.setLong(4, now);
        ps.executeUpdate();
        try (ResultSet keys = ps.getGeneratedKeys()) {
          keys.next();
          return new Note(keys.getLong(1), title, content, now, now);
        }
      }
    }

    private void update(Long id, String title, String content) throws SQLException {
      long now = System.currentTimeMillis();
      String sql = "UPDATE notes SET title=?, content=?, updated_at=? WHERE id=?";
      try (Connection c = db.openConnection();
           PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setString(1, title);
        ps.setString(2, content);
        ps.setLong(3, now);
        ps.setLong(4, id);
        ps.executeUpdate();
      }
    }

    void delete(Long id) throws SQLException {
      if (id == null) return;
      try (Connection c = db.openConnection();
           PreparedStatement ps = c.prepareStatement("DELETE FROM notes WHERE id = ?")) {
        ps.setLong(1, id);
        ps.executeUpdate();
      }
    }
  }
}