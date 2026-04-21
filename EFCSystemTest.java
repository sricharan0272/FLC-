package efc;

import efc.data.DataSeeder;
import efc.model.*;
import efc.service.BookingManager;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite for the Elite Fitness Club Booking System.
 *
 * Coverage:
 *  - Booking placement (happy path)
 *  - Capacity enforcement (max 4 per session)
 *  - Duplicate booking prevention
 *  - Time-slot conflict detection
 *  - Booking modification
 *  - Booking cancellation
 *  - Attendance and feedback recording
 *  - Feedback rating validation (1–5)
 *  - Report generation (attendance + revenue)
 *  - Data seeder verification (10 members, 48 sessions, 22 reviews)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EFCSystemTest {

    private BookingManager bm;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        bm = new BookingManager();

        // 5 test members
        bm.registerMember(new Member("T01", "Aarav Test",   "+44 7000 000001", "aarav@t.com"));
        bm.registerMember(new Member("T02", "Priya Test",   "+44 7000 000002", "priya@t.com"));
        bm.registerMember(new Member("T03", "Rohan Test",   "+44 7000 000003", "rohan@t.com"));
        bm.registerMember(new Member("T04", "Ananya Test",  "+44 7000 000004", "ananya@t.com"));
        bm.registerMember(new Member("T05", "Vikram Test",  "+44 7000 000005", "vikram@t.com"));

        // Four sessions for testing — two at different slots on the same day
        bm.addSession(new ClassSession("S001", "Pilates",  Day.SATURDAY, TimeSlot.MORNING, 1, 14.00));
        bm.addSession(new ClassSession("S002", "HIIT",     Day.SATURDAY, TimeSlot.MIDDAY,  1, 12.00));
        bm.addSession(new ClassSession("S003", "CrossFit", Day.SATURDAY, TimeSlot.EVENING, 1, 13.50));
        bm.addSession(new ClassSession("S004", "Pilates",  Day.SATURDAY, TimeSlot.MORNING, 2, 14.00));
        bm.addSession(new ClassSession("S005", "Yoga Flow",Day.SUNDAY,   TimeSlot.MORNING, 1, 10.00));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BOOKING — HAPPY PATH
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(1)
    @DisplayName("Place booking returns a valid Booking with BOOKED status")
    void testPlaceBookingSuccess() {
        Booking b = bm.placeBooking("T01", "S001");

        assertNotNull(b);
        assertEquals("T01",  b.getMemberId());
        assertEquals("S001", b.getSessionId());
        assertEquals(BookingStatus.BOOKED, b.getStatus());
        assertTrue(b.isActive());
    }

    @Test @Order(2)
    @DisplayName("Booking references are unique across multiple bookings")
    void testBookingRefUniqueness() {
        Booking b1 = bm.placeBooking("T01", "S001");
        Booking b2 = bm.placeBooking("T02", "S001");
        assertNotEquals(b1.getBookingRef(), b2.getBookingRef());
    }

    @Test @Order(3)
    @DisplayName("Booking reference format matches EFC-XXXX pattern")
    void testBookingRefFormat() {
        Booking b = bm.placeBooking("T01", "S001");
        assertTrue(b.getBookingRef().matches("EFC-\\d{4}"),
                "Reference should match EFC-XXXX format");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CAPACITY ENFORCEMENT
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(4)
    @DisplayName("Session capacity enforced — 5th booking throws IllegalStateException")
    void testCapacityEnforcement() {
        bm.placeBooking("T01", "S001");
        bm.placeBooking("T02", "S001");
        bm.placeBooking("T03", "S001");
        bm.placeBooking("T04", "S001");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bm.placeBooking("T05", "S001"));

        assertTrue(ex.getMessage().contains("fully booked"),
                "Error message should state the session is fully booked");
    }

    @Test @Order(5)
    @DisplayName("Available spot count decrements correctly with each booking")
    void testFreeSpotDecrement() {
        ClassSession session = bm.getSchedule().findById("S001").orElseThrow();
        assertEquals(4, session.getFreeSpots());

        bm.placeBooking("T01", "S001");
        assertEquals(3, session.getFreeSpots());

        bm.placeBooking("T02", "S001");
        assertEquals(2, session.getFreeSpots());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DUPLICATE BOOKING PREVENTION
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(6)
    @DisplayName("Duplicate booking for same member and session is rejected")
    void testDuplicateBookingRejected() {
        bm.placeBooking("T01", "S001");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bm.placeBooking("T01", "S001"));

        assertTrue(ex.getMessage().toLowerCase().contains("already has an active booking"),
                "Error message should reference duplicate booking");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TIME-SLOT CONFLICT DETECTION
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(7)
    @DisplayName("Conflict detected when member books two sessions in same week/day/slot")
    void testTimeConflictDetected() {
        // Add a second session at the same slot as S001
        bm.addSession(new ClassSession("CONF", "Strength Training",
                Day.SATURDAY, TimeSlot.MORNING, 1, 11.00));
        bm.placeBooking("T01", "S001");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bm.placeBooking("T01", "CONF"));

        assertTrue(ex.getMessage().toLowerCase().contains("conflict"),
                "Error message should mention schedule conflict");
    }

    @Test @Order(8)
    @DisplayName("No conflict when sessions are on different time slots (same day)")
    void testNoConflictDifferentSlots() {
        bm.placeBooking("T01", "S001"); // Saturday Morning
        Booking b = bm.placeBooking("T01", "S002"); // Saturday Midday
        assertNotNull(b, "Different slot on same day should be allowed");
    }

    @Test @Order(9)
    @DisplayName("No conflict when sessions are in different weeks (same day/slot)")
    void testNoConflictDifferentWeeks() {
        bm.placeBooking("T01", "S001"); // Week 1 Sat Morning
        Booking b = bm.placeBooking("T01", "S004"); // Week 2 Sat Morning
        assertNotNull(b, "Same slot in a different week should be allowed");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MODIFY BOOKING
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(10)
    @DisplayName("Modify booking updates session and sets status to MODIFIED")
    void testModifyBookingSuccess() {
        Booking original = bm.placeBooking("T01", "S001");
        String  ref      = original.getBookingRef();

        bm.modifyBooking(ref, "S005"); // Yoga Flow Sunday

        Booking updated = bm.findBooking(ref).orElseThrow();
        assertEquals("S005",               updated.getSessionId());
        assertEquals(BookingStatus.MODIFIED, updated.getStatus());

        // Old session should have released the spot
        ClassSession oldSession = bm.getSchedule().findById("S001").orElseThrow();
        assertFalse(oldSession.isMemberEnrolled("T01"),
                "Member should be removed from the old session");

        // New session should have the member
        ClassSession newSession = bm.getSchedule().findById("S005").orElseThrow();
        assertTrue(newSession.isMemberEnrolled("T01"),
                "Member should be enrolled in the new session");
    }

    @Test @Order(11)
    @DisplayName("Modify to a fully booked session throws IllegalStateException")
    void testModifyToFullSession() {
        bm.placeBooking("T01", "S002");
        bm.placeBooking("T02", "S002");
        bm.placeBooking("T03", "S002");
        bm.placeBooking("T04", "S002"); // S002 is now full

        Booking b = bm.placeBooking("T05", "S001");
        assertThrows(IllegalStateException.class,
                () -> bm.modifyBooking(b.getBookingRef(), "S002"));
    }

    @Test @Order(12)
    @DisplayName("Modify to the same session throws IllegalArgumentException")
    void testModifyToSameSession() {
        Booking b = bm.placeBooking("T01", "S001");
        assertThrows(IllegalArgumentException.class,
                () -> bm.modifyBooking(b.getBookingRef(), "S001"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CANCEL BOOKING
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(13)
    @DisplayName("Cancel booking sets status to CANCELLED and releases spot")
    void testCancelBookingSuccess() {
        Booking b = bm.placeBooking("T01", "S001");
        bm.cancelBooking(b.getBookingRef());

        Booking cancelled = bm.findBooking(b.getBookingRef()).orElseThrow();
        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertFalse(cancelled.isActive());

        ClassSession session = bm.getSchedule().findById("S001").orElseThrow();
        assertFalse(session.isMemberEnrolled("T01"),
                "Spot should be released after cancellation");
    }

    @Test @Order(14)
    @DisplayName("Cancelling an already-cancelled booking throws IllegalStateException")
    void testCancelAlreadyCancelledThrows() {
        Booking b = bm.placeBooking("T01", "S001");
        bm.cancelBooking(b.getBookingRef());

        assertThrows(IllegalStateException.class,
                () -> bm.cancelBooking(b.getBookingRef()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ATTENDANCE & FEEDBACK
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(15)
    @DisplayName("Record attendance sets status to ATTENDED and stores feedback")
    void testRecordAttendance() {
        Booking b = bm.placeBooking("T01", "S001");
        bm.recordAttendance(b.getBookingRef(), 5, "Brilliant class!");

        Booking attended = bm.findBooking(b.getBookingRef()).orElseThrow();
        assertEquals(BookingStatus.ATTENDED, attended.getStatus());
        assertTrue(attended.isAttended());

        ClassSession session = bm.getSchedule().findById("S001").orElseThrow();
        assertEquals(1, session.getAttendanceCount());
        assertEquals(5.0, session.getAverageRating(), 0.001);
    }

    @Test @Order(16)
    @DisplayName("Average rating computed correctly from multiple feedback entries")
    void testAverageRatingCalculation() {
        Booking b1 = bm.placeBooking("T01", "S001");
        Booking b2 = bm.placeBooking("T02", "S001");
        Booking b3 = bm.placeBooking("T03", "S001");

        bm.recordAttendance(b1.getBookingRef(), 5, "Excellent");
        bm.recordAttendance(b2.getBookingRef(), 3, "Average");
        bm.recordAttendance(b3.getBookingRef(), 4, "Good");

        ClassSession session = bm.getSchedule().findById("S001").orElseThrow();
        assertEquals(4.0, session.getAverageRating(), 0.001, "(5+3+4)/3 = 4.0");
    }

    @Test @Order(17)
    @DisplayName("Rating of 0 is rejected with IllegalArgumentException")
    void testRatingZeroRejected() {
        Booking b = bm.placeBooking("T01", "S001");
        assertThrows(IllegalArgumentException.class,
                () -> bm.recordAttendance(b.getBookingRef(), 0, "Test"));
    }

    @Test @Order(18)
    @DisplayName("Rating of 6 is rejected with IllegalArgumentException")
    void testRatingSixRejected() {
        Booking b = bm.placeBooking("T01", "S001");
        assertThrows(IllegalArgumentException.class,
                () -> bm.recordAttendance(b.getBookingRef(), 6, "Test"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  REPORTS
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(19)
    @DisplayName("Revenue report identifies the top earner as champion")
    void testRevenueReportChampion() {
        Booking b = bm.placeBooking("T01", "S001"); // Pilates £14
        bm.recordAttendance(b.getBookingRef(), 4, "Great");

        String report = bm.buildRevenueReport();
        assertTrue(report.contains("TOP EARNER"),  "Report should flag the champion");
        assertTrue(report.contains("Pilates"),     "Pilates should appear in the report");
    }

    @Test @Order(20)
    @DisplayName("Attendance report contains session ID and fitness type for attended sessions")
    void testAttendanceReportContent() {
        Booking b = bm.placeBooking("T01", "S001");
        bm.recordAttendance(b.getBookingRef(), 5, "Outstanding");

        String report = bm.buildAttendanceReport();
        assertTrue(report.contains("S001"),    "Report should contain session ID");
        assertTrue(report.contains("Pilates"), "Report should contain fitness type name");
    }

    @Test @Order(21)
    @DisplayName("Non-attended bookings are excluded from the revenue report")
    void testNonAttendedExcludedFromRevenue() {
        bm.placeBooking("T01", "S001"); // booked but NOT attended

        String report = bm.buildRevenueReport();
        assertFalse(report.contains("Pilates"),
                "Pilates should not appear when no attendance has been recorded");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TIMETABLE QUERIES
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(22)
    @DisplayName("View by day returns only Saturday sessions")
    void testViewByDayFiltersSaturday() {
        List<ClassSession> saturdaySessions = bm.viewByDay(Day.SATURDAY);
        assertFalse(saturdaySessions.isEmpty());
        saturdaySessions.forEach(s ->
                assertEquals(Day.SATURDAY, s.getDay()));
    }

    @Test @Order(23)
    @DisplayName("View by fitness type returns only Pilates sessions")
    void testViewByFitnessTypeFiltersPilates() {
        List<ClassSession> pilatesSessions = bm.viewByFitnessType("Pilates");
        assertFalse(pilatesSessions.isEmpty());
        pilatesSessions.forEach(s ->
                assertEquals("Pilates", s.getFitnessType()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DATA SEEDER VERIFICATION
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(24)
    @DisplayName("DataSeeder loads at least 10 members, 48 sessions, and 22 feedback entries")
    void testDataSeederMeetsRequirements() {
        BookingManager seeded = new BookingManager();
        DataSeeder.seed(seeded);

        // Members
        assertTrue(seeded.getAllMembers().size() >= 10,
                "At least 10 members required");

        // Sessions (8 weekends × 6 = 48)
        long sessionCount = seeded.getSchedule().getAllSessions().size();
        assertEquals(48, sessionCount,
                "Exactly 48 sessions required (8 weekends × 6)");

        // Feedback entries
        long feedbackCount = seeded.getSchedule().getAllSessions().stream()
                .mapToLong(s -> s.getFeedbackList().size())
                .sum();
        assertTrue(feedbackCount >= 22,
                "At least 22 feedback entries required; found: " + feedbackCount);
    }
}
