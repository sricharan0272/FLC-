package efc.model;

/**
 * A registered gym member of Elite Fitness Club.
 * Each member has a unique ID, full name, phone number, and email.
 */
public class Member {

    private final String memberId;
    private final String fullName;
    private final String phone;
    private final String email;

    public Member(String memberId, String fullName, String phone, String email) {
        this.memberId = memberId;
        this.fullName = fullName;
        this.phone    = phone;
        this.email    = email;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getMemberId() { return memberId; }
    public String getFullName() { return fullName; }
    public String getPhone()    { return phone;    }
    public String getEmail()    { return email;    }

    @Override
    public String toString() {
        return String.format("[%s] %-22s  %s", memberId, fullName, email);
    }
}
