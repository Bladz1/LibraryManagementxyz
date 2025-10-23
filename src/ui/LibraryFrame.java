package ui;

import com.formdev.flatlaf.FlatLightLaf;
import dao.BookDAO;
import dao.BorrowRecordDAO;
import dao.ReaderDAO;
import net.miginfocom.swing.MigLayout;
import ui.events.AppEvent;
import ui.events.EventBus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import static ui.UiStyles.*;

public class LibraryFrame extends JFrame {
    private final JPanel content = new JPanel(new CardLayout());
    private final BookDAO bookDAO = new BookDAO();
    private final ReaderDAO readerDAO = new ReaderDAO();
    private final BorrowRecordDAO recordDAO = new BorrowRecordDAO();

    private JLabel statBooks;
    private JLabel statReaders;
    private JLabel statBorrowing;

    public LibraryFrame() {
        super("📚 Hệ thống quản lý thư viện");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 700));
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        buildSidebar();
        buildPages();
        showPage("dashboard");

        EventBus.getInstance().subscribe(this::handleAppEvent);
    }

    private void buildSidebar() {
        JPanel side = new JPanel(new MigLayout("wrap, fillx, insets 16", "[grow]"));
        side.setBackground(SIDEBAR_BG);
        side.setPreferredSize(new Dimension(240, 0));

        JLabel title = new JLabel("📚 Thư Viện");
        title.setFont(TITLE);
        side.add(title, "gapbottom 12");

        side.add(navBtn("Trang chủ", IconLoader.load("home", 20), () -> showPage("dashboard")), "growx");
        side.add(navBtn("Quản lý sách", IconLoader.load("book-open", 20), () -> showPage("books")), "growx");
        side.add(navBtn("Quản lý độc giả", IconLoader.load("users", 20), () -> showPage("readers")), "growx");
        side.add(navBtn("Mượn/Trả sách", IconLoader.load("git-pull-request", 20), () -> showPage("borrow")), "growx");
        side.add(navBtn("Báo cáo", IconLoader.load("bar-chart-2", 20), () -> showPage("reports")), "growx");

        add(side, BorderLayout.WEST);
    }

    private JComponent navBtn(String text, Icon icon, Runnable onClick) {
        JButton b = new JButton(text, icon);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setIconTextGap(12);
        b.setFont(H2.deriveFont(14f));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 15, 10, 15));
        b.addActionListener(e -> onClick.run());
        return b;
    }

    private void buildPages() {
        content.setBorder(new EmptyBorder(16, 16, 16, 16));
        content.setBackground(BG);
        add(content, BorderLayout.CENTER);

        content.add(buildDashboard(), "dashboard");
        content.add(new BookUI(this), "books");
        content.add(new ReaderUI(this), "readers");
        content.add(new BorrowRecordUI(this, recordDAO, bookDAO, readerDAO), "borrow");
        content.add(buildReports(), "reports");
    }

    private void showPage(String name) {
        ((CardLayout) content.getLayout()).show(content, name);
        if ("dashboard".equals(name)) {
            refreshDashboardStats();
        }
    }

    private JPanel buildDashboard() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setOpaque(false);

        JPanel metrics = new JPanel(new MigLayout("insets 0, gapx 16", "[grow][grow][grow]"));
        metrics.setOpaque(false);
        metrics.add(metric("Tổng số sách", statBooks = new JLabel("0"), PRIMARY), "grow");
        metrics.add(metric("Độc giả", statReaders = new JLabel("0"), SUCCESS), "grow");
        metrics.add(metric("Đang mượn", statBorrowing = new JLabel("0"), INFO), "grow");
        root.add(metrics, BorderLayout.NORTH);

        JPanel placeholder = new JPanel();
        placeholder.setOpaque(false);
        root.add(placeholder, BorderLayout.CENTER);

        return root;
    }

    private JPanel metric(String title, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        JLabel t = new JLabel(title);
        t.setFont(BODY);
        value.setFont(new Font("Segoe UI", Font.BOLD, 28));
        value.setForeground(accent);
        card.add(t, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildReports() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setOpaque(false);

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 14));
        root.add(new JScrollPane(area), BorderLayout.CENTER);

        JButton refresh = new JButton("↻ Làm mới báo cáo");
        refresh.setBackground(PRIMARY);
        refresh.setForeground(Color.WHITE);
        root.add(refresh, BorderLayout.NORTH);

        Runnable reload = () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("📚 Top 5 sách được mượn nhiều:\n");
            recordDAO.getTopBooks(5).forEach((bookTitle, count) ->
                    sb.append(String.format(" • %-50s %d lượt\n", bookTitle, count)));

            sb.append("\n👤 Top 5 độc giả tích cực:\n");
            recordDAO.getTopReaders(5).forEach((readerName, count) ->
                    sb.append(String.format(" • %-50s %d lượt\n", readerName, count)));

            area.setText(sb.toString());
        };

        refresh.addActionListener(e -> reload.run());
        reload.run();

        return root;
    }

    private void handleAppEvent(AppEvent event) {
        if (event.type == AppEvent.Type.BOOK_CHANGED
                || event.type == AppEvent.Type.READER_CHANGED
                || event.type == AppEvent.Type.BORROW_RECORD_CHANGED) {
            refreshDashboardStats();
        }
    }

    private void refreshDashboardStats() {
        if (statBooks != null) {
            statBooks.setText(String.valueOf(bookDAO.countBooks()));
        }
        if (statReaders != null) {
            statReaders.setText(String.valueOf(readerDAO.countReaders()));
        }
        if (statBorrowing != null) {
            statBorrowing.setText(String.valueOf(recordDAO.countBorrowing()));
        }
    }

    public static javax.swing.event.DocumentListener simpleChange(Runnable run) {
        return new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { run.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { run.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { run.run(); }
        };
    }

    public static void launch() {
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> new LibraryFrame().setVisible(true));
    }
}
