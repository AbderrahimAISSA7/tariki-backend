package com.tariki.backend.controller;

import com.tariki.backend.dto.DeliveryWorkflowDTO.*;
import com.tariki.backend.dto.LivraisonDTO;
import com.tariki.backend.dto.auth.AuthResponse;
import com.tariki.backend.service.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DeliveryWorkflowController {
    private final DeliveryInvitationService invitations;
    private final DeliveryCompletionService completion;
    public DeliveryWorkflowController(DeliveryInvitationService invitations, DeliveryCompletionService completion) {
        this.invitations=invitations; this.completion=completion;
    }
    @GetMapping("/livraisons/{id}/invitation")
    public ResponseEntity<Invitation> invitation(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invitations.invitation(id,false));
    }
    @PostMapping("/livraisons/{id}/invitation")
    public ResponseEntity<Invitation> renew(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invitations.invitation(id,true));
    }
    @GetMapping("/invitations/{token}")
    public ResponseEntity<InvitePreview> preview(@PathVariable String token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Referrer-Policy","no-referrer").body(invitations.preview(token));
    }
    @PostMapping("/invitations/{token}/register")
    public ResponseEntity<AuthResponse> activate(@PathVariable String token, @Valid @RequestBody Activation request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invitations.activate(token,request));
    }
    @PatchMapping("/livraisons/{id}/tarif")
    public LivraisonDTO pricing(@PathVariable Long id, @Valid @RequestBody Pricing request) { return completion.pricing(id,request); }
    @PostMapping("/livraisons/{id}/reception")
    public LivraisonDTO receive(@PathVariable Long id, @Valid @RequestBody Receipt request) { return completion.receive(id,request); }
    @GetMapping("/livraisons/{id}/documents")
    public Documents documents(@PathVariable Long id) { return completion.documents(id); }
}
