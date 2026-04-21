package efc.data;

import efc.model.*;
import efc.service.BookingManager;

/**
 * Seeds the Elite Fitness Club system with realistic sample data:
 *
 *   Members   : 10  (IDs: EFC-M01 – EFC-M10, Indian names)
 *   Sessions  : 48  (8 weekends × 6 sessions each)
 *   Feedback  : 22+ (pre-attended bookings with ratings and comments)
 *
 * Fitness class fees (fixed per type):
 *   Pilates          £14.00
 *   HIIT             £12.00
 *   CrossFit         £13.50
 *   Strength Training£11.00
 *   Yoga Flow        £10.00
 */
public class DataSeeder {

    // Fixed fees per fitness type
    private static final double PILATES_FEE   = 14.00;
    private static final double HIIT_FEE      = 12.00;
    private static final double CROSSFIT_FEE  = 13.50;
    private static final double STRENGTH_FEE  = 11.00;
    private static final double YOGAFLOW_FEE  = 10.00;

    public static void seed(BookingManager bm) {
        loadMembers(bm);
        loadSchedule(bm);
        loadAttendanceAndFeedback(bm);
    }

    // ── Members ───────────────────────────────────────────────────────────────

    private static void loadMembers(BookingManager bm) {
        bm.registerMember(new Member("EFC-M01", "Aarav Sharma",       "+44 7700 100001", "aarav.sharma@gym.co.uk"));
        bm.registerMember(new Member("EFC-M02", "Priya Patel",        "+44 7700 100002", "priya.patel@gym.co.uk"));
        bm.registerMember(new Member("EFC-M03", "Rohan Mehta",        "+44 7700 100003", "rohan.mehta@gym.co.uk"));
        bm.registerMember(new Member("EFC-M04", "Ananya Iyer",        "+44 7700 100004", "ananya.iyer@gym.co.uk"));
        bm.registerMember(new Member("EFC-M05", "Vikram Nair",        "+44 7700 100005", "vikram.nair@gym.co.uk"));
        bm.registerMember(new Member("EFC-M06", "Deepika Reddy",      "+44 7700 100006", "deepika.reddy@gym.co.uk"));
        bm.registerMember(new Member("EFC-M07", "Arjun Kapoor",       "+44 7700 100007", "arjun.kapoor@gym.co.uk"));
        bm.registerMember(new Member("EFC-M08", "Sneha Krishnamurthy","+44 7700 100008", "sneha.krish@gym.co.uk"));
        bm.registerMember(new Member("EFC-M09", "Karan Malhotra",     "+44 7700 100009", "karan.malhotra@gym.co.uk"));
        bm.registerMember(new Member("EFC-M10", "Meera Joshi",        "+44 7700 100010", "meera.joshi@gym.co.uk"));
    }

    // ── Schedule (8 weekends × 6 sessions = 48 total) ─────────────────────────
    //
    // Saturday layout:
    //   Morning → Pilates
    //   Midday  → HIIT
    //   Evening → CrossFit
    // Sunday layout:
    //   Morning → Strength Training
    //   Midday  → Yoga Flow
    //   Evening → Pilates  (second weekly instance)

    private static void loadSchedule(BookingManager bm) {
        for (int w = 1; w <= 8; w++) {
            String wp = "W" + w;
            bm.addSession(new ClassSession(wp + "SAM", "Pilates",           Day.SATURDAY, TimeSlot.MORNING, w, PILATES_FEE));
            bm.addSession(new ClassSession(wp + "SAD", "HIIT",              Day.SATURDAY, TimeSlot.MIDDAY,  w, HIIT_FEE));
            bm.addSession(new ClassSession(wp + "SAE", "CrossFit",          Day.SATURDAY, TimeSlot.EVENING, w, CROSSFIT_FEE));
            bm.addSession(new ClassSession(wp + "SUM", "Strength Training", Day.SUNDAY,   TimeSlot.MORNING, w, STRENGTH_FEE));
            bm.addSession(new ClassSession(wp + "SUD", "Yoga Flow",         Day.SUNDAY,   TimeSlot.MIDDAY,  w, YOGAFLOW_FEE));
            bm.addSession(new ClassSession(wp + "SUE", "Pilates",           Day.SUNDAY,   TimeSlot.EVENING, w, PILATES_FEE));
        }
    }

    // ── Attendance & Feedback ─────────────────────────────────────────────────

