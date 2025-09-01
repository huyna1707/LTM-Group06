package uth.edu.appchat.Dtos;

public class GroupDTO {
    private Long id;
    private String name;
    private int memberCount;
    private String nickname;       // biệt danh nhóm (nếu có)
    private String effectiveTitle; // nickname nếu có, else name
    private String avatarUrl; // 👈 THÊM

    public GroupDTO(Long id, String name, int memberCount, String avatarUrl, String nickname) {
        this(id, name, memberCount, avatarUrl, nickname,
                (nickname != null && !nickname.isBlank()) ? nickname : name);
    }
    public GroupDTO(Long id, String name, int memberCount, String avatarUrl,String nickname, String effectiveTitle) {
        this.id = id;
        this.name = name;
        this.memberCount = memberCount;
        this.avatarUrl = avatarUrl;
        this.nickname = nickname;
        this.effectiveTitle = effectiveTitle;
    }



    // Getters và setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getEffectiveTitle() { return effectiveTitle; }
    public void setEffectiveTitle(String effectiveTitle) { this.effectiveTitle = effectiveTitle; }
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}