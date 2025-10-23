package ui;

import dao.ReaderDAO;
import model.Reader;
import net.miginfocom.swing.MigLayout;
import ui.events.AppEvent;
import ui.events.EventBus;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;

public class ReaderDialog extends JDialog {
    private final ReaderDAO readerDAO = new ReaderDAO();
    private final Reader currentReader;

    private final JTextField nameField = new JTextField(30);
    private final JTextField emailField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JTextField addressField = new JTextField();
    private final JCheckBox activeCheck = new JCheckBox("Hoạt động");

    public ReaderDialog(JFrame parent, Reader readerToEdit) {
        super(parent, true);
        this.currentReader = readerToEdit;

        setTitle(isEditing() ? "Cập nhật độc giả" : "Thêm độc giả mới");
        initUI();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);

        if (isEditing()) {
            populateFields();
        } else {
            activeCheck.setSelected(true);
        }
    }

    private boolean isEditing() {
        return currentReader != null;
    }

    private void populateFields() {
        nameField.setText(currentReader.getName());
        emailField.setText(currentReader.getEmail());
        phoneField.setText(currentReader.getPhone());
        addressField.setText(currentReader.getAddress());
        activeCheck.setSelected(currentReader.isActive());
    }

    private void initUI() {
        JPanel form = new JPanel(new MigLayout("wrap 2, fillx", "[label, right]rel[grow, fill]"));

        form.add(new JLabel("Họ tên:"));
        form.add(nameField, "growx");
        form.add(new JLabel("Email:"));
        form.add(emailField, "growx");
        form.add(new JLabel("Số điện thoại:"));
        form.add(phoneField, "growx");
        form.add(new JLabel("Địa chỉ:"));
        form.add(addressField, "growx");
        form.add(new JLabel("Trạng thái:"));
        form.add(activeCheck, "left");

        JButton saveButton = new JButton("Lưu lại", IconLoader.load("save", 16));
        JButton cancelButton = new JButton("Hủy bỏ");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(saveButton);
        buttons.add(cancelButton);

        setLayout(new BorderLayout(10, 10));
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> save());
        cancelButton.addActionListener(e -> dispose());
    }

    private void save() {
        try {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String address = addressField.getText().trim();
            boolean active = activeCheck.isSelected();

            if (name.isEmpty()) {
                throw new IllegalArgumentException("Tên độc giả không được để trống.");
            }

            Reader target = isEditing() ? currentReader : new Reader();
            target.setName(name);
            target.setEmail(email);
            target.setPhone(phone);
            target.setAddress(address);
            target.setActive(active);

            if (isEditing()) {
                readerDAO.updateReader(target);
            } else {
                target.setJoinDate(LocalDate.now());
                readerDAO.addReader(target);
            }

            EventBus.getInstance().publish(new AppEvent(AppEvent.Type.READER_CHANGED));
            JOptionPane.showMessageDialog(this, "✅ Thao tác thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "❌ Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
