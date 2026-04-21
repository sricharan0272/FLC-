package efc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single scheduled group fitness class at Elite Fitness Club.
 *
 * Key rules enforced here:
 *   - Maximum 4 participants per session (MAX_SPOTS)
 *   - Fee is fixed per fitness type regardless of time slot
 *   - Feedback can only be submitted after attendance (enforced by BookingManager)
 */
public class ClassSession {

    public static final int MAX_SPOTS = 4;

    private final String   sessionId;
    private final String   fitnessType;   // e.g. "Pilates", "HIIT"
    private final Day      day;
    private final TimeSlot timeSlot;
    private final int      weekNumber;    // 1 – 8
    private final double   fee;           // price per participant

    /** Member IDs currently enrolled in this session (active bookings only). */
    private final List<String>   enrolledIds = new ArrayList<>();

    /** All feedback entries submitted for this session. */
    private final List<Feedback> feedbackList = new ArrayList<>();

    public ClassSession(String sessionId, String fitnessType, Day day,
                        TimeSlot timeSlot, int weekNumber, double fee) {
        this.sessionId   = sessionId;
        this.fitnessType = fitnessType;
        this.day         = day;
        this.timeSlot    = timeSlot;
        this.weekNumber  = weekNumber;
        this.fee         = fee;
    }

    // ── Enrolment helpers ────────────────────────────────────────────────────

    /** Returns true if the session has at least one free spot. */
    public boolean hasAvailability() {
        return enrolledIds.size() < MAX_SPOTS;
    }

    public int getFreeSpots() {
        return MAX_SPOTS - enrolledIds.size();
    }

    public boolean isMemberEnrolled(String memberId) {
        return enrolledIds.contains(memberId);
    }

    /**
     * Adds a member to the enrolled list.
     * Throws {@link IllegalStateException} if the session is full.
     */
    public void enrol(String memberId) {
        if (!hasAvailability()) {
            throw new IllegalStateException("Session is fully booked.");
        }
        if (isMemberEnrolled(memberId)) {
            throw new IllegalStateException("Member is already enrolled in this session.");
        }
        enrolledIds.add(memberId);
    }

    /**
     * Removes a member from the enrolled list.
     * Called on cancellation or modification.
     */
    public void withdraw(String memberId) {
        enrolledIds.remove(memberId);
    }

    // ── Feedback helpers ─────────────────────────────────────────────────────

    public void addFeedback(Feedback fb) {
        feedbackList.add(fb);
    }

    /**
     * Calculates the mean rating across all submitted feedback.
     * Returns 0.0 if no feedback exists yet.
     */
    public double getAverageRating() {
        if (feedbackList.isEmpty()) return 0.0;
        return feedbackList.stream()
                .mapToInt(Feedback::getRating)
                .average()
                .orElse(0.0);
    }

    /**
     * Attendance count is proxied by the number of feedback entries,
     * since feedback is only submitted on attendance.
     */
    public int getAttendanceCount() {
        return feedbackList.size();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String       getSessionId()   { return sessionId;   }
    public String       getFitnessType() { return fitnessType; }
    public Day          getDay()         { return day;         }
    public TimeSlot     getTimeSlot()    { return timeSlot;    }
    public int          getWeekNumber()  { return weekNumber;  }
    public double       getFee()         { return fee;         }

    public List<String>   getEnrolledIds()  { return Collections.unmodifiableList(enrolledIds);   }
    public List<Feedback> getFeedbackList() { return Collections.unmodifiableList(feedbackList);  }

    @Override
    public String toString() {
        return String.format(
            "%-10s | %-18s | %-9s | %-9s | Wk%-2d | £%5.2f | %d/%d spots",
            sessionId, fitnessType, day.getLabel(), timeSlot.getLabel(),
            weekNumber, fee, enrolledIds.size(), MAX_SPOTS
        );
    }
}
