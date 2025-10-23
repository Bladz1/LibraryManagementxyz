package ui;

import dao.ReaderDAO;
import model.Reader;
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

public class ReaderUI extends JPanel {
    private final ReaderDAO readerDAO = new ReaderDAO();
    private final JFrame parentFrame;
    private JTable table;
    private DefaultTableModel model;
    private JTextField searchField;

    public ReaderUI(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 0, 0));
        buildReaderPanel();
        loadData();
    }

    private void buildReaderPanel() {
        JPanel topPanel = surface(new MigLayout("fillx, insets 0, gapx 12", "[grow][][][]"));
        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText",
                "Tìm kiếm theo tên, email hoặc số điện thoại...");
        styleSearchField(searchField);

        JButton addButton = new JButton("Thêm mới", IconLoader.load("plus", 16));
        stylePrimaryButton(addButton);

        JButton editButton = new JButton("Cập nhật", IconLoader.load("edit-2", 16));
        styleWarningButton(editButton);

        JButton deleteButton = new JButton("Xóa", IconLoader.load("trash-2", 16));
        styleDangerButton(deleteButton);

        topPanel.add(searchField, "growx");
        topPanel.add(addButton);
        topPanel.add(editButton);
        topPanel.add(deleteButton);
        add(topPanel, BorderLayout.NORTH);

        String[] cols = {"Mã độc giả", "Tên", "Email", "Điện thoại", "Địa chỉ", "Ngày tham gia", "Trạng thái"};
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

        int[] widths = {90, 200, 220, 120, 260, 120, 120};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JPanel tableContainer = surface(new BorderLayout());
        tableContainer.add(wrapTable(table), BorderLayout.CENTER);
        add(tableContainer, BorderLayout.CENTER);

        searchField.getDocument().addDocumentListener(simpleChange(this::loadData));

        addButton.addActionListener(e -> {
            ReaderDialog dialog = new ReaderDialog(parentFrame, null);
            dialog.setVisible(true);
        });

        editButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn độc giả để cập nhật!", "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            int readerId = (int) table.getValueAt(selectedRow, 0);
            Reader reader = readerDAO.getReaderById(readerId);
            if (reader != null) {
                ReaderDialog dialog = new ReaderDialog(parentFrame, reader);
                dialog.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Không tìm thấy thông tin độc giả.",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        deleteButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn độc giả để xóa!", "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            int readerId = (int) table.getValueAt(selectedRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc muốn xóa độc giả này?", "Xác nhận xóa",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    readerDAO.deleteReader(readerId);
                    EventBus.getInstance().publish(new AppEvent(AppEvent.Type.READER_CHANGED));
                    JOptionPane.showMessageDialog(this, "Đã xóa độc giả thành công!");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi khi xóa độc giả: " + ex.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        EventBus.getInstance().subscribe(event -> {
            if (event.type == AppEvent.Type.READER_CHANGED) {
                loadData();
            }
        });
    }

    private void loadData() {
        model.setRowCount(0);
        String keyword = searchField.getText().trim();
        List<Reader> readers = keyword.isEmpty()
                ? readerDAO.getAllReaders()
                : readerDAO.searchReaders(keyword);

        for (Reader r : readers) {
            model.addRow(new Object[]{
                    r.getReaderID(),
                    r.getName(),
                    r.getEmail(),
                    r.getPhone(),
                    r.getAddress(),
                    r.getJoinDate() != null ? r.getJoinDate() : "",
                    r.isActive() ? "Hoạt động" : "Ngưng"
            });
        }
    }
}