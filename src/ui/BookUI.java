package ui;

import dao.BookDAO;
import model.Book;
import net.miginfocom.swing.MigLayout;
import ui.events.AppEvent;
import ui.events.EventBus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.List;

import static ui.LibraryFrame.simpleChange;
import static ui.UiStyles.*;

public class BookUI extends JPanel {
    private final BookDAO bookDAO = new BookDAO();
    private final JFrame parentFrame;
    private JTable table;
    private DefaultTableModel model;
    private JTextField searchField;

    public BookUI(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 0, 0));
        buildBookPanel();
        loadData();
    }

    private void buildBookPanel() {
        JPanel topPanel = surface(new MigLayout("fillx, insets 0, gapx 12", "[grow][][][]"));
        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Tìm kiếm theo tên sách, tác giả hoặc ISBN...");
        styleSearchField(searchField);

        JButton addButton = new JButton("Thêm mới", IconLoader.load("plus", 16));
        addButton.setToolTipText("Thêm sách mới");
        stylePrimaryButton(addButton);

        JButton editButton = new JButton("Cập nhật", IconLoader.load("edit-2", 16));
        editButton.setToolTipText("Cập nhật sách đang chọn");
        styleWarningButton(editButton);

        JButton deleteButton = new JButton("Xóa", IconLoader.load("trash-2", 16));
        deleteButton.setToolTipText("Xóa sách đang chọn");
        styleDangerButton(deleteButton);

        topPanel.add(searchField, "growx");
        topPanel.add(addButton);
        topPanel.add(editButton);
        topPanel.add(deleteButton);
        add(topPanel, BorderLayout.NORTH);

        String[] cols = {
                "Mã sách", "Tên sách", "Tác giả", "ISBN",
                "NXB", "Số trang", "Ngày XB", "Tổng", "Khả dụng"
        };
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        applyTableStyling(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        int[] widths = {70, 280, 180, 120, 160, 90, 110, 70, 80};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JPanel tableContainer = surface(new BorderLayout());
        tableContainer.add(wrapTable(table), BorderLayout.CENTER);
        add(tableContainer, BorderLayout.CENTER);

        searchField.getDocument().addDocumentListener(simpleChange(this::loadData));

        addButton.addActionListener(e -> {
            BookDialog dialog = new BookDialog(parentFrame, null);
            dialog.setVisible(true);
        });

        editButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một cuốn sách để cập nhật.",
                        "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int bookId = (int) table.getValueAt(selectedRow, 0);
            Book bookToEdit = bookDAO.findByID(bookId).orElse(null);
            if (bookToEdit != null) {
                BookDialog dialog = new BookDialog(parentFrame, bookToEdit);
                dialog.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Không tìm thấy thông tin sách.",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        deleteButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn sách cần xóa!", "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            int bookId = (int) table.getValueAt(selectedRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc muốn xóa sách này? Hành động này không thể hoàn tác.",
                    "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    bookDAO.deleteBook(bookId);
                    EventBus.getInstance().publish(new AppEvent(AppEvent.Type.BOOK_CHANGED));
                    JOptionPane.showMessageDialog(this, "Đã xóa sách thành công!");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi khi xóa: " + ex.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        EventBus.getInstance().subscribe(event -> {
            if (event.type == AppEvent.Type.BOOK_CHANGED) {
                loadData();
            }
        });
    }

    private void loadData() {
        model.setRowCount(0);
        String keyword = searchField.getText().trim();
        List<Book> books = keyword.isEmpty()
                ? bookDAO.getAllBooks()
                : bookDAO.searchBooks(keyword);

        for (Book b : books) {
            model.addRow(new Object[]{
                    b.getBookID(),
                    b.getTitle(),
                    b.getAuthor(),
                    b.getIsbn(),
                    b.getPublisher(),
                    b.getNumPages(),
                    b.getPublicationDate() != null ? b.getPublicationDate() : "",
                    b.getTotal(),
                    b.getAvailable()
            });
        }
    }
}