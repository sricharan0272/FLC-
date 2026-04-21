package efc.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Holds and queries all {@link ClassSession} objects across all weekends.
 *
 * Acts as an in-memory repository with multi-key lookup capability.
 * Indexed primarily by sessionId; filtered views are derived on demand.
 */
public class Schedule {

    /** Primary store: sessionId → ClassSession */
    private final Map<String, ClassSession> sessions = new LinkedHashMap<>();

    // ── Mutation ──────────────────────────────────────────────────────────────

    public void addSession(ClassSession session) {
        sessions.put(session.getSessionId(), session);
    }

    // ── Lookups ───────────────────────────────────────────────────────────────

    public Optional<ClassSession> findById(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    /**
     * All sessions on a specific day, ordered by week number then time slot.
     */
    public List<ClassSession> getSessionsByDay(Day day) {
        return sessions.values().stream()
                .filter(s -> s.getDay() == day)
                .sorted(Comparator.comparingInt(ClassSession::getWeekNumber)
                        .thenComparing(ClassSession::getTimeSlot))
                .collect(Collectors.toList());
    }

    /**
     * All sessions of a given fitness type (case-insensitive).
     */
    public List<ClassSession> getSessionsByType(String fitnessType) {
        return sessions.values().stream()
                .filter(s -> s.getFitnessType().equalsIgnoreCase(fitnessType))
                .sorted(Comparator.comparingInt(ClassSession::getWeekNumber)
                        .thenComparing(ClassSession::getDay)
                        .thenComparing(ClassSession::getTimeSlot))
                .collect(Collectors.toList());
    }

    /** Returns a sorted list of all distinct fitness type names. */
    public List<String> getAllFitnessTypes() {
        return sessions.values().stream()
                .map(ClassSession::getFitnessType)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public Collection<ClassSession> getAllSessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }

    /**
     * Finds a session by week, day, and time slot — used for conflict detection.
     */
    public Optional<ClassSession> findBySlot(int weekNumber, Day day, TimeSlot timeSlot) {
        return sessions.values().stream()
                .filter(s -> s.getWeekNumber() == weekNumber
                          && s.getDay()         == day
                          && s.getTimeSlot()    == timeSlot)
                .findFirst();
    }
}
