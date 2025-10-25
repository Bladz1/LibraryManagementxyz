package ui.fx;

import dao.BookDAO;
import dao.BorrowRecordDAO;
import dao.ReaderDAO;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TableCell;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.BorrowRecord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javafx.scene.text.TextAlignment;

public class LibraryApp extends Application {

    private final BookDAO bookDAO = new BookDAO();
    private final ReaderDAO readerDAO = new ReaderDAO();
    private final BorrowRecordDAO recordDAO = new BorrowRecordDAO();

    private final Map<String, ToggleButton> navButtons = new HashMap<>();
    private final Map<String, Node> pages = new HashMap<>();

    private StackPane content;

    private final Label booksMetric = new Label("--");
    private final Label readersMetric = new Label("--");
    private final Label borrowingMetric = new Label("--");
    private final ListView<String> topBooksList = new ListView<>();
    private final ListView<String> topReadersList = new ListView<>();
    private final ListView<String> overdueList = new ListView<>();
    private final TableView<BorrowRecord> recentTable = new TableView<>();

    private BooksPage booksPage;
    private ReadersPage readersPage;
    private BorrowPage borrowPage;
    private ReportsPage reportsPage;
    private Node dashboardView;

    private final Map<Integer, String> readerCache = new HashMap<>();
    private final Map<Integer, String> bookCache = new HashMap<>();

    public static void launchApp(String... args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app");
        root.setLeft(buildSidebar());

        content = new StackPane();
        content.getStyleClass().add("content-container");
        root.setCenter(content);

        buildPages();

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(Objects.requireNonNull(
                LibraryApp.class.getResource("library-theme.css"))
                .toExternalForm());

        stage.setTitle("📚 Hệ thống quản lý thư viện");
        stage.setScene(scene);
        stage.show();

        Platform.runLater(() -> showPage("dashboard"));
    }

    private VBox buildSidebar() {
        VBox side = new VBox(16);
        side.getStyleClass().add("sidebar");
        side.setPadding(new Insets(24));
        side.setPrefWidth(240);

        Label brand = new Label("📚 Library");
        brand.getStyleClass().add("brand");

        ToggleGroup group = new ToggleGroup();
        side.getChildren().add(brand);
        side.getChildren().add(createNavButton("Trang chủ", "dashboard", group));
        side.getChildren().add(createNavButton("Quản lý sách", "books", group));
        side.getChildren().add(createNavButton("Độc giả", "readers", group));
        side.getChildren().add(createNavButton("Mượn/Trả", "borrow", group));
        side.getChildren().add(createNavButton("Báo cáo", "reports", group));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        side.getChildren().add(spacer);

        Label footer = new Label("Made with JavaFX ✨");
        footer.getStyleClass().add("muted-label");
        side.getChildren().add(footer);
        return side;
    }

    private ToggleButton createNavButton(String text, String key, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("nav-button");
        button.setToggleGroup(group);
        button.setUserData(key);
        button.setOnAction(e -> showPage(key));
        navButtons.put(key, button);
        return button;
    }

    private void buildPages() {
        reportsPage = new ReportsPage(recordDAO, bookDAO, readerDAO);
        borrowPage = new BorrowPage(recordDAO, bookDAO, readerDAO, () -> {
            clearCaches();
            reportsPage.clearCaches();
            refreshDashboard();
            reportsPage.refresh();
        });
        booksPage = new BooksPage(bookDAO, () -> {
            clearCaches();
            if (borrowPage != null) {
                borrowPage.clearCaches();
            }
            if (reportsPage != null) {
                reportsPage.clearCaches();
            }
            refreshDashboard();
        });
        readersPage = new ReadersPage(readerDAO, () -> {
            clearCaches();
            if (borrowPage != null) {
                borrowPage.clearCaches();
            }
            if (reportsPage != null) {
                reportsPage.clearCaches();
            }
            refreshDashboard();
        });

        dashboardView = buildDashboard();

        pages.put("dashboard", dashboardView);
        pages.put("books", booksPage);
        pages.put("readers", readersPage);
        pages.put("borrow", borrowPage);
        pages.put("reports", reportsPage);
    }

