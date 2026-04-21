package efc.model;

/**
 * Post-attendance feedback submitted by a member for a ClassSession.
 *
 * Rating scale:
 *   1 = Very Poor  |  2 = Poor  |  3 = Average  |  4 = Good  |  5 = Excellent
 *
 * A Feedback instance is immutable after creation — it is a value object.
 */
public class Feedback {

    private final String memberId;
    private final String sessionId;
    private final int    rating;     // 1 – 5
    private final String comment;

    public Feedback(String memberId, String sessionId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException(
                "Rating must be between 1 (Very Poor) and 5 (Excellent). Received: " + rating);
        }
        this.memberId  = memberId;
        this.sessionId = sessionId;
        this.rating    = rating;
        this.comment   = comment;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getMemberId()  { return memberId;  }
    public String getSessionId() { return sessionId; }
    public int    getRating()    { return rating;    }
    public String getComment()   { return comment;   }

    /** Converts the numeric rating to a human-readable descriptor. */
    public String getRatingDescription() {
        return switch (rating) {
            case 1 -> "Very Poor";
            case 2 -> "Poor";
            case 3 -> "Average";
            case 4 -> "Good";
            case 5 -> "Excellent";
            default -> "Unknown";
        };
    }

    @Override
    public String toString() {
        return String.format("%d/5 (%s) — \"%s\"", rating, getRatingDescription(), comment);
    }
}
