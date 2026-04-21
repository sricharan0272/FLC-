package efc.model;

/**
 * Lifecycle states for a Booking record.
 *
 * Only ATTENDED bookings contribute to attendance and revenue reports.
 * Once CANCELLED, a booking ID is permanently retired — it is never reused.
 */
public enum BookingStatus {
    BOOKED,
    MODIFIED,
    CANCELLED,
    ATTENDED
}