    private Node buildDashboard() {
        VBox container = new VBox(24);
        container.getStyleClass().add("page");
        container.setPadding(new Insets(24));

        Label title = new Label("Xin chào 👋");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Bảng điều khiển tổng quan về thư viện của bạn.");
        subtitle.getStyleClass().add("page-subtitle");

        FlowPane metrics = new FlowPane();
        metrics.setHgap(16);
        metrics.setVgap(16);

        metrics.getChildren().add(metricCard("Tổng số sách", booksMetric, "accent-primary"));
        metrics.getChildren().add(metricCard("Độc giả", readersMetric, "accent-success"));
        metrics.getChildren().add(metricCard("Đang mượn", borrowingMetric, "accent-info"));

        configureStatList(topBooksList);
        configureStatList(topReadersList);
        configureStatList(overdueList);

        HBox lists = new HBox(16,
                statCard("Top sách được mượn", topBooksList),
                statCard("Độc giả tích cực", topReadersList),
                statCard("Đang quá hạn", overdueList));
        lists.setPrefHeight(240);

        configureRecentTable();
        VBox recentCard = new VBox(12, new Label("Nhật ký mượn gần đây"), recentTable);
        recentCard.getStyleClass().add("card");
        VBox.setVgrow(recentTable, Priority.ALWAYS);

        container.getChildren().addAll(title, subtitle, metrics, lists, recentCard);

        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-page");
        return scroll;
    }

    private void configureRecentTable() {
        recentTable.getStyleClass().add("elevated-card");
        recentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        recentTable.setPlaceholder(new Label("Không có bản ghi gần đây."));

        TableColumn<BorrowRecord, String> readerCol = new TableColumn<>("Độc giả");
        readerCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(readerName(cell.getValue().getReaderID())));

