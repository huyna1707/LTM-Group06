package uth.edu.appchat.Dtos;

import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class CreateGroupForm {
    private String name;
    private String membersRaw; // A,B,C

    public List<String> getMembers() {
        return Arrays.stream(membersRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

}