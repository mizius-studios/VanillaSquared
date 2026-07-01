package blob.vanillasquared.main.gui.hud;

public final class LungingClientState {
    private static boolean active;

    private LungingClientState() {
    }

    public static boolean active() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }

    public static void clear() {
        active = false;
    }
}