        TableColumn<BorrowRecord, String> bookCol = new TableColumn<>("Sách");
        bookCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(bookTitle(cell.getValue().getBookID())));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        TableColumn<BorrowRecord, String> borrowCol = new TableColumn<>("Ngày mượn");
        borrowCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatDate(cell.getValue().getBorrowDate(), formatter)));

        TableColumn<BorrowRecord, String> statusCol = new TableColumn<>("Trạng thái");
        statusCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(resolveStatus(cell.getValue())));

        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label chip = new Label(item);
                    chip.getStyleClass().add("status-chip");
                    switch (item) {
                        case "Đã trả" -> chip.getStyleClass().add("chip-success");
                        case "Quá hạn" -> chip.getStyleClass().add("chip-danger");
                        default -> chip.getStyleClass().add("chip-info");
                    }
                    setGraphic(chip);
                    setText(null);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        recentTable.getColumns().addAll(readerCol, bookCol, borrowCol, statusCol);
    }

    private void configureStatList(ListView<String> listView) {
        listView.getStyleClass().add("stat-list");
        listView.setFocusTraversable(false);
        listView.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    setTextAlignment(TextAlignment.CENTER);
                    setWrapText(true);
                }
            }
        });
    }

    private VBox metricCard(String title, Label valueLabel, String accentClass) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("metric-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        valueLabel.getStyleClass().add("metric-value");
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        valueLabel.setAlignment(Pos.CENTER);
        valueLabel.setTextAlignment(TextAlignment.CENTER);
        VBox card = new VBox(8, titleLabel, valueLabel);
        card.getStyleClass().addAll("card", "metric-card", accentClass);
        card.setAlignment(Pos.CENTER);
        return card;
    }

    private VBox statCard(String title, Control content) {
        Label heading = new Label(title);
        heading.getStyleClass().add("card-heading");
        heading.setMaxWidth(Double.MAX_VALUE);
        heading.setAlignment(Pos.CENTER);
        heading.setTextAlignment(TextAlignment.CENTER);
        VBox card = new VBox(12, heading, content);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(content, Priority.ALWAYS);
        return card;
    }

    private String formatDate(LocalDate date, DateTimeFormatter formatter) {
        if (date == null) {
            return "-";
        }
        return formatter.format(date);
    }

    private String resolveStatus(BorrowRecord record) {
        if (record.getReturnDate() != null) {
            return "Đã trả";
        }
        LocalDate due = record.getDueDate();
        if (due != null && due.isBefore(LocalDate.now())) {
            return "Quá hạn";
        }
        return "Đang mượn";
    }

    private String readerName(int readerId) {
        return readerCache.computeIfAbsent(readerId, id -> {
            try {
                return Optional.ofNullable(readerDAO.getReaderNameById(id)).orElse("#" + id);
            } catch (Exception ex) {
                return "#" + id;
            }
        });
    }

    private String bookTitle(int bookId) {
        return bookCache.computeIfAbsent(bookId, id -> {
            try {
                return Optional.ofNullable(bookDAO.getBookTitleById(id)).orElse("#" + id);
            } catch (Exception ex) {
                return "#" + id;
            }
        });
    }

    private void clearCaches() {
        readerCache.clear();
        bookCache.clear();
    }

    private void showPage(String key) {
        Node page = pages.get(key);
        if (page == null) {
            return;
        }
        content.getChildren().setAll(page);
        navButtons.values().forEach(btn -> btn.setSelected(Objects.equals(btn.getUserData(), key)));

        switch (key) {
            case "dashboard" -> refreshDashboard();
            case "books" -> booksPage.refresh();
            case "readers" -> readersPage.refresh();
            case "borrow" -> borrowPage.refresh();
            case "reports" -> reportsPage.refresh();
        }
    }

    private void refreshDashboard() {
        try {
            booksMetric.setText(String.valueOf(bookDAO.countBooks()));
        } catch (Exception ex) {
            booksMetric.setText("--");
        }
        try {
            readersMetric.setText(String.valueOf(readerDAO.countReaders()));
        } catch (Exception ex) {
            readersMetric.setText("--");
        }
        try {
            borrowingMetric.setText(String.valueOf(recordDAO.countBorrowing()));
        } catch (Exception ex) {
            borrowingMetric.setText("--");
        }

        try {
            List<String> topBooks = recordDAO.getTopBooks(5).entrySet().stream()
                    .map(entry -> String.format("%s — %d lượt", entry.getKey(), entry.getValue()))
                    .toList();
            topBooksList.setItems(FXCollections.observableArrayList(topBooks));
        } catch (Exception ex) {
            topBooksList.setItems(FXCollections.observableArrayList("Không có dữ liệu"));
        }

        try {
            List<String> topReaders = recordDAO.getTopReaders(5).entrySet().stream()
                    .map(entry -> String.format("%s — %d lượt", entry.getKey(), entry.getValue()))
                    .toList();
            topReadersList.setItems(FXCollections.observableArrayList(topReaders));
        } catch (Exception ex) {
            topReadersList.setItems(FXCollections.observableArrayList("Không có dữ liệu"));
        }

        try {
            List<String> overdue = recordDAO.getOverdueRecords().stream()
                    .map(record -> bookTitle(record.getBookID()) + " (" + readerName(record.getReaderID()) + ")")
                    .toList();
            overdueList.setItems(FXCollections.observableArrayList(overdue));
        } catch (Exception ex) {
            overdueList.setItems(FXCollections.observableArrayList("Không có dữ liệu"));
        }

        try {
            List<BorrowRecord> recent = recordDAO.getRecentRecords(8);
            recentTable.setItems(FXCollections.observableArrayList(recent));
        } catch (Exception ex) {
            recentTable.setItems(FXCollections.observableArrayList());
        }
    }
}
