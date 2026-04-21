package efc.model;

/**
 * Records a single booking linking a {@link Member} to a {@link ClassSession}.
 *
 * Booking lifecycle:
 *   BOOKED → MODIFIED (if session is swapped)
 *          → CANCELLED (permanently retired)
 *          → ATTENDED  (after class + feedback submitted)
 *
 * Booking IDs are sequential and are NEVER reused after cancellation.
 */
public class Booking {

    private final String        bookingRef;   // e.g. "EFC-0001"
    private final String        memberId;
    private       String        sessionId;
    private       BookingStatus status;

    public Booking(String bookingRef, String memberId, String sessionId) {
        this.bookingRef = bookingRef;
        this.memberId   = memberId;
        this.sessionId  = sessionId;
        this.status     = BookingStatus.BOOKED;
    }

    // ── State transitions ─────────────────────────────────────────────────────

    /**
     * Moves the booking to a new session.
     * Caller must handle enrolment changes on old and new sessions.
     */
    public void reassign(String newSessionId) {
        this.sessionId = newSessionId;
        this.status    = BookingStatus.MODIFIED;
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
    }

    public void markAttended() {
        this.status = BookingStatus.ATTENDED;
    }

    // ── Predicates ────────────────────────────────────────────────────────────

    /** Returns true if this booking is still actionable (BOOKED or MODIFIED). */
    public boolean isActive() {
        return status == BookingStatus.BOOKED || status == BookingStatus.MODIFIED;
    }

    public boolean isCancelled() { return status == BookingStatus.CANCELLED; }
    public boolean isAttended()  { return status == BookingStatus.ATTENDED;  }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String        getBookingRef() { return bookingRef; }
    public String        getMemberId()   { return memberId;   }
    public String        getSessionId()  { return sessionId;  }
    public BookingStatus getStatus()     { return status;     }

    @Override
    public String toString() {
        return String.format("Booking[%s] Member:%s  Session:%s  Status:%s",
                bookingRef, memberId, sessionId, status);
    }
}
