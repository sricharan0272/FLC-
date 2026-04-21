package efc.service;

import efc.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central service controller for the Elite Fitness Club Booking System.
 *
 * Design pattern: Façade — single, clean API over all domain objects.
 *
 * All business rules enforced here:
 *   • Session capacity (max 4 per session)
 *   • Duplicate enrolment prevention
 *   • Time-slot conflict detection
 *   • Unique, non-recycled booking reference generation
 *   • Status lifecycle management
 *   • Report computation (attendance + revenue)
 */
public class BookingManager {

    // ── Domain stores ─────────────────────────────────────────────────────────

    private final Map<String, Member>  members  = new LinkedHashMap<>();
    private final Schedule             schedule = new Schedule();
    /** bookingRef → Booking  (refs are never removed, only status-updated) */
    private final Map<String, Booking> bookings = new LinkedHashMap<>();

    private int refCounter = 1;

    // ══════════════════════════════════════════════════════════════════════════
    //  MEMBER MANAGEMENT
    // ══════════════════════════════════════════════════════════════════════════

    public void registerMember(Member member) {
        members.put(member.getMemberId(), member);
    }

    public Optional<Member> lookupMember(String memberId) {
        return Optional.ofNullable(members.get(memberId));
    }

    public Collection<Member> getAllMembers() {
        return Collections.unmodifiableCollection(members.values());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SCHEDULE
    // ══════════════════════════════════════════════════════════════════════════

    public void addSession(ClassSession session) {
        schedule.addSession(session);
    }

    public Schedule getSchedule() { return schedule; }

    public List<ClassSession> viewByDay(Day day) {
        return schedule.getSessionsByDay(day);
    }

    public List<ClassSession> viewByFitnessType(String type) {
        return schedule.getSessionsByType(type);
    }

    public List<String> getAllFitnessTypes() {
        return schedule.getAllFitnessTypes();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BOOKING
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Creates a new booking for a member in a given session.
     *
     * Validations (in order):
     *  1. Member exists
     *  2. Session exists
     *  3. Session has a free spot
     *  4. Member has no active booking for this session already
     *  5. No time-slot conflict with other active bookings
     *
     * @return the newly created {@link Booking}
     * @throws IllegalArgumentException if member/session not found
     * @throws IllegalStateException    if any constraint is violated
     */
    public Booking placeBooking(String memberId, String sessionId) {
        Member       member  = requireMember(memberId);
        ClassSession session = requireSession(sessionId);

        // 1. Capacity check
        if (!session.hasAvailability()) {
            throw new IllegalStateException(
                "Error: Class is fully booked — '"
                + session.getFitnessType() + "' on "
                + session.getDay() + " " + session.getTimeSlot().getLabel()
                + " (Week " + session.getWeekNumber() + ") has no free spots.");
        }

        // 2. Duplicate check
        boolean alreadyBooked = bookings.values().stream()
                .anyMatch(b -> b.getMemberId().equals(memberId)
                            && b.getSessionId().equals(sessionId)
                            && b.isActive());
        if (alreadyBooked) {
            throw new IllegalStateException(
                "Error: " + member.getFullName()
                + " already has an active booking for session " + sessionId + ".");
        }

        // 3. Time-conflict check
        detectConflict(memberId, session, null);

        // All validations passed — create booking
        String  ref     = generateRef();
        Booking booking = new Booking(ref, memberId, sessionId);
        bookings.put(ref, booking);
        session.enrol(memberId);

        return booking;
    }

    /**
     * Modifies an existing booking by reassigning it to a different session.
     *
     * Rules:
     *   - Booking must be active
     *   - New session must differ from current
     *   - New session must have availability
     *   - No time-slot conflict after the move
     */
    public Booking modifyBooking(String bookingRef, String newSessionId) {
        Booking      booking    = requireActiveBooking(bookingRef);
        ClassSession oldSession = requireSession(booking.getSessionId());
        ClassSession newSession = requireSession(newSessionId);

        if (booking.getSessionId().equals(newSessionId)) {
            throw new IllegalArgumentException(
                "Error: The new session must be different from the current one.");
        }
        if (!newSession.hasAvailability()) {
            throw new IllegalStateException(
                "Error: The selected new class is fully booked. Modification not possible.");
        }

        // Check conflict ignoring the booking being moved
        detectConflict(booking.getMemberId(), newSession, bookingRef);

        // Apply the swap
        oldSession.withdraw(booking.getMemberId());
        newSession.enrol(booking.getMemberId());
        booking.reassign(newSessionId);

        return booking;
    }

    /**
     * Cancels an active booking and releases the spot.
     * The booking record is retained with CANCELLED status for audit purposes.
     */
    public void cancelBooking(String bookingRef) {
        Booking      booking = requireActiveBooking(bookingRef);
        ClassSession session = requireSession(booking.getSessionId());

        session.withdraw(booking.getMemberId());
        booking.cancel();
    }

    /**
     * Marks a booking as attended and records the member's feedback.
     *
     * @param rating  integer 1–5
     * @param comment free-text feedback comment
     */
    public void recordAttendance(String bookingRef, int rating, String comment) {
        Booking      booking = requireActiveBooking(bookingRef);
        ClassSession session = requireSession(booking.getSessionId());

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException(
                "Error: Rating must be between 1 and 5. Received: " + rating);
        }

        booking.markAttended();
        Feedback fb = new Feedback(booking.getMemberId(), session.getSessionId(), rating, comment);
        session.addFeedback(fb);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  REPORTS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Attendance Report — one row per session that has at least one attendee,
     * showing head count and average feedback rating.
     */
    public String buildAttendanceReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("═".repeat(86)).append("\n");
        sb.append("        ELITE FITNESS CLUB — CLASS ATTENDANCE & RATINGS REPORT\n");
        sb.append("═".repeat(86)).append("\n");
        sb.append(String.format("  %-10s %-20s %-10s %-10s %-6s %-10s %s%n",
                "Session", "Fitness Type", "Day", "Slot", "Week", "Attendees", "Avg Rating"));
        sb.append("  " + "─".repeat(82)).append("\n");

        schedule.getAllSessions().stream()
                .filter(s -> s.getAttendanceCount() > 0)
                .sorted(Comparator.comparingInt(ClassSession::getWeekNumber)
                        .thenComparing(ClassSession::getDay)
                        .thenComparing(ClassSession::getTimeSlot))
                .forEach(s -> sb.append(String.format(
                        "  %-10s %-20s %-10s %-10s %-6d %-10d %.2f / 5%n",
                        s.getSessionId(), s.getFitnessType(),
                        s.getDay().getLabel(), s.getTimeSlot().getLabel(),
                        s.getWeekNumber(), s.getAttendanceCount(),
                        s.getAverageRating())));

        sb.append("═".repeat(86)).append("\n");
        return sb.toString();
    }

    /**
     * Revenue Report — total income per fitness type (attended bookings only),
     * highlighting the top earner as the Champion.
     */
    public String buildRevenueReport() {
        // Accumulate revenue: fitnessType → total income
        Map<String, Double> revenueMap = new LinkedHashMap<>();

        for (ClassSession s : schedule.getAllSessions()) {
            if (s.getAttendanceCount() == 0) continue;
            double income = s.getFee() * s.getAttendanceCount();
            revenueMap.merge(s.getFitnessType(), income, Double::sum);
        }

        if (revenueMap.isEmpty()) {
            return "\n  No revenue data available yet — no attended sessions found.\n";
        }

        String champion = Collections.max(
                revenueMap.entrySet(), Map.Entry.comparingByValue()).getKey();

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("═".repeat(60)).append("\n");
        sb.append("        ELITE FITNESS CLUB — REVENUE REPORT\n");
        sb.append("═".repeat(60)).append("\n");

        revenueMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(e -> {
                    String tag = e.getKey().equals(champion) ? "  ◆ TOP EARNER" : "";
                    sb.append(String.format("  %-22s  £%,9.2f%s%n",
                            e.getKey(), e.getValue(), tag));
                });

        sb.append("  " + "─".repeat(56)).append("\n");
        sb.append(String.format("  Champion class type : %-18s  (£%,.2f)%n",
                champion, revenueMap.get(champion)));
        sb.append("═".repeat(60)).append("\n");
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  QUERY HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /** Active bookings for a given member (BOOKED or MODIFIED). */
    public List<Booking> getActiveBookingsForMember(String memberId) {
        return bookings.values().stream()
                .filter(b -> b.getMemberId().equals(memberId) && b.isActive())
                .collect(Collectors.toList());
    }

    /** All bookings (any status) for a given member. */
    public List<Booking> getAllBookingsForMember(String memberId) {
        return bookings.values().stream()
                .filter(b -> b.getMemberId().equals(memberId))
                .collect(Collectors.toList());
    }

    public Optional<Booking> findBooking(String bookingRef) {
        return Optional.ofNullable(bookings.get(bookingRef));
    }

    public Collection<Booking> getAllBookings() {
        return Collections.unmodifiableCollection(bookings.values());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /** Generates a unique booking reference in the format EFC-XXXX. */
    private String generateRef() {
        return String.format("EFC-%04d", refCounter++);
    }

    private Member requireMember(String memberId) {
        return lookupMember(memberId).orElseThrow(
            () -> new IllegalArgumentException(
                "Error: Member ID '" + memberId + "' not found in the system."));
    }

    private ClassSession requireSession(String sessionId) {
        return schedule.findById(sessionId).orElseThrow(
            () -> new IllegalArgumentException(
                "Error: Session ID '" + sessionId + "' not found in the schedule."));
    }

    private Booking requireActiveBooking(String bookingRef) {
        Booking b = bookings.get(bookingRef);
        if (b == null) {
            throw new IllegalArgumentException(
                "Error: Booking reference '" + bookingRef + "' does not exist.");
        }
        if (!b.isActive()) {
            throw new IllegalStateException(
                "Error: Booking " + bookingRef
                + " cannot be modified — current status is " + b.getStatus() + ".");
        }
        return b;
    }

    /**
     * Checks whether a member already has an active booking at the same
     * week/day/time-slot as {@code target}.
     *
     * @param excludeRef if non-null, that booking is skipped (used during modify)
     */
    private void detectConflict(String memberId, ClassSession target,
                                String excludeRef) {
        bookings.values().stream()
                .filter(b -> b.getMemberId().equals(memberId))
                .filter(b -> b.isActive())
                .filter(b -> !b.getBookingRef().equals(excludeRef))
                .forEach(b -> {
                    ClassSession existing =
                            schedule.findById(b.getSessionId()).orElse(null);
                    if (existing != null
                            && existing.getWeekNumber() == target.getWeekNumber()
                            && existing.getDay()        == target.getDay()
                            && existing.getTimeSlot()   == target.getTimeSlot()) {
                        throw new IllegalStateException(
                            "Error: Schedule conflict — " + memberId
                            + " already has a class booked at "
                            + target.getDay() + " "
                            + target.getTimeSlot().getLabel()
                            + " in Week " + target.getWeekNumber() + ".");
                    }
                });
    }
}
