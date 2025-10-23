package ui.fx;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import model.Book;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Dialog used for creating or editing a {@link Book} instance.
 */
public class BookForm extends Dialog<Book> {

    private final TextField titleField = new TextField();
    private final TextField authorField = new TextField();
    private final TextField isbnField = new TextField();
    private final TextField publisherField = new TextField();
    private final TextField pagesField = new TextField();
    private final TextField totalField = new TextField();
    private final TextField availableField = new TextField();
    private final TextField languageField = new TextField();
    private final TextField ratingField = new TextField();
    private final DatePicker publicationDatePicker = new DatePicker();

    private final Book original;

    public BookForm(Book book) {
        this.original = book;
        setTitle(book == null ? "Thêm sách mới" : "Cập nhật thông tin sách");
        setHeaderText(null);
        getDialogPane().getStyleClass().add("app-dialog");
        getDialogPane().getStylesheets().add(Objects.requireNonNull(
                LibraryApp.class.getResource("library-theme.css"))
                .toExternalForm());

        ButtonType saveButtonType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(12, 0, 0, 0));

        titleField.setPromptText("Tên sách");
        authorField.setPromptText("Tác giả");
        isbnField.setPromptText("ISBN");
        publisherField.setPromptText("Nhà xuất bản");
        pagesField.setPromptText("Số trang");
        totalField.setPromptText("Tổng số bản");
        availableField.setPromptText("Khả dụng");
        languageField.setPromptText("Ngôn ngữ");
        ratingField.setPromptText("Đánh giá trung bình");
        publicationDatePicker.setPromptText("Ngày phát hành");

        grid.add(new Label("Tên sách"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Tác giả"), 0, 1);
        grid.add(authorField, 1, 1);
        grid.add(new Label("ISBN"), 0, 2);
        grid.add(isbnField, 1, 2);
        grid.add(new Label("Nhà xuất bản"), 0, 3);
        grid.add(publisherField, 1, 3);
        grid.add(new Label("Số trang"), 0, 4);
        grid.add(pagesField, 1, 4);
        grid.add(new Label("Tổng số bản"), 0, 5);
        grid.add(totalField, 1, 5);
        grid.add(new Label("Có sẵn"), 0, 6);
        grid.add(availableField, 1, 6);
        grid.add(new Label("Ngôn ngữ"), 0, 7);
        grid.add(languageField, 1, 7);
        grid.add(new Label("Điểm đánh giá"), 0, 8);
        grid.add(ratingField, 1, 8);
        grid.add(new Label("Ngày phát hành"), 0, 9);
        grid.add(publicationDatePicker, 1, 9);

        getDialogPane().setContent(grid);

        Node saveButton = getDialogPane().lookupButton(saveButtonType);
        saveButton.getStyleClass().add("filled-button");

        if (book != null) {
            populateFields(book);
        } else {
            languageField.setText("vi");
            totalField.textProperty().addListener((obs, old, value) -> {
                if (availableField.getText().isBlank() || Objects.equals(old, availableField.getText())) {
                    availableField.setText(value);
                }
            });
        }

        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validate()) {
                event.consume();
            }
        });

        setResultConverter(button -> {
            if (button == saveButtonType) {
                return buildBook();
            }
            return null;
        });
    }

    private void populateFields(Book book) {
        titleField.setText(book.getTitle());
        authorField.setText(book.getAuthor());
        isbnField.setText(book.getIsbn());
        publisherField.setText(book.getPublisher());
        pagesField.setText(book.getNumPages() > 0 ? String.valueOf(book.getNumPages()) : "");
        totalField.setText(book.getTotal() > 0 ? String.valueOf(book.getTotal()) : "");
        availableField.setText(book.getAvailable() > 0 ? String.valueOf(book.getAvailable()) : "");
        languageField.setText(book.getLanguageCode());
        ratingField.setText(book.getAverageRating() > 0 ? String.valueOf(book.getAverageRating()) : "");
        publicationDatePicker.setValue(book.getPublicationDate());
    }

    private boolean validate() {
        if (titleField.getText().trim().isEmpty()) {
            showValidationError("Vui lòng nhập tên sách.");
            return false;
        }
        if (authorField.getText().trim().isEmpty()) {
            showValidationError("Vui lòng nhập tác giả.");
            return false;
        }
        if (!isEmptyOrNumber(pagesField.getText())) {
            showValidationError("Số trang không hợp lệ.");
            return false;
        }
        if (!isEmptyOrNumber(totalField.getText())) {
            showValidationError("Tổng số bản phải là số.");
            return false;
        }
        if (!isEmptyOrNumber(availableField.getText())) {
            showValidationError("Số lượng có sẵn phải là số.");
            return false;
        }
        if (!isEmptyOrDouble(ratingField.getText())) {
            showValidationError("Điểm đánh giá không hợp lệ.");
            return false;
        }
        LocalDate pubDate = publicationDatePicker.getValue();
        if (pubDate != null && pubDate.isAfter(LocalDate.now().plusDays(1))) {
            showValidationError("Ngày phát hành không được lớn hơn hiện tại.");
            return false;
        }
        return true;
    }

    private boolean isEmptyOrNumber(String value) {
        if (value == null || value.isBlank()) return true;
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean isEmptyOrDouble(String value) {
        if (value == null || value.isBlank()) return true;
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thiếu thông tin");
        alert.setHeaderText(message);
        alert.getDialogPane().getStylesheets().add(Objects.requireNonNull(
                LibraryApp.class.getResource("library-theme.css"))
                .toExternalForm());
        alert.showAndWait();
    }

    private Book buildBook() {
        Book book = original != null ? original : new Book();
        book.setTitle(titleField.getText().trim());
        book.setAuthor(authorField.getText().trim());
        book.setIsbn(isbnField.getText().trim());
        book.setPublisher(publisherField.getText().trim());
        book.setLanguageCode(languageField.getText().trim());
        book.setPublicationDate(publicationDatePicker.getValue());
        book.setNumPages(parseInt(pagesField.getText()));
        int total = parseInt(totalField.getText());
        int available = parseInt(availableField.getText());
        if (total > 0) {
            book.setTotal(total);
        }
        if (available > 0) {
            book.setAvailable(available);
        } else if (original == null) {
            book.setAvailable(total);
        }
        String ratingText = ratingField.getText();
        if (ratingText != null && !ratingText.isBlank()) {
            try {
                book.setAverageRating(Double.parseDouble(ratingText.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return book;
    }

    private int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
