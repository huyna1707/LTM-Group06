package uth.edu.appchat.Dtos;

public class GroupDTO {
    private Long id;
    private String name;
    private int memberCount;

    public GroupDTO(Long id, String name, int memberCount) {
        this.id = id;
        this.name = name;
        this.memberCount = memberCount;
    }

    // Getters và setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
}