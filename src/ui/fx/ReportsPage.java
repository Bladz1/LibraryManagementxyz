package ui.fx;

import dao.BookDAO;
import dao.BorrowRecordDAO;
import dao.ReaderDAO;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.BorrowRecord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import javafx.scene.text.TextAlignment;

public class ReportsPage extends BorderPane {

    private final BorrowRecordDAO recordDAO;
    private final BookDAO bookDAO;
    private final ReaderDAO readerDAO;

    private final ListView<String> topBooksList = new ListView<>();
    private final ListView<String> topReadersList = new ListView<>();
    private final TableView<BorrowRecord> overdueTable = new TableView<>();
    private final TextArea summaryArea = new TextArea();

    private final Map<Integer, String> readerCache = new HashMap<>();
    private final Map<Integer, String> bookCache = new HashMap<>();

    public ReportsPage(BorrowRecordDAO recordDAO, BookDAO bookDAO, ReaderDAO readerDAO) {
        this.recordDAO = recordDAO;
        this.bookDAO = bookDAO;
        this.readerDAO = readerDAO;
        getStyleClass().add("page");
        setPadding(new Insets(24));

        setCenter(buildContent());
        configureLists();
        configureTable();
        refresh();
    }

    private VBox buildContent() {
        VBox root = new VBox(16);
        Label title = new Label("Báo cáo & Thống kê");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Cập nhật nhanh hoạt động của thư viện với top sách, độc giả và cảnh báo quá hạn.");
        subtitle.getStyleClass().add("page-subtitle");

        Button refreshButton = new Button("↻ Làm mới");
        refreshButton.getStyleClass().addAll("filled-button", "soft");
        refreshButton.setOnAction(e -> refresh());

        Region spacer = new Region();
        HBox headerRow = new HBox(12, subtitle, spacer, refreshButton);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox listRow = new HBox(16,
                createCard("Top sách được mượn nhiều", topBooksList),
                createCard("Độc giả tích cực", topReadersList));
        listRow.setPrefHeight(220);

        VBox overdueCard = createOverdueCard();

        summaryArea.setEditable(false);
        summaryArea.setWrapText(true);
        summaryArea.getStyleClass().add("code-area");
        summaryArea.setPrefRowCount(6);

        VBox summaryCard = new VBox(12, new Label("Tổng quan hệ thống"), summaryArea);
        summaryCard.getStyleClass().add("card");

        root.getChildren().addAll(title, headerRow, listRow, overdueCard, summaryCard);
        return root;
    }

    private void configureLists() {
        topBooksList.getStyleClass().add("stat-list");
        topBooksList.setFocusTraversable(false);
        topBooksList.setCellFactory(view -> new ListCell<>() {
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

        topReadersList.getStyleClass().add("stat-list");
        topReadersList.setFocusTraversable(false);
        topReadersList.setCellFactory(view -> new ListCell<>() {
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

    private void configureTable() {
        overdueTable.getStyleClass().add("elevated-card");
        overdueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        overdueTable.setPlaceholder(new Label("Không có bản ghi quá hạn."));

        TableColumn<BorrowRecord, Number> idCol = new TableColumn<>("Mã");
        idCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getRecordID()));
        idCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<BorrowRecord, String> readerCol = new TableColumn<>("Độc giả");
        readerCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(readerName(cell.getValue().getReaderID())));

        TableColumn<BorrowRecord, String> bookCol = new TableColumn<>("Sách");
        bookCol.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(bookTitle(cell.getValue().getBookID())));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        TableColumn<BorrowRecord, String> dueCol = new TableColumn<>("Hạn trả");
        dueCol.setCellValueFactory(cell -> {
            LocalDate due = cell.getValue().getDueDate();
            if (due == null) return new javafx.beans.property.SimpleStringProperty("-");
            return new javafx.beans.property.SimpleStringProperty(formatter.format(due));
        });
        dueCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<BorrowRecord, Number> daysCol = new TableColumn<>("Quá (ngày)");
        daysCol.setCellValueFactory(cell -> {
            LocalDate due = cell.getValue().getDueDate();
            int diff = 0;
            if (due != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(due, LocalDate.now());
                diff = (int) Math.max(0, days);
            }
            return new ReadOnlyObjectWrapper<>(diff);
        });
        daysCol.setStyle("-fx-alignment: CENTER;");

        overdueTable.getColumns().addAll(idCol, readerCol, bookCol, dueCol, daysCol);
    }

    private VBox createOverdueCard() {
        Label title = new Label("Cảnh báo quá hạn");
        VBox box = new VBox(12, title, overdueTable);
        box.getStyleClass().add("card");
        box.setPrefHeight(260);
        VBox.setVgrow(overdueTable, Priority.ALWAYS);
        return box;
    }

    private VBox createCard(String title, Control content) {
        Label heading = new Label(title);
        heading.getStyleClass().add("card-heading");
        heading.setMaxWidth(Double.MAX_VALUE);
        heading.setAlignment(Pos.CENTER);
        heading.setTextAlignment(TextAlignment.CENTER);
        VBox box = new VBox(12, heading, content);
        box.getStyleClass().add("card");
        box.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(content, Priority.ALWAYS);
        return box;
    }

    public void refresh() {
        Map<String, Integer> topBooks = recordDAO.getTopBooks(8);
        topBooksList.setItems(FXCollections.observableArrayList(formatEntries(topBooks)));

        Map<String, Integer> topReaders = recordDAO.getTopReaders(8);
        topReadersList.setItems(FXCollections.observableArrayList(formatEntries(topReaders)));

        List<BorrowRecord> overdueRecords = recordDAO.getOverdueRecords();
        overdueTable.setItems(FXCollections.observableArrayList(overdueRecords));

        List<BorrowRecord> allRecords = recordDAO.getAllRecords();
        long total = allRecords.size();
        long returned = allRecords.stream().filter(r -> r.getReturnDate() != null).count();
        long overdue = overdueRecords.size();
        long borrowing = total - returned - overdue;

        String summary = "📊 Số liệu thống kê hiện tại:\n" +
                String.format(" • Tổng số phiếu: %d\n", total) +
                String.format(" • Đang mượn: %d\n", borrowing) +
                String.format(" • Quá hạn: %d\n", overdue) +
                String.format(" • Đã trả: %d\n", returned);
        summaryArea.setText(summary);
    }

    private List<String> formatEntries(Map<String, Integer> data) {
        return data.entrySet().stream()
                .map(entry -> String.format("%s — %d lượt", entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public void clearCaches() {
        readerCache.clear();
        bookCache.clear();
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
}
