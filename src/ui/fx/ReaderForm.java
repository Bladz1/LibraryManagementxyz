package ui.fx;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import model.Reader;

import java.time.LocalDate;
import java.util.Objects;

public class ReaderForm extends Dialog<Reader> {

    private final TextField nameField = new TextField();
    private final TextField emailField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField addressField = new TextField();
    private final DatePicker joinDatePicker = new DatePicker();
    private final CheckBox activeBox = new CheckBox("Đang hoạt động");

    private final Reader original;

    public ReaderForm(Reader reader) {
        this.original = reader;
        setTitle(reader == null ? "Thêm độc giả" : "Cập nhật thông tin độc giả");
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

        nameField.setPromptText("Họ tên");
        emailField.setPromptText("Email");
        phoneField.setPromptText("Số điện thoại");
        addressField.setPromptText("Địa chỉ");
        joinDatePicker.setPromptText("Ngày tham gia");

        grid.add(new Label("Họ tên"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Email"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Số điện thoại"), 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(new Label("Địa chỉ"), 0, 3);
        grid.add(addressField, 1, 3);
        grid.add(new Label("Ngày tham gia"), 0, 4);
        grid.add(joinDatePicker, 1, 4);
        grid.add(activeBox, 1, 5);

        getDialogPane().setContent(grid);

        Node saveButton = getDialogPane().lookupButton(saveType);
        saveButton.getStyleClass().add("filled-button");

        if (reader != null) {
            populate(reader);
        } else {
            joinDatePicker.setValue(LocalDate.now());
            activeBox.setSelected(true);
        }

        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validate()) {
                event.consume();
            }
        });

        setResultConverter(button -> {
            if (button == saveType) {
                return buildReader();
            }
            return null;
        });
    }

    private void populate(Reader reader) {
        nameField.setText(reader.getName());
        emailField.setText(reader.getEmail());
        phoneField.setText(reader.getPhone());
        addressField.setText(reader.getAddress());
        joinDatePicker.setValue(reader.getJoinDate());
        activeBox.setSelected(reader.isActive());
    }

    private boolean validate() {
        if (nameField.getText().trim().isEmpty()) {
            showWarning("Vui lòng nhập tên độc giả.");
            return false;
        }
        if (emailField.getText() != null && !emailField.getText().isBlank()
                && !emailField.getText().contains("@")) {
            showWarning("Email không hợp lệ.");
            return false;
        }
        if (phoneField.getText() != null && !phoneField.getText().isBlank()
                && !phoneField.getText().matches("[0-9+ ]{6,15}")) {
            showWarning("Số điện thoại không hợp lệ.");
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

    private Reader buildReader() {
        Reader reader = original != null ? original : new Reader();
        reader.setName(nameField.getText().trim());
        reader.setEmail(emailField.getText() != null ? emailField.getText().trim() : null);
        reader.setPhone(phoneField.getText() != null ? phoneField.getText().trim() : null);
        reader.setAddress(addressField.getText() != null ? addressField.getText().trim() : null);
        reader.setJoinDate(joinDatePicker.getValue() != null ? joinDatePicker.getValue() : LocalDate.now());
        reader.setActive(activeBox.isSelected());
        return reader;
    }
}
