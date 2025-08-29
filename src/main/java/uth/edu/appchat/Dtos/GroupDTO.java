package uth.edu.appchat.Dtos;

public class GroupDTO {
    private Long id;
    private String name;
    private int memberCount;
    private Integer streakCount;
    private java.time.LocalDate streakLastDate;

    public GroupDTO(Long id, String name, int memberCount) {
        this.id = id;
        this.name = name;
        this.memberCount = memberCount;
        this.streakCount = 0;
    }

    public GroupDTO(Long id, String name, int memberCount, Integer streakCount) {
        this.id = id;
        this.name = name;
        this.memberCount = memberCount;
        this.streakCount = streakCount == null ? 0 : streakCount;
    this.streakLastDate = null;
    }

    // Getters và setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
    public Integer getStreakCount() { return streakCount == null ? 0 : streakCount; }
    public void setStreakCount(Integer streakCount) { this.streakCount = streakCount; }
    public java.time.LocalDate getStreakLastDate() { return streakLastDate; }
    public void setStreakLastDate(java.time.LocalDate streakLastDate) { this.streakLastDate = streakLastDate; }
}