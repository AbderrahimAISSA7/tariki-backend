package com.tariki.backend;

import com.fasterxml.jackson.databind.*;
import com.tariki.backend.model.*;
import com.tariki.backend.repository.*;
import jakarta.persistence.EntityManager;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:profiles;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
        "spring.jpa.show-sql=false", "logging.level.org.springframework.web=WARN"
})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
class DeliveryWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired LivraisonRepository deliveries;
    @Autowired FactureRepository invoices;
    @Autowired SignatureRepository signatures;
    @Autowired UserRepository users;
    @Autowired EntityManager entities;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;

    private String login(String account,String password) throws Exception {
        return "Bearer " + json.readTree(mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("email",account,"password",password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();
    }
    private String company() throws Exception { return login("entreprise.demo@tariki.ma","Entreprise123!"); }
    private String driver() throws Exception { return login("chauffeur.demo@tariki.ma","Chauffeur123!"); }
    private String client() throws Exception { return login("client.demo@tariki.ma","Client123!"); }
    private Livraison active() { return deliveries.findAll().stream().filter(d -> "OT-2026-001".equals(d.getReference())).findFirst().orElseThrow(); }
    private JsonNode read(String path,String token) throws Exception {
        return json.readTree(mvc.perform(get(path).header("Authorization",token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    private JsonNode create() throws Exception {
        Livraison assigned=active();
        var data=new HashMap<String,Object>();
        data.put("dateLivraison","2026-10-01"); data.put("villeDepart","Oujda"); data.put("villeArrivee","Figuig");
        data.put("marchandise","Ciment"); data.put("poidsTonnes",10); data.put("chauffeurId",assigned.getChauffeur().getId());
        data.put("camionId",assigned.getCamion().getId()); data.put("prixHT",1234.56); data.put("tauxTVA",20); data.put("serviceFacture","Transport de ciment");
        data.put("nouveauClient",Map.of("nom","Materiaux","prenom","Client","email","new.workflow@tariki.ma","telephone","0600000000","adresse","Figuig"));
        return json.readTree(mvc.perform(post("/api/livraisons").header("Authorization",company()).contentType("application/json").content(json.writeValueAsString(data)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    private String invitation(long id) throws Exception {
        return read("/api/livraisons/"+id+"/invitation",company()).get("path").asText().split("invitation=")[1];
    }
    private String activationBody() throws Exception {
        return json.writeValueAsString(Map.of("nom","Materiaux","prenom","Client","telephone","0600000000","adresse","Figuig","password","ClientFlow123!","role","ADMIN","email","attacker@example.com"));
    }
    private String image(boolean ink) throws Exception {
        BufferedImage bitmap=new BufferedImage(360,160,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=bitmap.createGraphics(); g.setColor(Color.WHITE); g.fillRect(0,0,360,160);
        if (ink) { g.setColor(Color.BLACK); g.setStroke(new BasicStroke(3)); g.drawPolyline(new int[]{30,60,90,110,160,210,270},new int[]{110,30,120,70,100,40,90},7); }
        g.dispose(); var out=new ByteArrayOutputStream(); ImageIO.write(bitmap,"png",out);
        return "data:image/png;base64,"+Base64.getEncoder().encodeToString(out.toByteArray());
    }
    private String receipt(Livraison d, boolean ink, boolean consent) throws Exception {
        return json.writeValueAsString(Map.of("signataire","Client Materiaux","signature",image(ink),"consentement",consent,"version",d.getVersion()));
    }
    private void ready(Livraison d) {
        d.setStatut("EN_ATTENTE_VALIDATION"); d.setArriveeAt(Instant.now()); d.setServiceFacture("Transport de ciment");
        d.setPrixHT(new BigDecimal("1234.56")); d.setTauxTVA(new BigDecimal("20.00")); deliveries.saveAndFlush(d);
    }

    @Test void invitationActivatesOnlyItsClientAndPreservesDeliveryBinding() throws Exception {
        JsonNode created=create(); long id=created.get("id").asLong(); String token=invitation(id);
        assertThat(created.get("reference").asText()).startsWith("TRK-");
        assertThat(created.get("clientInvitationPending").asBoolean()).isTrue();
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/livraisons/"+id).header("Authorization",client())).andExpect(status().isNotFound());
        mvc.perform(get("/api/livraisons/"+id+"/invitation").header("Authorization",driver())).andExpect(status().isForbidden());
        String result=mvc.perform(post("/api/invitations/"+token+"/register").contentType("application/json").content(activationBody()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.email").value("new.workflow@tariki.ma")).andReturn().getResponse().getContentAsString();
        String clientToken="Bearer "+json.readTree(result).get("accessToken").asText();
        assertThat(read("/api/livraisons",clientToken).size()).isEqualTo(1);
        assertThat(read("/api/livraisons/"+id,clientToken).get("clientId").asLong()).isEqualTo(created.get("clientId").asLong());
        mvc.perform(post("/api/invitations/"+token+"/register").contentType("application/json").content(activationBody())).andExpect(status().isConflict());
        mvc.perform(get("/api/invitations/"+token)).andExpect(status().isOk()).andExpect(jsonPath("$.email").isEmpty());
        assertThat(read("/api/livraisons/"+id+"/invitation",company()).get("path").asText()).isEqualTo("/livraisons/"+id);
    }

    @Test void expiredRotatedAndTamperedInvitationsCannotActivateAccounts() throws Exception {
        long id=create().get("id").asLong(); String old=invitation(id);
        mvc.perform(post("/api/livraisons/"+id+"/invitation").header("Authorization",company())).andExpect(status().isOk());
        mvc.perform(get("/api/invitations/"+old)).andExpect(status().isNotFound());
        String current=invitation(id);
        mvc.perform(get("/api/invitations/x"+current)).andExpect(status().isNotFound());
        Livraison d=deliveries.findById(id).orElseThrow(); d.setInvitationExpiresAt(Instant.now().minusSeconds(5)); deliveries.saveAndFlush(d);
        mvc.perform(post("/api/invitations/"+current+"/register").contentType("application/json").content(activationBody())).andExpect(status().isNotFound());
        assertThat(d.getClient().isInvitationPending()).isTrue();
    }

    @Test void driverCanOnlyRequestClientValidation() throws Exception {
        long id=active().getId();
        mvc.perform(patch("/api/livraisons/"+id+"/statut").header("Authorization",driver()).contentType("application/json").content("{\"statut\":\"LIVREE\"}")).andExpect(status().isConflict());
        mvc.perform(patch("/api/livraisons/"+id+"/statut").header("Authorization",company()).contentType("application/json").content("{\"statut\":\"EN_ATTENTE_VALIDATION\"}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/livraisons/"+id+"/statut").header("Authorization",driver()).contentType("application/json").content("{\"statut\":\"EN_ATTENTE_VALIDATION\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.arriveeAt").isNotEmpty());
        assertThat(invoices.findByLivraisonId(id)).isEmpty();
    }

    @Test void receiptRejectsWrongRolesBlankSignatureMissingConsentAndStalePricing() throws Exception {
        Livraison d=active(); ready(d); String path="/api/livraisons/"+d.getId()+"/reception";
        mvc.perform(post(path).header("Authorization",driver()).contentType("application/json").content(receipt(d,true,true))).andExpect(status().isForbidden());
        mvc.perform(post(path).header("Authorization",company()).contentType("application/json").content(receipt(d,true,true))).andExpect(status().isForbidden());
        mvc.perform(post(path).header("Authorization",client()).contentType("application/json").content(receipt(d,false,true))).andExpect(status().isBadRequest());
        mvc.perform(post(path).header("Authorization",client()).contentType("application/json").content(receipt(d,true,false))).andExpect(status().isBadRequest());
        String stale=receipt(d,true,true); d.setPrixHT(new BigDecimal("1400")); deliveries.saveAndFlush(d);
        mvc.perform(post(path).header("Authorization",client()).contentType("application/json").content(stale)).andExpect(status().isConflict());
        assertThat(invoices.findByLivraisonId(d.getId())).isEmpty();
    }

    @Test void validatedInvoiceIsArchivedScopedIdempotentAndImmutable() throws Exception {
        Livraison d=active(); ready(d); long id=d.getId(); String body=receipt(d,true,true); String client=client();
        d.getEntreprise().setLogoPng(Base64.getDecoder().decode(image(true).split(",")[1])); entities.flush();
        mvc.perform(post("/api/livraisons/"+id+"/reception").header("Authorization",client).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("LIVREE"));
        Facture invoice=invoices.findByLivraisonId(id).orElseThrow();
        assertThat(invoice.getMontantTVA()).isEqualByComparingTo("246.91"); assertThat(invoice.getMontantTTC()).isEqualByComparingTo("1481.47");
        byte[] bytes=mvc.perform(get("/api/factures/"+invoice.getId()+"/pdf").header("Authorization",client)).andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf")).andReturn().getResponse().getContentAsByteArray();
        try (var document=Loader.loadPDF(bytes)) {
            assertThat(new PDFTextStripper().getText(document)).contains("FACTURE", "Oriental Transport", "Figuig", "Transport de ciment", "1481.47", "Client Materiaux");
            int images=0;
            for (var page:document.getPages()) for (var name:page.getResources().getXObjectNames()) {
                if (page.getResources().isImageXObject(name)) images++;
            }
            assertThat(images).isEqualTo(2);
            var output=java.nio.file.Path.of("target","test-artifacts","invoice.png");
            java.nio.file.Files.createDirectories(output.getParent());
            ImageIO.write(new org.apache.pdfbox.rendering.PDFRenderer(document).renderImageWithDPI(0,100),"png",output.toFile());
        }
        mvc.perform(post("/api/livraisons/"+id+"/reception").header("Authorization",client).contentType("application/json").content(body)).andExpect(status().isOk());
        assertThat(invoices.count()).isEqualTo(1);
        assertThat(signatures.findAll().stream().filter(s -> "RECEPTION_CLIENT".equals(s.getType())).count()).isEqualTo(1);
        mvc.perform(delete("/api/factures/"+invoice.getId()).header("Authorization",company())).andExpect(status().isConflict());
        mvc.perform(put("/api/factures/"+invoice.getId()).header("Authorization",company()).contentType("application/json")
                .content(json.writeValueAsString(Map.of("livraisonId",id,"montantHT",1)))).andExpect(status().isConflict());
        Long signature=signatures.findFirstByLivraisonIdAndType(id,"RECEPTION_CLIENT").orElseThrow().getId();
        mvc.perform(delete("/api/signatures/"+signature).header("Authorization",company())).andExpect(status().isConflict());
        mvc.perform(get("/api/factures/"+invoice.getId()+"/pdf").header("Authorization",driver())).andExpect(status().isForbidden());
        User otherUser=users.findByUsernameIgnoreCase("negoce.demo@tariki.ma").orElseThrow();
        otherUser.setPassword(passwords.encode("OtherClient123!")); users.saveAndFlush(otherUser);
        String other=login("negoce.demo@tariki.ma","OtherClient123!");
        mvc.perform(get("/api/factures/"+invoice.getId()+"/pdf").header("Authorization",other)).andExpect(status().isNotFound());
        d.getEntreprise().setNom("Nouvelle raison sociale"); entities.flush();
        assertThat(mvc.perform(get("/api/factures/"+invoice.getId()+"/pdf").header("Authorization",client)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray()).isEqualTo(bytes);
    }

    @Test void companyOwnsPricingAndInvalidRatesAreRejected() throws Exception {
        Livraison d=active(); String path="/api/livraisons/"+d.getId()+"/tarif";
        var fields=new HashMap<String,Object>(Map.of("serviceFacture","Transport","prixHT",100,"tauxTVA",20,"version",d.getVersion()));
        mvc.perform(patch(path).header("Authorization",client()).contentType("application/json").content(json.writeValueAsString(fields))).andExpect(status().isForbidden());
        mvc.perform(patch(path).header("Authorization",driver()).contentType("application/json").content(json.writeValueAsString(fields))).andExpect(status().isForbidden());
        fields.put("tauxTVA",101);
        mvc.perform(patch(path).header("Authorization",company()).contentType("application/json").content(json.writeValueAsString(fields))).andExpect(status().isBadRequest());
        fields.put("tauxTVA",20); fields.put("prixHT",-1);
        mvc.perform(patch(path).header("Authorization",company()).contentType("application/json").content(json.writeValueAsString(fields))).andExpect(status().isBadRequest());
        fields.put("prixHT",100);
        mvc.perform(patch(path).header("Authorization",company()).contentType("application/json").content(json.writeValueAsString(fields)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.montantTTC").value(120));
        mvc.perform(post("/api/livraisons/"+d.getId()+"/reception").header("Authorization",client()).contentType("application/json").content(receipt(d,true,true))).andExpect(status().isConflict());
        ready(d); d.setPrixHT(null); deliveries.saveAndFlush(d);
        mvc.perform(post("/api/livraisons/"+d.getId()+"/reception").header("Authorization",client()).contentType("application/json").content(receipt(d,true,true))).andExpect(status().isBadRequest());
        assertThat(invoices.findByLivraisonId(d.getId())).isEmpty();
    }

    @Test void logoUpdatesRequireCompanyAndDecodeBoundedImages() throws Exception {
        Livraison d=active(); long id=d.getEntreprise().getId();
        var fields=new HashMap<String,Object>(Map.of("nom","Transport Test","adresse","Oujda","email","test@tariki.ma","telephone","0600000000","logo",image(true)));
        mvc.perform(patch("/api/entreprises/"+id+"/facturation").header("Authorization",client()).contentType("application/json").content(json.writeValueAsString(fields))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/entreprises/"+id+"/facturation").header("Authorization",company()).contentType("application/json").content(json.writeValueAsString(fields)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.logo").isNotEmpty());
        fields.put("logo","data:image/svg+xml;base64,PHN2Zy8+");
        mvc.perform(patch("/api/entreprises/"+id+"/facturation").header("Authorization",company()).contentType("application/json").content(json.writeValueAsString(fields))).andExpect(status().isBadRequest());
    }
}
