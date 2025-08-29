
package uth.edu.appchat.Controllers;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uth.edu.appchat.Models.ChatAlias.ScopeType;
import uth.edu.appchat.Services.ChatAliasService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-alias")
public class ChatAliasController {

    private final ChatAliasService service;

    @GetMapping
    public ResponseEntity<?> getAlias(@RequestParam("type") String type,
                                      @RequestParam(value = "id", required = false) Long id) {
        ScopeType st = ScopeType.valueOf(type.toUpperCase());
        Long scopeId = (st == ScopeType.PUBLIC) ? 0L : id;
        String alias = service.getAlias(st, scopeId);
        return ResponseEntity.ok(new AliasDTO(alias));
    }

    @PostMapping
    public ResponseEntity<?> saveAlias(@RequestBody SaveAliasReq req) {
        ScopeType st = ScopeType.valueOf(req.type.toUpperCase());
        Long scopeId = (st == ScopeType.PUBLIC) ? 0L : req.id;
        String alias = service.upsertAlias(st, scopeId, req.alias);
        return ResponseEntity.ok(new AliasDTO(alias));
    }

    @Data
    public static class SaveAliasReq {
        private String type; // "group" | "private" | "public"
        private Long id;     // bắt buộc với group/private
        private String alias;
    }

    @Data
    public static class AliasDTO {
        private final String alias;
    }
}
