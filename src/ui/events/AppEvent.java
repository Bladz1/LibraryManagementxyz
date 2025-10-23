package ui.events;

public class AppEvent {
    public enum Type {
        BOOK_CHANGED,
        READER_CHANGED,
        BORROW_RECORD_CHANGED
    }

    public final Type type;

    public AppEvent(Type type) {
        this.type = type;
    }
}
