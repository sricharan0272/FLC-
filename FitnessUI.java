package efc.ui;

import efc.model.*;
import efc.service.BookingManager;

import java.util.*;

/**
 * Menu-driven command-line interface for the Elite Fitness Club Booking System.
 *
 * Responsibilities:
 *   - Render menus and read validated user input
 *   - Delegate ALL business logic to {@link BookingManager}
 *   - Display clear success / error messages in EFC branding style
 */
public class FitnessUI {

    private final BookingManager bm;
    private final Scanner        input = new Scanner(System.in);

    public FitnessUI(BookingManager bm) {
        this.bm = bm;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ══════════════════════════════════════════════════════════════════════════

    public void launch() {
        printWelcomeBanner();
        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt("  Select an option: ", 0, 9);
            switch (choice) {
                case 1 -> viewSchedule();
                case 2 -> placeBooking();
                case 3 -> modifyBooking();
                case 4 -> cancelBooking();
                case 5 -> attendClass();
                case 6 -> viewMyBookings();
                case 7 -> viewAllMembers();
                case 8 -> System.out.println(bm.buildAttendanceReport());
                case 9 -> System.out.println(bm.buildRevenueReport());
                case 0 -> running = false;
            }
        }
        System.out.println("\n  ╔══════════════════════════════════════════════╗");
        System.out.println("  ║  Thank you for using Elite Fitness Club!     ║");
        System.out.println("  ║  Stay fit. Stay strong. See you next weekend.║");
        System.out.println("  ╚══════════════════════════════════════════════╝\n");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MENU HANDLERS
    // ══════════════════════════════════════════════════════════════════════════

    // ── 1. View Schedule ─────────────────────────────────────────────────────

    private void viewSchedule() {
        printHeader("VIEW CLASS SCHEDULE");
        System.out.println("  1. Browse by day (Saturday / Sunday)");
        System.out.println("  2. Browse by fitness type");
        int choice = readInt("  Choose: ", 1, 2);

        if (choice == 1) {
            Day day = pickDay();
            printSessionTable(bm.viewByDay(day), "Schedule for " + day);
        } else {
            String type = pickFitnessType();
            printSessionTable(bm.viewByFitnessType(type), "All '" + type + "' classes");
        }
    }

    // ── 2. Place Booking ─────────────────────────────────────────────────────

    private void placeBooking() {
        printHeader("BOOK A CLASS");
        String memberId = promptMemberId();
        showAvailableSessions();
        String sessionId = prompt("  Enter Session ID to book: ").toUpperCase();

        try {
            Booking b = bm.placeBooking(memberId, sessionId);
            printSuccess("Booking successful!  Reference: " + b.getBookingRef());
        } catch (Exception e) {
            printError(e.getMessage());
        }
    }

    // ── 3. Modify Booking ────────────────────────────────────────────────────

    private void modifyBooking() {
        printHeader("MODIFY A BOOKING");
        String memberId = promptMemberId();
        List<Booking> active = bm.getActiveBookingsForMember(memberId);

        if (active.isEmpty()) {
            printError("No active bookings found for member " + memberId + ".");
            return;
        }

        printBookingTable(active);
        String ref = prompt("  Enter Booking Reference to modify: ").toUpperCase();

        showAvailableSessions();
        String newId = prompt("  Enter NEW Session ID: ").toUpperCase();

        try {
            Booking b = bm.modifyBooking(ref, newId);
            printSuccess("Booking " + b.getBookingRef()
                + " successfully moved to session " + newId + ".");
        } catch (Exception e) {
            printError(e.getMessage());
        }
    }

    // ── 4. Cancel Booking ────────────────────────────────────────────────────

    private void cancelBooking() {
        printHeader("CANCEL A BOOKING");
        String memberId = promptMemberId();
        List<Booking> active = bm.getActiveBookingsForMember(memberId);

        if (active.isEmpty()) {
            printError("No active bookings found for member " + memberId + ".");
            return;
        }

        printBookingTable(active);
        String ref = prompt("  Enter Booking Reference to cancel: ").toUpperCase();

        try {
            bm.cancelBooking(ref);
            printSuccess("Booking " + ref + " has been cancelled. Spot released.");
        } catch (Exception e) {
            printError(e.getMessage());
        }
    }

    // ── 5. Attend Class + Feedback ───────────────────────────────────────────

    private void attendClass() {
        printHeader("RECORD ATTENDANCE & SUBMIT FEEDBACK");
        String memberId = promptMemberId();
        List<Booking> active = bm.getActiveBookingsForMember(memberId);

        if (active.isEmpty()) {
            printError("No active bookings found for member " + memberId + ".");
            return;
        }

        printBookingTable(active);
        String ref     = prompt("  Enter Booking Reference to mark attended: ").toUpperCase();
        int    rating  = readInt("  Rate this class (1 = Very Poor … 5 = Excellent): ", 1, 5);
        String comment = prompt("  Leave a comment about the class: ");

        try {
            bm.recordAttendance(ref, rating, comment);
            printSuccess("Attendance recorded. Thank you for your feedback!");
        } catch (Exception e) {
            printError(e.getMessage());
        }
    }

    // ── 6. View My Bookings ──────────────────────────────────────────────────

    private void viewMyBookings() {
        printHeader("MY BOOKING HISTORY");
        String memberId = promptMemberId();
        List<Booking> all = bm.getAllBookingsForMember(memberId);

        if (all.isEmpty()) {
            System.out.println("\n  No bookings on record for " + memberId + ".\n");
            return;
        }

        System.out.printf("%n  %-12s %-12s %-12s%n", "Reference", "Session ID", "Status");
        System.out.println("  " + "─".repeat(38));
        for (Booking b : all) {
            System.out.printf("  %-12s %-12s %-12s%n",
                    b.getBookingRef(), b.getSessionId(), b.getStatus());
        }
        System.out.println();
    }

    // ── 7. View All Members ──────────────────────────────────────────────────

    private void viewAllMembers() {
        printHeader("REGISTERED MEMBERS");
        bm.getAllMembers().forEach(m -> System.out.println("  " + m));
        System.out.println();
    }

    // ── 8 & 9 handled inline in launch() ─────────────────────────────────────

    // ══════════════════════════════════════════════════════════════════════════
    //  DISPLAY HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private void printWelcomeBanner() {
        System.out.println();
        System.out.println("  ╔═══════════════════════════════════════════════════════╗");
        System.out.println("  ║       ELITE FITNESS CLUB — BOOKING SYSTEM  v2.0      ║");
        System.out.println("  ║          Your Premium Weekend Fitness Partner         ║");
        System.out.println("  ╚═══════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private void printMainMenu() {
        System.out.println("  ┌────────────────────────────────────────────┐");
        System.out.println("  │              MAIN MENU                     │");
        System.out.println("  ├────────────────────────────────────────────┤");
        System.out.println("  │  1.  View Class Schedule                   │");
        System.out.println("  │  2.  Book a Class                          │");
        System.out.println("  │  3.  Modify a Booking                      │");
        System.out.println("  │  4.  Cancel a Booking                      │");
        System.out.println("  │  5.  Record Attendance & Submit Feedback   │");
        System.out.println("  │  6.  My Booking History                    │");
        System.out.println("  │  7.  View All Members                      │");
        System.out.println("  │  8.  Attendance & Ratings Report           │");
        System.out.println("  │  9.  Revenue Report                        │");
        System.out.println("  │  0.  Exit                                  │");
        System.out.println("  └────────────────────────────────────────────┘");
    }

    private void printHeader(String title) {
        System.out.println("\n  ── " + title + " " + "─".repeat(Math.max(0, 48 - title.length())));
        System.out.println();
    }

    private void printSuccess(String msg) {
        System.out.println("\n  ✔  " + msg + "\n");
    }

    private void printError(String msg) {
        System.out.println("\n  ✘  " + msg + "\n");
    }

    private void printSessionTable(List<ClassSession> sessions, String heading) {
        System.out.println("\n  " + heading);
        System.out.printf("%n  %-12s %-20s %-10s %-10s %-6s %-8s %-6s%n",
                "Session ID", "Fitness Type", "Day", "Slot", "Week", "Fee", "Spots");
        System.out.println("  " + "─".repeat(74));
        for (ClassSession s : sessions) {
            System.out.printf("  %-12s %-20s %-10s %-10s %-6d £%-7.2f %d/%d%n",
                    s.getSessionId(), s.getFitnessType(),
                    s.getDay().getLabel(), s.getTimeSlot().getLabel(),
                    s.getWeekNumber(), s.getFee(),
                    s.getEnrolledIds().size(), ClassSession.MAX_SPOTS);
        }
        System.out.println();
    }

    private void showAvailableSessions() {
        List<ClassSession> available = new ArrayList<>();
        bm.getSchedule().getAllSessions().forEach(s -> {
            if (s.hasAvailability()) available.add(s);
        });
        System.out.println("\n  Sessions with available spots:");
        System.out.printf("  %-12s %-20s %-10s %-10s %-6s %-8s %-6s%n",
                "Session ID", "Fitness Type", "Day", "Slot", "Week", "Fee", "Spots");
        System.out.println("  " + "─".repeat(74));
        available.forEach(s ->
            System.out.printf("  %-12s %-20s %-10s %-10s %-6d £%-7.2f %d/%d%n",
                    s.getSessionId(), s.getFitnessType(),
                    s.getDay().getLabel(), s.getTimeSlot().getLabel(),
                    s.getWeekNumber(), s.getFee(),
                    s.getEnrolledIds().size(), ClassSession.MAX_SPOTS));
        System.out.println();
    }

    private void printBookingTable(List<Booking> bookings) {
        System.out.printf("%n  %-12s %-12s %-12s%n", "Reference", "Session ID", "Status");
        System.out.println("  " + "─".repeat(38));
        bookings.forEach(b ->
            System.out.printf("  %-12s %-12s %-12s%n",
                    b.getBookingRef(), b.getSessionId(), b.getStatus()));
        System.out.println();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INPUT HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private Day pickDay() {
        System.out.println("  1. Saturday");
        System.out.println("  2. Sunday");
        return readInt("  Choose day: ", 1, 2) == 1 ? Day.SATURDAY : Day.SUNDAY;
    }

    private String pickFitnessType() {
        List<String> types = bm.getAllFitnessTypes();
        System.out.println("  Available fitness types:");
        for (int i = 0; i < types.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, types.get(i));
        }
        int idx = readInt("  Choose type: ", 1, types.size());
        return types.get(idx - 1);
    }

    private String promptMemberId() {
        System.out.println("  Members: EFC-M01 to EFC-M10");
        return prompt("  Enter Member ID: ").toUpperCase();
    }

    /** Reads a trimmed non-empty line from stdin. */
    private String prompt(String label) {
        System.out.print(label);
        String line = input.nextLine().trim();
        while (line.isEmpty()) {
            System.out.print("  (cannot be empty) " + label);
            line = input.nextLine().trim();
        }
        return line;
    }

    /** Reads an integer in [min, max], re-prompting on invalid input. */
    private int readInt(String label, int min, int max) {
        while (true) {
            System.out.print(label);
            String line = input.nextLine().trim();
            try {
                int val = Integer.parseInt(line);
                if (val >= min && val <= max) return val;
                System.out.println("  Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input — please enter a whole number.");
            }
        }
    }
}
