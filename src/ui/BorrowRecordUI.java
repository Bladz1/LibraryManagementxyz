package ui;

import dao.BookDAO;
import dao.BorrowRecordDAO;
import dao.ReaderDAO;
import model.BorrowRecord;
import net.miginfocom.swing.MigLayout;
import ui.events.AppEvent;
import ui.events.EventBus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

import static ui.UiStyles.*;

public class BorrowRecordUI extends JPanel {
    private final BorrowRecordDAO recordDAO;
    private final BookDAO bookDAO;
    private final ReaderDAO readerDAO;
    private final JFrame parentFrame;

    private final BorrowTableModel tableModel = new BorrowTableModel();
    private JTable borrowTable;

    public BorrowRecordUI(JFrame parentFrame, BorrowRecordDAO recordDAO, BookDAO bookDAO, ReaderDAO readerDAO) {
        this.parentFrame = parentFrame;
        this.recordDAO = recordDAO;
        this.bookDAO = bookDAO;
        this.readerDAO = readerDAO;

        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 0, 0));
        buildBorrowPanel();
        refreshTable();

        EventBus.getInstance().subscribe(event -> {
            if (event.type == AppEvent.Type.BORROW_RECORD_CHANGED
                    || event.type == AppEvent.Type.BOOK_CHANGED
                    || event.type == AppEvent.Type.READER_CHANGED) {
                refreshTable();
            }
        });
    }

    private void buildBorrowPanel() {
        JPanel topPanel = surface(new MigLayout("fillx, insets 0, gapx 12", "[grow][]"));
        JButton create = new JButton("Tạo phiếu mượn", IconLoader.load("plus", 16));
        stylePrimaryButton(create);
        topPanel.add(create, "align right");
        add(topPanel, BorderLayout.NORTH);

        borrowTable = new JTable(tableModel);
        applyTableStyling(borrowTable);
        borrowTable.setFillsViewportHeight(true);
        borrowTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        int[] widths = {80, 120, 120, 110, 110, 110, 110};
        for (int i = 0; i < widths.length && i < borrowTable.getColumnCount(); i++) {
            borrowTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JPanel tableContainer = surface(new BorderLayout());
        tableContainer.add(wrapTable(borrowTable), BorderLayout.CENTER);
        add(tableContainer, BorderLayout.CENTER);

        JButton markReturned = new JButton("Trả sách");
        styleSuccessButton(markReturned);
        JPanel actions = surface(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.add(markReturned);
        add(actions, BorderLayout.SOUTH);

        markReturned.addActionListener(e -> {
            int row = borrowTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn phiếu mượn!", "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            BorrowRecord br = tableModel.getRecordAt(row);
            if (br == null) {
                return;
            }

            if (br.getReturnDate() != null) {
                JOptionPane.showMessageDialog(this, "Phiếu này đã được trả!", "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Xác nhận trả sách cho phiếu #" + br.getRecordID() + "?",
                    "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            recordDAO.markReturned(br.getRecordID());
            EventBus.getInstance().publish(new AppEvent(AppEvent.Type.BORROW_RECORD_CHANGED));
        });

        create.addActionListener(e -> {
            BorrowDialog dialog = new BorrowDialog(parentFrame, recordDAO, bookDAO, readerDAO);
            dialog.setVisible(true);
        });
    }

    public void refreshTable() {
        tableModel.setRecords(recordDAO.getAllRecordsSorted());
    }
}