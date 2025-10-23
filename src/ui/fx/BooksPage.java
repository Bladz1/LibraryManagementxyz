package ui.fx;

import dao.BookDAO;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import model.Book;
import ui.fx.components.PagedTableView;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class BooksPage extends BorderPane {

    private final BookDAO bookDAO;
    private final Runnable onBooksChanged;

    private final ObservableList<Book> masterData = FXCollections.observableArrayList();
    private final FilteredList<Book> filteredData = new FilteredList<>(masterData, b -> true);

    private final PagedTableView<Book> pagedTable = new PagedTableView<>();
    private final TextField searchField = new TextField();
    private final Label emptyState = new Label("Không có sách trong thư viện.");

    public BooksPage(BookDAO bookDAO, Runnable onBooksChanged) {
        this.bookDAO = bookDAO;
        this.onBooksChanged = onBooksChanged != null ? onBooksChanged : () -> {};
        getStyleClass().add("page");
        setPadding(new Insets(24));

        setTop(buildHeader());
        setCenter(buildTableArea());

        pagedTable.setSource(filteredData);
        pagedTable.setPlaceholder(emptyState);
        configureTableColumns();
        configureSearch();
    }

    private VBox buildHeader() {
        VBox box = new VBox(12);
        Label title = new Label("Quản lý sách");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Theo dõi kho sách và thao tác nhanh với danh mục.");
        subtitle.getStyleClass().add("page-subtitle");

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("Tìm kiếm theo tên sách, tác giả hoặc ISBN…");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button reloadButton = new Button("↻ Làm mới");
        reloadButton.getStyleClass().addAll("ghost-button", "pill-button");
        reloadButton.setOnAction(e -> refresh());

        Button addButton = new Button("＋ Thêm sách");
        addButton.getStyleClass().addAll("filled-button", "primary");
        addButton.setOnAction(e -> handleAdd());

        Button editButton = new Button("✎ Cập nhật");
        editButton.getStyleClass().addAll("filled-button", "soft");

        Button deleteButton = new Button("🗑 Xóa");
        deleteButton.getStyleClass().addAll("filled-button", "danger");

        TableView<Book> table = pagedTable.getTableView();
        editButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        editButton.setOnAction(e -> handleEdit());
        deleteButton.setOnAction(e -> handleDelete());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actions.getChildren().addAll(searchField, reloadButton, spacer, addButton, editButton, deleteButton);

        box.getChildren().addAll(title, subtitle, actions);
        return box;
    }

    private VBox buildTableArea() {
        VBox wrapper = new VBox(16);
        wrapper.getChildren().addAll(pagedTable);
        return wrapper;
    }

    private void configureTableColumns() {
        TableView<Book> table = pagedTable.getTableView();
        table.getColumns().clear();

        TableColumn<Book, Number> idCol = new TableColumn<>("Mã sách");
        idCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getBookID()));

        TableColumn<Book, String> titleCol = new TableColumn<>("Tên sách");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Book, String> authorCol = new TableColumn<>("Tác giả");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));

        TableColumn<Book, String> isbnCol = new TableColumn<>("ISBN");
        isbnCol.setCellValueFactory(new PropertyValueFactory<>("isbn"));

        TableColumn<Book, String> publisherCol = new TableColumn<>("Nhà XB");
        publisherCol.setCellValueFactory(new PropertyValueFactory<>("publisher"));

        TableColumn<Book, Number> totalCol = new TableColumn<>("Tổng bản");
        totalCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getTotal()));

        TableColumn<Book, Number> availableCol = new TableColumn<>("Khả dụng");
        availableCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getAvailable()));

        TableColumn<Book, String> languageCol = new TableColumn<>("Ngôn ngữ");
        languageCol.setCellValueFactory(new PropertyValueFactory<>("languageCode"));

        TableColumn<Book, Number> pagesCol = new TableColumn<>("Số trang");
        pagesCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getNumPages()));

        TableColumn<Book, String> dateCol = new TableColumn<>("Phát hành");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        dateCol.setCellValueFactory(cell -> {
            if (cell.getValue().getPublicationDate() == null) {
                return new javafx.beans.property.SimpleStringProperty("-");
            }
            String formatted = cell.getValue().getPublicationDate().format(formatter);
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });

        TableColumn<Book, Number> ratingCol = new TableColumn<>("Đánh giá");
        ratingCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getAverageRating()));
        ratingCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.doubleValue() <= 0) {
                    setText(null);
                } else {
                    setText(String.format(Locale.US, "%.1f★", item.doubleValue()));
                }
            }
        });

        table.getColumns().addAll(idCol, titleCol, authorCol, isbnCol, publisherCol,
                pagesCol, totalCol, availableCol, languageCol, dateCol, ratingCol);
    }

    private void configureSearch() {
        searchField.textProperty().addListener((obs, old, value) -> applyFilter());
    }

    private void applyFilter() {
        String keyword = searchField.getText();
        if (keyword == null || keyword.isBlank()) {
            filteredData.setPredicate(book -> true);
        } else {
            String lower = keyword.toLowerCase(Locale.ROOT);
            filteredData.setPredicate(book -> contains(book.getTitle(), lower)
                    || contains(book.getAuthor(), lower)
                    || contains(book.getIsbn(), lower)
                    || contains(book.getPublisher(), lower));
        }
        pagedTable.refresh();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    public void refresh() {
        try {
            List<Book> books = bookDAO.getAllBooks();
            masterData.setAll(books);
        } catch (Exception ex) {
            masterData.clear();
            showError("Không thể tải danh sách sách", ex);
        }
        applyFilter();
    }

    private void handleAdd() {
        BookForm dialog = new BookForm(null);
        dialog.initOwner(getScene() != null ? getScene().getWindow() : null);
        dialog.showAndWait().ifPresent(book -> {
            try {
                bookDAO.addBook(book);
                showInfo("Đã thêm sách mới thành công!");
                refresh();
                onBooksChanged.run();
            } catch (Exception ex) {
                showError("Không thể thêm sách", ex);
            }
        });
    }

    private void handleEdit() {
        Book selected = pagedTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) return;

        BookForm dialog = new BookForm(selected);
        dialog.initOwner(getScene() != null ? getScene().getWindow() : null);
        dialog.showAndWait().ifPresent(book -> {
            try {
                book.setBookID(selected.getBookID());
                bookDAO.updateBook(book);
                showInfo("Đã cập nhật thông tin sách.");
                refresh();
                onBooksChanged.run();
            } catch (Exception ex) {
                showError("Không thể cập nhật sách", ex);
            }
        });
    }

    private void handleDelete() {
        Book selected = pagedTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xóa sách");
        alert.setHeaderText("Bạn chắc chắn muốn xóa sách \"" + selected.getTitle() + "\"?");
        alert.setContentText("Thao tác này không thể hoàn tác.");
        alert.getDialogPane().getStylesheets().add(Objects.requireNonNull(
                        LibraryApp.class.getResource("library-theme.css"))
                .toExternalForm());

        if (alert.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
            try {
                bookDAO.deleteBook(selected.getBookID());
                showInfo("Đã xóa sách thành công.");
                refresh();
                onBooksChanged.run();
            } catch (Exception ex) {
                showError("Không thể xóa sách", ex);
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
        if (ex != null && ex.getMessage() != null)
            alert.setContentText(ex.getMessage());
        alert.getDialogPane().getStylesheets().add(Objects.requireNonNull(
                        LibraryApp.class.getResource("library-theme.css"))
                .toExternalForm());
        alert.showAndWait();
    }
}
