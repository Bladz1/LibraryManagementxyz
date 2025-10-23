package ui;

import dao.BookDAO;
import dao.BorrowRecordDAO;
import dao.ReaderDAO;
import model.Book;
import model.BorrowRecord;
import model.Reader;
import net.miginfocom.swing.MigLayout;
import ui.events.AppEvent;
import ui.events.EventBus;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.List;

public class BorrowDialog extends JDialog {
    private JComboBox<Reader> readerCombo;
    private JComboBox<Book> bookCombo;
    private JTextField daysField;
    private final BorrowRecordDAO borrowRecordDAO;
    private final BookDAO bookDAO;
    private final ReaderDAO readerDAO;

    public BorrowDialog(JFrame parent, BorrowRecordDAO borrowRecordDAO, BookDAO bookDAO, ReaderDAO readerDAO) {
        super(parent, "Tạo Phiếu Mượn", true);
        this.borrowRecordDAO = borrowRecordDAO;
        this.bookDAO = bookDAO;
        this.readerDAO = readerDAO;

        initUI();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        JPanel form = new JPanel(new MigLayout("wrap 2, fillx", "[label, right]rel[grow, fill]"));

        List<Reader> readers = readerDAO.getAllReaders();
        List<Book> books = bookDAO.getAllBooks();

        readerCombo = new JComboBox<>(readers.toArray(new Reader[0]));
        readerCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                   boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Reader reader) {
                    setText(reader.getReaderID() + " - " + reader.getName());
                }
                return this;
            }
        });

        bookCombo = new JComboBox<>(books.toArray(new Book[0]));
        bookCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                   boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Book book) {
                    setText(book.getBookID() + " - " + book.getTitle());
                }
                return this;
            }
        });

        daysField = new JTextField("7");

        form.add(new JLabel("Độc giả:"));
        form.add(readerCombo, "growx");
        form.add(new JLabel("Sách:"));
        form.add(bookCombo, "growx");
        form.add(new JLabel("Số ngày mượn:"));
        form.add(daysField, "growx");

        JButton okBtn = new JButton("Xác nhận", IconLoader.load("save", 16));
        JButton cancelBtn = new JButton("Hủy", IconLoader.load("trash-2", 16));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(okBtn);
        buttons.add(cancelBtn);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        okBtn.addActionListener(e -> handleBorrow());
        cancelBtn.addActionListener(e -> dispose());
    }

    private void handleBorrow() {
        Reader reader = (Reader) readerCombo.getSelectedItem();
        Book book = (Book) bookCombo.getSelectedItem();
        if (reader == null || book == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn đầy đủ độc giả và sách.",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int days;
        try {
            days = Integer.parseInt(daysField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Số ngày mượn không hợp lệ!", "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocalDate borrowDate = LocalDate.now();
        LocalDate dueDate = borrowDate.plusDays(days);

        BorrowRecord record = new BorrowRecord();
        record.setReader(reader);
        record.setReaderID(reader.getReaderID());
        record.setBook(book);
        record.setBookID(book.getBookID());
        record.setBorrowDate(borrowDate);
        record.setDueDate(dueDate);
        record.setStatus("Đang mượn");

        borrowRecordDAO.addBorrowRecord(record);

        EventBus.getInstance().publish(new AppEvent(AppEvent.Type.BORROW_RECORD_CHANGED));
        JOptionPane.showMessageDialog(this, "✅ Tạo phiếu mượn thành công!", "Thành công",
                JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}
