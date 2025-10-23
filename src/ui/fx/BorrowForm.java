package ui.fx;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import model.Book;
import model.BorrowRecord;
import model.Reader;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class BorrowForm extends Dialog<BorrowRecord> {

    private final ComboBox<Reader> readerBox = new ComboBox<>();
    private final ComboBox<Book> bookBox = new ComboBox<>();
    private final DatePicker borrowDatePicker = new DatePicker();
    private final DatePicker dueDatePicker = new DatePicker();

    public BorrowForm(List<Reader> readers, List<Book> books) {
        setTitle("Tạo phiếu mượn sách");
        setHeaderText(null);
        getDialogPane().getStyleClass().add("app-dialog");
        getDialogPane().getStylesheets().add(Objects.requireNonNull(
                LibraryApp.class.getResource("library-theme.css"))
                .toExternalForm());

        ButtonType saveType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(12, 0, 0, 0));

        readerBox.setItems(FXCollections.observableArrayList(readers));
        readerBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Reader reader) {
                if (reader == null) return "";
                return reader.getReaderID() + " - " + reader.getName();
            }

            @Override
            public Reader fromString(String string) {
                return null;
            }
        });
        readerBox.setPromptText("Chọn độc giả");

        bookBox.setItems(FXCollections.observableArrayList(books));
        bookBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Book book) {
                if (book == null) return "";
                return book.getBookID() + " - " + book.getTitle();
            }

            @Override
            public Book fromString(String string) {
                return null;
            }
        });
        bookBox.setPromptText("Chọn sách");

        borrowDatePicker.setValue(LocalDate.now());
        borrowDatePicker.setPromptText("Ngày mượn");
        dueDatePicker.setValue(LocalDate.now().plusWeeks(2));
        dueDatePicker.setPromptText("Hạn trả");

        grid.add(new Label("Độc giả"), 0, 0);
        grid.add(readerBox, 1, 0);
        grid.add(new Label("Sách"), 0, 1);
        grid.add(bookBox, 1, 1);
        grid.add(new Label("Ngày mượn"), 0, 2);
        grid.add(borrowDatePicker, 1, 2);
        grid.add(new Label("Hạn trả"), 0, 3);
        grid.add(dueDatePicker, 1, 3);

        getDialogPane().setContent(grid);

        Node saveButton = getDialogPane().lookupButton(saveType);
        saveButton.getStyleClass().add("filled-button");
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validate()) {
                event.consume();
            }
        });

        setResultConverter(button -> {
            if (button == saveType) {
                BorrowRecord record = new BorrowRecord();
                record.setReaderID(readerBox.getValue().getReaderID());
                record.setBookID(bookBox.getValue().getBookID());
                record.setBorrowDate(borrowDatePicker.getValue());
                record.setDueDate(dueDatePicker.getValue());
                record.setStatus("borrowed");
                return record;
            }
            return null;
        });
    }

    private boolean validate() {
        if (readerBox.getValue() == null) {
            showWarning("Vui lòng chọn độc giả.");
            return false;
        }
        if (bookBox.getValue() == null) {
            showWarning("Vui lòng chọn sách.");
            return false;
        }
        LocalDate borrow = borrowDatePicker.getValue();
        LocalDate due = dueDatePicker.getValue();
        if (borrow == null) {
            showWarning("Ngày mượn không được để trống.");
            return false;
        }
        if (due == null || due.isBefore(borrow)) {
            showWarning("Hạn trả phải lớn hơn hoặc bằng ngày mượn.");
            return false;
        }
        return true;
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thiếu thông tin");
        alert.setHeaderText(message);
        alert.getDialogPane().getStylesheets().add(Objects.requireNonNull(
                LibraryApp.class.getResource("library-theme.css"))
                .toExternalForm());
        alert.showAndWait();
    }
}
