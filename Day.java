package efc.model;

/**
 * Represents the two weekend days on which Elite Fitness Club
 * runs its group fitness classes.
 */
public enum Day {
    SATURDAY("Saturday"),
    SUNDAY("Sunday");

    private final String label;

    Day(String label) { this.label = label; }

    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}
