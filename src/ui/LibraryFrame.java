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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;

import static ui.UiStyles.*;

public class LibraryFrame extends JFrame {
    private final JPanel content = new JPanel(new CardLayout());
    private final BookDAO bookDAO = new BookDAO();
    private final ReaderDAO readerDAO = new ReaderDAO();
    private final BorrowRecordDAO recordDAO = new BorrowRecordDAO();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private String currentPage = "";

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
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(SIDEBAR_BG);
        side.setPreferredSize(new Dimension(260, 0));
        side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, SIDEBAR_BORDER));

        JPanel contentPanel = new JPanel(new MigLayout("wrap, fillx, gapy 8, insets 24 20 24 20", "[grow]"));
        contentPanel.setOpaque(false);

        JLabel title = new JLabel("📚 Thư Viện Số");
        title.setFont(TITLE.deriveFont(24f));
        title.setForeground(TEXT);
        contentPanel.add(title, "gapbottom 2, growx");

        JLabel subtitle = new JLabel("Quản lý thông minh & đẳng cấp");
        subtitle.setFont(BODY);
        subtitle.setForeground(TEXT_MUTED);
        contentPanel.add(subtitle, "gapbottom 16, growx");

        contentPanel.add(sidebarBanner(), "growx, gapbottom 16");

        contentPanel.add(navBtn("dashboard", "Trang chủ", IconLoader.load("home", 20)), "growx");
        contentPanel.add(navBtn("books", "Quản lý sách", IconLoader.load("book-open", 20)), "growx");
        contentPanel.add(navBtn("readers", "Quản lý độc giả", IconLoader.load("users", 20)), "growx");
        contentPanel.add(navBtn("borrow", "Mượn/Trả sách", IconLoader.load("git-pull-request", 20)), "growx");
        contentPanel.add(navBtn("reports", "Báo cáo", IconLoader.load("bar-chart-2", 20)), "growx");

        side.add(contentPanel, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(12, 20, 24, 20));
        JLabel version = new JLabel("<html><b>Library Pro Edition</b><br/>Phiên bản 2024</html>");
        version.setFont(BODY.deriveFont(12f));
        version.setForeground(TEXT_MUTED);
        footer.add(version, BorderLayout.CENTER);
        side.add(footer, BorderLayout.SOUTH);

        add(side, BorderLayout.WEST);
    }

    private JComponent navBtn(String key, String text, Icon icon) {
        JButton b = new JButton(text, icon);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setIconTextGap(12);
        b.setFont(H2.deriveFont(Font.PLAIN, 14f));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(12, 18, 12, 18));
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.putClientProperty("JButton.arc", 24);
        b.setForeground(TEXT_MUTED);
        installNavHover(b, key);
        b.addActionListener(e -> showPage(key));
        navButtons.put(key, b);
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
        setActivePage(name);
        if ("dashboard".equals(name)) {
            refreshDashboardStats();
        }
    }

    private JPanel buildDashboard() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setOpaque(false);

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.add(buildHeroSection());
        stack.add(Box.createVerticalStrut(16));

        JPanel metrics = new JPanel(new MigLayout("insets 0, gapx 16", "[grow][grow][grow]"));
        metrics.setOpaque(false);
        metrics.add(metric("Tổng số sách", statBooks = new JLabel("0"), PRIMARY, IconLoader.load("book-open", 24)), "grow");
        metrics.add(metric("Độc giả", statReaders = new JLabel("0"), SUCCESS, IconLoader.load("users", 24)), "grow");
        metrics.add(metric("Đang mượn", statBorrowing = new JLabel("0"), INFO, IconLoader.load("git-pull-request", 24)), "grow");
        stack.add(metrics);

        root.add(stack, BorderLayout.NORTH);

        root.add(buildInsightCard(), BorderLayout.CENTER);

        return root;
    }

    private JPanel metric(String title, JLabel value, Color accent, Icon icon) {
        GradientPanel card = new GradientPanel(lighten(accent, 0.2), accent);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        card.putClientProperty("JComponent.roundRect", true);

        JLabel iconHolder = new JLabel(icon);
        iconHolder.setOpaque(true);
        iconHolder.setBackground(new Color(255, 255, 255, 45));
        iconHolder.setForeground(Color.WHITE);
        iconHolder.setHorizontalAlignment(SwingConstants.CENTER);
        iconHolder.setBorder(new EmptyBorder(10, 10, 10, 10));
        iconHolder.putClientProperty("JComponent.roundRect", true);

        JLabel t = new JLabel(title);
        t.setFont(H2.deriveFont(Font.PLAIN, 14f));
        t.setForeground(Color.WHITE);

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.add(iconHolder, BorderLayout.WEST);
        header.add(t, BorderLayout.CENTER);

        value.setFont(new Font("Segoe UI", Font.BOLD, 32));
        value.setForeground(Color.WHITE);

        card.add(header, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);

        JLabel hint = new JLabel("Cập nhật tức thời");
        hint.setFont(BODY.deriveFont(Font.PLAIN, 12f));
        hint.setForeground(new Color(255, 255, 255, 180));
        card.add(hint, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildReports() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setOpaque(false);

        JPanel card = surface(new BorderLayout(0, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel heading = new JLabel("📈 Báo cáo hoạt động thư viện");
        heading.setFont(TITLE.deriveFont(18f));
        heading.setForeground(TEXT);
        header.add(heading, BorderLayout.WEST);

        JButton refresh = new JButton("↻ Làm mới báo cáo");
        stylePrimaryButton(refresh);
        header.add(refresh, BorderLayout.EAST);

        card.add(header, BorderLayout.NORTH);

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 14));
        area.setForeground(TEXT);
        area.setBackground(Color.WHITE);
        area.setBorder(new EmptyBorder(12, 16, 12, 16));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        card.add(scroll, BorderLayout.CENTER);

        JLabel caption = new JLabel("Dữ liệu được tổng hợp theo thời gian thực từ các phiếu mượn");
        caption.setFont(BODY.deriveFont(Font.PLAIN, 12f));
        caption.setForeground(TEXT_MUTED);
        card.add(caption, BorderLayout.SOUTH);

        root.add(card, BorderLayout.CENTER);

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

    private void setActivePage(String page) {
        currentPage = page;
        navButtons.forEach((key, button) -> {
            boolean active = key.equals(page);
            if (active) {
                button.setOpaque(true);
                button.setContentAreaFilled(true);
                button.setBackground(PRIMARY);
                button.setForeground(Color.WHITE);
                button.setBorder(new EmptyBorder(14, 20, 14, 20));
            } else {
                button.setOpaque(false);
                button.setContentAreaFilled(false);
                button.setForeground(TEXT_MUTED);
                button.setBorder(new EmptyBorder(12, 18, 12, 18));
            }
        });
    }

    private void installNavHover(JButton button, String key) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!key.equals(currentPage)) {
                    button.setOpaque(true);
                    button.setContentAreaFilled(true);
                    button.setBackground(SIDEBAR_ACTIVE);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!key.equals(currentPage)) {
                    button.setOpaque(false);
                    button.setContentAreaFilled(false);
                }
            }
        });
    }

    private JComponent sidebarBanner() {
        GradientPanel banner = new GradientPanel(PRIMARY_DARK, PRIMARY);
        banner.setLayout(new BorderLayout(0, 10));
        banner.setBorder(new EmptyBorder(20, 20, 20, 20));
        banner.putClientProperty("JComponent.roundRect", true);

        JLabel heading = new JLabel("Xin chào, quản trị viên! ✨");
        heading.setFont(TITLE.deriveFont(18f));
        heading.setForeground(Color.WHITE);

        JLabel desc = new JLabel("Theo dõi thư viện của bạn một cách đẳng cấp.");
        desc.setFont(BODY);
        desc.setForeground(new Color(240, 249, 255));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(heading);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(desc);

        banner.add(textPanel, BorderLayout.CENTER);

        JButton cta = new JButton("🚀 Khám phá ngay");
        styleGhostButton(cta);
        cta.addActionListener(e -> showPage("dashboard"));
        JPanel action = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        action.setOpaque(false);
        action.add(cta);
        banner.add(action, BorderLayout.SOUTH);

        return banner;
    }

    private JComponent buildHeroSection() {
        GradientPanel hero = new GradientPanel(PRIMARY, PRIMARY_DARK);
        hero.setLayout(new BorderLayout(0, 16));
        hero.setBorder(new EmptyBorder(28, 32, 28, 32));
        hero.putClientProperty("JComponent.roundRect", true);

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("Tổng quan thư viện ✨");
        heading.setFont(TITLE.deriveFont(26f));
        heading.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Cập nhật nhanh tình hình hoạt động hôm nay.");
        subtitle.setFont(BODY.deriveFont(Font.PLAIN, 14f));
        subtitle.setForeground(new Color(226, 232, 255));

        texts.add(heading);
        texts.add(Box.createVerticalStrut(6));
        texts.add(subtitle);

        hero.add(texts, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actions.setOpaque(false);

        JButton addBook = new JButton("➕ Thêm sách mới");
        styleGhostButton(addBook);
        addBook.addActionListener(e -> showPage("books"));

        JButton addReader = new JButton("👤 Thêm độc giả");
        styleGhostButton(addReader);
        addReader.addActionListener(e -> showPage("readers"));

        JButton viewReport = new JButton("📊 Xem báo cáo");
        styleGhostButton(viewReport);
        viewReport.addActionListener(e -> showPage("reports"));

        actions.add(addBook);
        actions.add(addReader);
        actions.add(viewReport);

        hero.add(actions, BorderLayout.SOUTH);

        return hero;
    }

    private JPanel buildInsightCard() {
        JPanel card = surface(new BorderLayout(0, 16));

        JLabel heading = new JLabel("✨ Gợi ý quản trị");
        heading.setFont(TITLE.deriveFont(18f));
        heading.setForeground(TEXT);
        card.add(heading, BorderLayout.NORTH);

        JPanel tips = new JPanel();
        tips.setOpaque(false);
        tips.setLayout(new BoxLayout(tips, BoxLayout.Y_AXIS));
        tips.add(tipLabel("Theo dõi yêu cầu mượn để xử lý trong ngày.", "git-pull-request"));
        tips.add(tipLabel("Cập nhật thông tin sách mới nhất để thu hút độc giả.", "book-open"));
        tips.add(tipLabel("Tạo báo cáo định kỳ cho ban quản trị.", "bar-chart-2"));
        card.add(tips, BorderLayout.CENTER);

        JLabel footer = new JLabel("Hệ thống được tinh chỉnh cho trải nghiệm đẳng cấp.");
        footer.setFont(BODY.deriveFont(Font.PLAIN, 12f));
        footer.setForeground(TEXT_MUTED);
        card.add(footer, BorderLayout.SOUTH);

        return card;
    }

    private JLabel tipLabel(String text, String icon) {
        JLabel label = new JLabel(text, IconLoader.load(icon, 18), SwingConstants.LEFT);
        label.setFont(BODY);
        label.setForeground(TEXT_MUTED);
        label.setBorder(new EmptyBorder(6, 0, 6, 0));
        return label;
    }

    private Color lighten(Color color, double factor) {
        int r = (int) Math.min(255, color.getRed() + (255 - color.getRed()) * factor);
        int g = (int) Math.min(255, color.getGreen() + (255 - color.getGreen()) * factor);
        int b = (int) Math.min(255, color.getBlue() + (255 - color.getBlue()) * factor);
        return new Color(r, g, b, color.getAlpha());
    }

    private static class GradientPanel extends JPanel {
        private final Color start;
        private final Color end;

        GradientPanel(Color start, Color end) {
            this.start = start;
            this.end = end;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint paint = new GradientPaint(0, 0, start, getWidth(), getHeight(), end);
            g2.setPaint(paint);
            Shape shape = new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 28, 28);
            g2.fill(shape);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
