package ui.fx.components;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.Objects;

/**
 * A reusable JavaFX control that couples a {@link TableView} with
 * a modern looking pagination bar. The control accepts an
 * {@link ObservableList} as its backing data source and automatically
 * builds slices of the list to show the current page.
 */
public class PagedTableView<T> extends VBox {

    private final TableView<T> tableView = new TableView<>();
    private final Button prevButton = new Button("‹");
    private final Button nextButton = new Button("›");
    private final Label pageLabel = new Label();
    private final ComboBox<Integer> pageSizeBox = new ComboBox<>();

    private final IntegerProperty pageSize = new SimpleIntegerProperty(this, "pageSize", 15);
    private final IntegerProperty currentPage = new SimpleIntegerProperty(this, "currentPage", 0);

    private ObservableList<T> source = FXCollections.observableArrayList();
    private final ListChangeListener<T> sourceListener = change -> refresh();

    public PagedTableView() {
        getStyleClass().add("paged-table");
        setSpacing(16);
        setFillWidth(true);

        tableView.getStyleClass().add("elevated-card");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.setPrefHeight(480);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        configureControls();

        getChildren().addAll(tableView, buildPaginationBar());
    }

    private void configureControls() {
        pageSizeBox.getItems().setAll(Arrays.asList(10, 15, 25, 50, 100));
        pageSizeBox.setValue(pageSize.get());
        pageSizeBox.valueProperty().addListener((obs, old, value) -> {
            if (value != null && !Objects.equals(value, old)) {
                setPageSize(value);
            }
        });

        prevButton.getStyleClass().addAll("ghost-button", "circle-button");
        nextButton.getStyleClass().addAll("ghost-button", "circle-button");

        prevButton.setOnAction(e -> setCurrentPage(currentPage.get() - 1));
        nextButton.setOnAction(e -> setCurrentPage(currentPage.get() + 1));

        pageSize.addListener((obs, old, value) -> refresh());
        currentPage.addListener((obs, old, value) -> refresh());
    }

    private HBox buildPaginationBar() {
        HBox box = new HBox(12);
        box.getStyleClass().add("pagination-bar");
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(4, 0, 0, 0));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label sizeLabel = new Label("Mỗi trang");
        sizeLabel.getStyleClass().add("muted-label");

        box.getChildren().addAll(prevButton, pageLabel, nextButton, spacer, sizeLabel, pageSizeBox);
        return box;
    }

    /**
     * Sets the backing source for this paged table.
     */
    public void setSource(ObservableList<T> items) {
        if (source != null) {
            source.removeListener(sourceListener);
        }
        source = items != null ? items : FXCollections.observableArrayList();
        source.addListener(sourceListener);
        setCurrentPage(0);
        refresh();
    }

    public TableView<T> getTableView() {
        return tableView;
    }

    public void setPlaceholder(Node node) {
        tableView.setPlaceholder(node);
    }

    public void refresh() {
        int totalItems = source != null ? source.size() : 0;
        int size = Math.max(1, pageSize.get());
        int totalPages = totalItems == 0 ? 1 : (int) Math.ceil(totalItems / (double) size);
        if (currentPage.get() >= totalPages) {
            currentPage.set(totalPages - 1);
            return;
        }
        if (currentPage.get() < 0) {
            currentPage.set(0);
            return;
        }

        int fromIndex = currentPage.get() * size;
        int toIndex = Math.min(fromIndex + size, totalItems);
        if (fromIndex > toIndex) {
            fromIndex = 0;
            toIndex = Math.min(size, totalItems);
        }

        if (source == null || source.isEmpty()) {
            tableView.getItems().clear();
            pageLabel.setText("Không có dữ liệu");
            prevButton.setDisable(true);
            nextButton.setDisable(true);
            return;
        }

        tableView.setItems(FXCollections.observableArrayList(source.subList(fromIndex, toIndex)));

        String label = String.format("Trang %d/%d • Hiển thị %d–%d trên %d",
                currentPage.get() + 1,
                totalPages,
                fromIndex + 1,
                toIndex,
                totalItems);
        pageLabel.setText(label);

        prevButton.setDisable(currentPage.get() <= 0);
        nextButton.setDisable(currentPage.get() >= totalPages - 1);
    }

    public void setPageSize(int size) {
        pageSize.set(Math.max(1, size));
        setCurrentPage(0);
    }

    public int getPageSize() {
        return pageSize.get();
    }

    public IntegerProperty pageSizeProperty() {
        return pageSize;
    }

    public void setCurrentPage(int page) {
        currentPage.set(Math.max(0, page));
    }

    public int getCurrentPage() {
        return currentPage.get();
    }

    public IntegerProperty currentPageProperty() {
        return currentPage;
    }
}