    private static void loadAttendanceAndFeedback(BookingManager bm) {
        // Week 1 — Saturday Morning Pilates
        attend(bm, "EFC-M01", "W1SAM", 5, "Outstanding session — instructor was incredibly encouraging.");
        attend(bm, "EFC-M02", "W1SAM", 4, "Great core workout, will definitely return.");
        attend(bm, "EFC-M03", "W1SAM", 5, "Best Pilates class I have taken. Highly recommend.");

        // Week 1 — Saturday Midday HIIT
        attend(bm, "EFC-M04", "W1SAD", 4, "Intense and well-structured. Loved the circuit format.");
        attend(bm, "EFC-M05", "W1SAD", 3, "Good energy but slightly rushed at the end.");

        // Week 1 — Sunday Morning Strength Training
        attend(bm, "EFC-M06", "W1SUM", 5, "Excellent coaching on form and technique.");
        attend(bm, "EFC-M07", "W1SUM", 4, "Very effective. Felt results the next day!");

        // Week 2 — Saturday Evening CrossFit
        attend(bm, "EFC-M01", "W2SAE", 4, "Tough but fair. The coach kept everyone motivated.");
        attend(bm, "EFC-M08", "W2SAE", 5, "Absolutely loved every minute. Great community vibe.");
        attend(bm, "EFC-M09", "W2SAE", 3, "Challenging for beginners but manageable.");

        // Week 2 — Sunday Midday Yoga Flow
        attend(bm, "EFC-M10", "W2SUD", 5, "Perfectly paced — felt calm and energised afterwards.");
        attend(bm, "EFC-M02", "W2SUD", 4, "Beautiful session. Instructor very attentive.");

        // Week 3 — Saturday Morning Pilates
        attend(bm, "EFC-M03", "W3SAM", 5, "Consistent quality — this gym never disappoints.");
        attend(bm, "EFC-M05", "W3SAM", 4, "Really focused on breathing technique this time.");

        // Week 3 — Sunday Evening Pilates
        attend(bm, "EFC-M06", "W3SUE", 3, "Decent class but room felt a bit crowded.");
        attend(bm, "EFC-M07", "W3SUE", 4, "Good session overall. Instructor very helpful.");

        // Week 4 — Saturday Midday HIIT
        attend(bm, "EFC-M08", "W4SAD", 5, "Best HIIT class in the city, no question.");
        attend(bm, "EFC-M09", "W4SAD", 4, "Really pushed my limits. Excellent.");

        // Week 4 — Sunday Morning Strength Training
        attend(bm, "EFC-M10", "W4SUM", 5, "Progressive overload guidance was brilliant.");
        attend(bm, "EFC-M01", "W4SUM", 4, "Solid programming, great use of the 90 minutes.");

        // Week 5 — Saturday Evening CrossFit (extra feedback)
        attend(bm, "EFC-M04", "W5SAE", 5, "Pushed myself harder than I thought possible.");
        attend(bm, "EFC-M02", "W5SAE", 4, "Fun and challenging. Booked next week already.");

        // Active (non-attended) bookings — shown in menu 6 and changeable
        silentBook(bm, "EFC-M03", "W5SAD");   // HIIT Week 5
        silentBook(bm, "EFC-M05", "W5SUD");   // Yoga Flow Week 5
        silentBook(bm, "EFC-M06", "W6SAM");   // Pilates Week 6
        silentBook(bm, "EFC-M07", "W6SAE");   // CrossFit Week 6
        silentBook(bm, "EFC-M08", "W7SAM");   // Pilates Week 7
        silentBook(bm, "EFC-M09", "W7SUM");   // Strength Training Week 7
        silentBook(bm, "EFC-M10", "W8SAD");   // HIIT Week 8
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Books a session and immediately marks it attended with feedback. */
    private static void attend(BookingManager bm, String memberId,
                                String sessionId, int rating, String comment) {
        try {
            Booking b = bm.placeBooking(memberId, sessionId);
            bm.recordAttendance(b.getBookingRef(), rating, comment);
        } catch (Exception e) {
            System.err.println("[DataSeeder] Could not seed attendance for "
                + memberId + "/" + sessionId + ": " + e.getMessage());
        }
    }

    /** Creates an active booking without attending it. */
    private static void silentBook(BookingManager bm, String memberId, String sessionId) {
        try {
            bm.placeBooking(memberId, sessionId);
        } catch (Exception e) {
            System.err.println("[DataSeeder] Could not seed booking for "
                + memberId + "/" + sessionId + ": " + e.getMessage());
        }
    }
}
