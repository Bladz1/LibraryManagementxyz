package ui.fx;

import dao.BookDAO;
import dao.BorrowRecordDAO;
import dao.ReaderDAO;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.Book;
import model.BorrowRecord;
import model.Reader;
import ui.fx.components.PagedTableView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class BorrowPage extends BorderPane {

    private final BorrowRecordDAO borrowDAO;
    private final BookDAO bookDAO;
    private final ReaderDAO readerDAO;
    private final Runnable onRecordsChanged;

    private final ObservableList<BorrowRecord> masterData = FXCollections.observableArrayList();
    private final FilteredList<BorrowRecord> filteredData = new FilteredList<>(masterData, r -> true);

    private final PagedTableView<BorrowRecord> pagedTable = new PagedTableView<>();
    private final TextField searchField = new TextField();
    private final ToggleGroup statusGroup = new ToggleGroup();
    private final Label summaryLabel = new Label();

    private final Map<Integer, String> readerCache = new HashMap<>();
    private final Map<Integer, String> bookCache = new HashMap<>();

    public BorrowPage(BorrowRecordDAO borrowDAO, BookDAO bookDAO, ReaderDAO readerDAO, Runnable onRecordsChanged) {
        this.borrowDAO = borrowDAO;
        this.bookDAO = bookDAO;
        this.readerDAO = readerDAO;
        this.onRecordsChanged = onRecordsChanged != null ? onRecordsChanged : () -> {};

        getStyleClass().add("page");
        setPadding(new Insets(24));

        setTop(buildHeader());
        setCenter(buildTableArea());

        pagedTable.setSource(filteredData);
        pagedTable.setPlaceholder(new Label("Chưa có phiếu mượn nào."));
        configureTableColumns();
        configureFiltering();
    }

    private VBox buildHeader() {
        VBox box = new VBox(12);
        Label title = new Label("Quản lý mượn trả");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Theo dõi tình trạng mượn sách với phân trang hiện đại.");
        subtitle.getStyleClass().add("page-subtitle");

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("Tìm theo độc giả, sách hoặc trạng thái…");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button refreshButton = new Button("↻");
        refreshButton.getStyleClass().addAll("ghost-button", "circle-button");
        refreshButton.setOnAction(e -> refresh());

        Button addButton = new Button("＋ Tạo phiếu");
        addButton.getStyleClass().addAll("filled-button", "primary");
        addButton.setOnAction(e -> handleAdd());

        Button markButton = new Button("✔ Đánh dấu đã trả");
        markButton.getStyleClass().addAll("filled-button", "soft");
        Button deleteButton = new Button("🗑 Xóa");
        deleteButton.getStyleClass().addAll("filled-button", "danger");

        TableView<BorrowRecord> table = pagedTable.getTableView();
        markButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
            BorrowRecord selected = table.getSelectionModel().getSelectedItem();
            return selected == null || isReturned(selected);
        }, table.getSelectionModel().selectedItemProperty()));
        deleteButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        markButton.setOnAction(e -> handleMarkReturned());
        deleteButton.setOnAction(e -> handleDelete());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actions.getChildren().addAll(searchField, refreshButton, spacer, addButton, markButton, deleteButton);

        HBox filterRow = new HBox(8,
                createFilterToggle("Tất cả", "all"),
                createFilterToggle("Đang mượn", "borrowing"),
                createFilterToggle("Quá hạn", "overdue"),
                createFilterToggle("Đã trả", "returned"));
        filterRow.getStyleClass().add("segmented-toggle");

        summaryLabel.getStyleClass().add("muted-label");

        box.getChildren().addAll(title, subtitle, actions, filterRow, summaryLabel);
        return box;
    }

    private VBox buildTableArea() {
        VBox wrapper = new VBox(16);
        wrapper.getChildren().addAll(pagedTable);
        return wrapper;
    }

    private ToggleButton createFilterToggle(String text, String key) {
        ToggleButton button = new ToggleButton(text);
        button.setUserData(key);
        button.getStyleClass().add("nav-toggle");
        button.setToggleGroup(statusGroup);
        if (Objects.equals(key, "all")) {
            button.setSelected(true);
        }
        return button;
    }

    private void configureTableColumns() {
        TableView<BorrowRecord> table = pagedTable.getTableView();
        table.getColumns().clear();

        TableColumn<BorrowRecord, Number> idCol = new TableColumn<>("Mã phiếu");
        idCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getRecordID()));

        TableColumn<BorrowRecord, String> readerCol = new TableColumn<>("Độc giả");
        readerCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(readerName(cell.getValue().getReaderID())));

        TableColumn<BorrowRecord, String> bookCol = new TableColumn<>("Sách");
        bookCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(bookTitle(cell.getValue().getBookID())));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        TableColumn<BorrowRecord, String> borrowCol = new TableColumn<>("Ngày mượn");
        borrowCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatDate(cell.getValue().getBorrowDate(), formatter)));

        TableColumn<BorrowRecord, String> dueCol = new TableColumn<>("Hạn trả");
        dueCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatDate(cell.getValue().getDueDate(), formatter)));
        dueCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    BorrowRecord record = getTableView().getItems().get(getIndex());
                    Label label = new Label(item == null || item.isBlank() ? "-" : item);
                    label.getStyleClass().add("status-chip");
                    if (!isReturned(record) && isOverdue(record)) {
                        label.getStyleClass().add("chip-danger");
                    } else {
                        label.getStyleClass().add("chip-info");
                    }
                    setGraphic(label);
                    setText(null);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<BorrowRecord, String> returnCol = new TableColumn<>("Ngày trả");
        returnCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatDate(cell.getValue().getReturnDate(), formatter)));

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

        table.getColumns().addAll(idCol, readerCol, bookCol, borrowCol, dueCol, returnCol, statusCol);
    }

    private void configureFiltering() {
        searchField.textProperty().addListener((obs, old, value) -> applyFilter());
        statusGroup.selectedToggleProperty().addListener((obs, old, toggle) -> applyFilter());
    }

    private void applyFilter() {
        String keyword = searchField.getText();
        String statusKey = statusGroup.getSelectedToggle() != null
                ? Objects.toString(statusGroup.getSelectedToggle().getUserData(), "all")
                : "all";
        filteredData.setPredicate(record -> matchesKeyword(record, keyword) && matchesStatus(record, statusKey));
        pagedTable.refresh();
    }

    private boolean matchesKeyword(BorrowRecord record, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String lower = keyword.toLowerCase(Locale.ROOT);
        return readerName(record.getReaderID()).toLowerCase(Locale.ROOT).contains(lower)
                || bookTitle(record.getBookID()).toLowerCase(Locale.ROOT).contains(lower)
                || resolveStatus(record).toLowerCase(Locale.ROOT).contains(lower);
    }

    private boolean matchesStatus(BorrowRecord record, String statusKey) {
        return switch (statusKey) {
            case "borrowing" -> !isReturned(record) && !isOverdue(record);
            case "overdue" -> !isReturned(record) && isOverdue(record);
            case "returned" -> isReturned(record);
            default -> true;
        };
    }

    private String formatDate(LocalDate date, DateTimeFormatter formatter) {
        if (date == null) {
            return "-";
        }
        return formatter.format(date);
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

    private boolean isOverdue(BorrowRecord record) {
        LocalDate due = record.getDueDate();
        return due != null && due.isBefore(LocalDate.now()) && !isReturned(record);
    }

    private boolean isReturned(BorrowRecord record) {
        if (record.getReturnDate() != null) {
            return true;
        }
        String status = record.getStatus();
        return status != null && status.equalsIgnoreCase("returned");
    }

    private String resolveStatus(BorrowRecord record) {
        if (isReturned(record)) {
            return "Đã trả";
        }
        if (isOverdue(record)) {
            return "Quá hạn";
        }
        return "Đang mượn";
    }

    public void refresh() {
        try {
            List<BorrowRecord> records = borrowDAO.getAllRecords();
            masterData.setAll(records);
            updateSummary();
        } catch (Exception ex) {
            masterData.clear();
            updateSummary();
            showError("Không thể tải danh sách mượn trả", ex);
        }
        applyFilter();
    }

    private void updateSummary() {
        long total = masterData.size();
        long returned = masterData.stream().filter(this::isReturned).count();
        long overdue = masterData.stream().filter(r -> !isReturned(r) && isOverdue(r)).count();
        long borrowing = total - returned - overdue;
        summaryLabel.setText(String.format("Tổng %d phiếu • Đang mượn %d • Quá hạn %d • Đã trả %d",
                total, borrowing, overdue, returned));
    }

    private void handleAdd() {
        try {
            List<Reader> readers = readerDAO.getAllReaders();
            List<Book> books = bookDAO.getAllBooks();
            if (readers.isEmpty() || books.isEmpty()) {
                showInfo("Cần có ít nhất một độc giả và một cuốn sách để tạo phiếu.");
                return;
            }
            BorrowForm form = new BorrowForm(readers, books);
            form.initOwner(getScene() != null ? getScene().getWindow() : null);
            form.showAndWait().ifPresent(record -> {
                try {
                    borrowDAO.addBorrowRecord(record);
                    showInfo("Đã tạo phiếu mượn thành công.");
                    clearCaches();
                    refresh();
                    onRecordsChanged.run();
                } catch (Exception ex) {
                    showError("Không thể tạo phiếu mượn", ex);
                }
            });
        } catch (Exception ex) {
            showError("Không thể tải dữ liệu cần thiết", ex);
        }
    }

    private void handleMarkReturned() {
        BorrowRecord selected = pagedTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null || isReturned(selected)) {
            return;
        }
        try {
            borrowDAO.markReturned(selected.getRecordID());
            showInfo("Đã cập nhật trạng thái phiếu mượn.");
            clearCaches();
            refresh();
            onRecordsChanged.run();
        } catch (Exception ex) {
            showError("Không thể cập nhật trạng thái", ex);
        }
    }

    private void handleDelete() {
        BorrowRecord selected = pagedTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xóa phiếu mượn");
        alert.setHeaderText("Bạn chắc chắn muốn xóa phiếu #" + selected.getRecordID() + "?");
        alert.setContentText("Thao tác này không thể hoàn tác.");
        alert.getDialogPane().getStylesheets().add(Objects.requireNonNull(
                LibraryApp.class.getResource("library-theme.css"))
                .toExternalForm());
        if (alert.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
            try {
                borrowDAO.deleteRecord(selected.getRecordID());
                showInfo("Đã xóa phiếu mượn.");
                clearCaches();
                refresh();
                onRecordsChanged.run();
            } catch (Exception ex) {
                showError("Không thể xóa phiếu", ex);
            }
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Hoàn tất");
        alert.setHeaderText(message);
        alert.getDialogPane().getStylesheets().add(Objects.requireNonNull(
                LibraryApp.class.getResource("library-theme.css"))
                .toExternalForm());
        alert.showAndWait();
    }

    private void showError(String message, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Có lỗi xảy ra");
        alert.setHeaderText(message);
        if (ex != null && ex.getMessage() != null) {
            alert.setContentText(ex.getMessage());
        }
        alert.getDialogPane().getStylesheets().add(Objects.requireNonNull(
                LibraryApp.class.getResource("library-theme.css"))
                .toExternalForm());
        alert.showAndWait();
    }

    public void clearCaches() {
        readerCache.clear();
        bookCache.clear();
    }
}
