package ui.events;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class EventBus {
    private static final EventBus INSTANCE = new EventBus();
    private final List<Consumer<AppEvent>> subscribers = new ArrayList<>();

    private EventBus() {
    }

    public static EventBus getInstance() {
        return INSTANCE;
    }

    public void subscribe(Consumer<AppEvent> subscriber) {
        subscribers.add(subscriber);
    }

    public void publish(AppEvent event) {
        for (Consumer<AppEvent> subscriber : new ArrayList<>(subscribers)) {
            subscriber.accept(event);
        }
    }
}
