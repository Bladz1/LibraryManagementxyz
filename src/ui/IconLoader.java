package ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.Icon;

public final class IconLoader {
    private IconLoader() {
    }

    public static Icon load(String name, int size) {
        return new FlatSVGIcon("icons/" + name + ".svg", size, size);
    }
}
