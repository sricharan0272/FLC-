package efc.model;

/**
 * The three daily time windows available for fitness classes.
 * Used to detect scheduling conflicts when a member tries to
 * book two classes at the same time on the same day.
 */
public enum TimeSlot {
    MORNING("Morning",   "07:00 – 08:30"),
    MIDDAY ("Midday",    "12:00 – 13:30"),
    EVENING("Evening",   "18:00 – 19:30");

    private final String label;
    private final String window;

    TimeSlot(String label, String window) {
        this.label  = label;
        this.window = window;
    }

    public String getLabel()  { return label; }
    public String getWindow() { return window; }

    @Override
    public String toString() { return label + " (" + window + ")"; }
}
