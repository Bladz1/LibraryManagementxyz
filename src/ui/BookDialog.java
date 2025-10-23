package ui;

import dao.BookDAO;
import model.Book;
import net.miginfocom.swing.MigLayout;
import ui.events.AppEvent;
import ui.events.EventBus;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class BookDialog extends JDialog {
    private final BookDAO bookDAO = new BookDAO();
    private final Book currentBook;

    private final JTextField titleField = new JTextField(30);
    private final JTextField authorField = new JTextField();
    private final JTextField isbnField = new JTextField();
    private final JTextField publisherField = new JTextField();
    private final JSpinner pagesSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));
    private final JTextField dateField = new JTextField("yyyy-MM-dd");
    private final JSpinner totalSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));

    public BookDialog(JFrame parent, Book bookToEdit) {
        super(parent, true);
        this.currentBook = bookToEdit;

        setTitle(isEditing() ? "Cập nhật thông tin sách" : "Thêm sách mới");
        initUI();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);

        if (isEditing()) {
            populateFields();
        }
    }

    private boolean isEditing() {
        return currentBook != null;
    }

    private void populateFields() {
        titleField.setText(currentBook.getTitle());
        authorField.setText(currentBook.getAuthor());
        isbnField.setText(currentBook.getIsbn());
        publisherField.setText(currentBook.getPublisher());
        pagesSpinner.setValue(Math.max(1, currentBook.getNumPages()));
        if (currentBook.getPublicationDate() != null) {
            dateField.setText(currentBook.getPublicationDate().toString());
        }
        totalSpinner.setValue(Math.max(1, currentBook.getTotal() > 0 ? currentBook.getTotal() : 1));
        isbnField.setEditable(false);
    }

    private void initUI() {
        JPanel panel = new JPanel(new MigLayout("wrap 2, fillx", "[label, right]rel[grow, fill]", ""));

        panel.add(new JLabel("Tên sách:"));
        panel.add(titleField, "growx");
        panel.add(new JLabel("Tác giả:"));
        panel.add(authorField, "growx");
        panel.add(new JLabel("ISBN:"));
        panel.add(isbnField, "growx");
        panel.add(new JLabel("Nhà xuất bản:"));
        panel.add(publisherField, "growx");
        panel.add(new JLabel("Số trang:"));
        panel.add(pagesSpinner);
        panel.add(new JLabel("Ngày xuất bản (yyyy-MM-dd):"));
        panel.add(dateField, "growx");
        panel.add(new JLabel("Tổng số lượng:"));
        panel.add(totalSpinner);

        JButton saveButton = new JButton("Lưu lại", IconLoader.load("save", 16));
        JButton cancelButton = new JButton("Hủy bỏ");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        setLayout(new BorderLayout(10, 10));
        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> save());
        cancelButton.addActionListener(e -> dispose());
    }

    private void save() {
        try {
            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            String isbn = isbnField.getText().trim();
            String publisher = publisherField.getText().trim();
            int pages = (Integer) pagesSpinner.getValue();
            LocalDate publicationDate = dateField.getText().isBlank() || "yyyy-MM-dd".equals(dateField.getText())
                    ? null
                    : LocalDate.parse(dateField.getText().trim());
            int total = (Integer) totalSpinner.getValue();

            if (title.isEmpty() || author.isEmpty() || isbn.isEmpty()) {
                throw new IllegalArgumentException("Tên sách, tác giả và ISBN không được để trống.");
            }

            Book target = isEditing() ? currentBook : new Book();
            target.setTitle(title);
            target.setAuthor(author);
            target.setIsbn(isbn);
            target.setPublisher(publisher);
            target.setNumPages(pages);
            target.setPublicationDate(publicationDate);

            if (isEditing()) {
                int borrowedCount = Math.max(0, target.getTotal() - target.getAvailable());
                int newTotal = total;
                if (newTotal < borrowedCount) {
                    throw new IllegalArgumentException(
                            "Tổng số lượng không thể nhỏ hơn số sách đang cho mượn (" + borrowedCount + ").");
                }
                target.setTotal(newTotal);
                target.setAvailable(newTotal - borrowedCount);
                bookDAO.updateBook(target);
            } else {
                target.setTotal(total);
                target.setAvailable(total);
                bookDAO.addBook(target);
            }

            EventBus.getInstance().publish(new AppEvent(AppEvent.Type.BOOK_CHANGED));
            JOptionPane.showMessageDialog(this, "✅ Thao tác thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "❌ Định dạng ngày không hợp lệ. Vui lòng nhập yyyy-MM-dd.",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "❌ Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
