package ui.fx;

import dao.ReaderDAO;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.Reader;
import ui.fx.components.PagedTableView;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ReadersPage extends BorderPane {

    private final ReaderDAO readerDAO;
    private final Runnable onReadersChanged;

    private final ObservableList<Reader> masterData = FXCollections.observableArrayList();
    private final FilteredList<Reader> filteredData = new FilteredList<>(masterData, r -> true);

    private final PagedTableView<Reader> pagedTable = new PagedTableView<>();
    private final TextField searchField = new TextField();

    public ReadersPage(ReaderDAO readerDAO, Runnable onReadersChanged) {
        this.readerDAO = readerDAO;
        this.onReadersChanged = onReadersChanged != null ? onReadersChanged : () -> {};
        getStyleClass().add("page");
        setPadding(new Insets(24));

        setTop(buildHeader());
        setCenter(buildTableArea());

        pagedTable.setSource(filteredData);
        pagedTable.setPlaceholder(new Label("Chưa có độc giả nào."));
        configureTableColumns();
        configureSearch();
    }

    private VBox buildHeader() {
        VBox box = new VBox(12);
        Label title = new Label("Quản lý độc giả");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Lưu trữ thông tin và trạng thái thành viên.");
        subtitle.getStyleClass().add("page-subtitle");

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("Tìm kiếm theo tên, email hoặc số điện thoại…");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button reloadButton = new Button("↻ Làm mới");
        reloadButton.getStyleClass().addAll("ghost-button", "pill-button");
        reloadButton.setOnAction(e -> refresh());

        Button addButton = new Button("＋ Thêm độc giả");
        addButton.getStyleClass().addAll("filled-button", "primary");
        addButton.setOnAction(e -> handleAdd());

        Button editButton = new Button("✎ Cập nhật");
        editButton.getStyleClass().addAll("filled-button", "soft");
        Button deleteButton = new Button("🗑 Xóa");
        deleteButton.getStyleClass().addAll("filled-button", "danger");

        TableView<Reader> table = pagedTable.getTableView();
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
        TableView<Reader> table = pagedTable.getTableView();
        table.getColumns().clear();

        TableColumn<Reader, Number> idCol = new TableColumn<>("Mã độc giả");
        idCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getReaderID()));

        TableColumn<Reader, String> nameCol = new TableColumn<>("Họ tên");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Reader, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Reader, String> phoneCol = new TableColumn<>("Điện thoại");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<Reader, String> addressCol = new TableColumn<>("Địa chỉ");
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));

        TableColumn<Reader, String> joinDateCol = new TableColumn<>("Ngày tham gia");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        joinDateCol.setCellValueFactory(cell -> {
            if (cell.getValue().getJoinDate() == null) {
                return new javafx.beans.property.SimpleStringProperty("-");
            }
            return new javafx.beans.property.SimpleStringProperty(cell.getValue().getJoinDate().format(formatter));
        });

        TableColumn<Reader, Boolean> activeCol = new TableColumn<>("Trạng thái");
        activeCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().isActive()));
        activeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label chip = new Label(item ? "Hoạt động" : "Tạm khóa");
                    chip.getStyleClass().addAll("status-chip", item ? "chip-success" : "chip-danger");
                    setGraphic(chip);
                    setText(null);
                }
            }
        });

        table.getColumns().addAll(idCol, nameCol, emailCol, phoneCol, addressCol, joinDateCol, activeCol);
    }

    private void configureSearch() {
        searchField.textProperty().addListener((obs, old, value) -> applyFilter());
    }

    private void applyFilter() {
        String keyword = searchField.getText();
        if (keyword == null || keyword.isBlank()) {
            filteredData.setPredicate(reader -> true);
        } else {
            String lower = keyword.toLowerCase(Locale.ROOT);
            filteredData.setPredicate(reader -> contains(reader.getName(), lower)
                    || contains(reader.getEmail(), lower)
                    || contains(reader.getPhone(), lower)
                    || contains(reader.getAddress(), lower));
        }
        pagedTable.refresh();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    public void refresh() {
        try {
            List<Reader> readers = readerDAO.getAllReaders();
            masterData.setAll(readers);
        } catch (Exception ex) {
            masterData.clear();
            showError("Không thể tải danh sách độc giả", ex);
        }
        applyFilter();
    }

    private void handleAdd() {
        ReaderForm dialog = new ReaderForm(null);
        dialog.initOwner(getScene() != null ? getScene().getWindow() : null);
        dialog.showAndWait().ifPresent(reader -> {
            try {
                readerDAO.addReader(reader);
                showInfo("Đã thêm độc giả mới.");
                refresh();
                onReadersChanged.run();
            } catch (Exception ex) {
                showError("Không thể thêm độc giả", ex);
            }
        });
    }

    private void handleEdit() {
        Reader selected = pagedTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Reader copy = new Reader(selected.getReaderID(), selected.getName(), selected.getEmail(),
                selected.getPhone(), selected.getAddress(), selected.getJoinDate(), selected.isActive());
        ReaderForm dialog = new ReaderForm(copy);
        dialog.initOwner(getScene() != null ? getScene().getWindow() : null);
        dialog.showAndWait().ifPresent(reader -> {
            try {
                reader.setReaderID(selected.getReaderID());
                readerDAO.updateReader(reader);
                showInfo("Đã cập nhật thông tin độc giả.");
                refresh();
                onReadersChanged.run();
            } catch (Exception ex) {
                showError("Không thể cập nhật độc giả", ex);
            }
        });
    }

    private void handleDelete() {
        Reader selected = pagedTable.getTableView().getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xóa độc giả");
        alert.setHeaderText("Bạn muốn xóa độc giả \"" + selected.getName() + "\"?");
        alert.setContentText("Hành động này sẽ xóa mọi lịch sử liên quan.");
        alert.getDialogPane().getStylesheets().add(Objects.requireNonNull(
                LibraryApp.class.getResource("library-theme.css"))
                .toExternalForm());

        if (alert.showAndWait().filter(ButtonType.OK::equals).isPresent()) {
            try {
                readerDAO.deleteReader(selected.getReaderID());
                showInfo("Đã xóa độc giả.");
                refresh();
                onReadersChanged.run();
            } catch (Exception ex) {
                showError("Không thể xóa độc giả", ex);
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
}
