package com.tariki.backend.service;

import com.tariki.backend.model.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;
import java.awt.Color;
import java.io.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class InvoicePdf {
    public byte[] generate(Facture invoice, Signature signature) {
        try (var doc = new PDDocument(); var buffer = new ByteArrayOutputStream();
             var fontStream = getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf")) {
            if (fontStream == null) throw new IOException("Police PDF absente");
            var font = PDType0Font.load(doc, fontStream);
            Livraison d = invoice.getLivraison();
            Entreprise e = d.getEntreprise();
            Client c = d.getClient();
            try (Layout page = new Layout(doc, font)) {
                if (e.getLogoPng() != null) page.image(e.getLogoPng(), 120, 55);
                page.text(e.getNom(), 20, new Color(22, 93, 75));
                page.text("FACTURE " + invoice.getNumero(), 16, Color.BLACK);
                page.text("Emise le " + date(invoice.getEmiseAt()) + " | Devise : MAD", 10, Color.DARK_GRAY);
                page.gap(12);
                page.text("ENTREPRISE", 11, Color.DARK_GRAY);
                page.text(e.getAdresse(), 11, Color.BLACK);
                page.text(e.getEmail() + " | " + e.getTelephone(), 10, Color.BLACK);
                if (e.getIce() != null && !e.getIce().isBlank()) page.text("ICE : " + e.getIce(), 10, Color.BLACK);
                if (e.getIdentifiantFiscal() != null && !e.getIdentifiantFiscal().isBlank()) page.text("IF : " + e.getIdentifiantFiscal(), 10, Color.BLACK);
                if (e.getRegistreCommerce() != null && !e.getRegistreCommerce().isBlank()) page.text("RC : " + e.getRegistreCommerce(), 10, Color.BLACK);
                page.gap(12);
                page.text("CLIENT", 11, Color.DARK_GRAY);
                page.text(c.getPrenom() + " " + c.getNom(), 12, Color.BLACK);
                page.text(c.getAdresse(), 11, Color.BLACK);
                page.text(c.getEmail() + " | " + c.getTelephone(), 10, Color.BLACK);
                page.gap(12);
                page.text("LIVRAISON " + d.getReference(), 12, Color.BLACK);
                page.text(d.getVilleDepart() + " > " + d.getVilleArrivee(), 12, Color.BLACK);
                page.text(d.getPoidsTonnes() + " tonnes - " + d.getMarchandise(), 11, Color.BLACK);
                page.text("Chauffeur : " + d.getChauffeur().getPrenom() + " " + d.getChauffeur().getNom()
                        + " | Camion : " + d.getCamion().getImmatriculation(), 10, Color.BLACK);
                page.text("Arrivee : " + date(d.getArriveeAt()) + " | Reception : " + date(d.getValidationAt()), 10, Color.BLACK);
                page.gap(14);
                page.text(d.getServiceFacture(), 12, Color.BLACK);
                page.text("Prix HT : " + invoice.getMontantHT().toPlainString() + " MAD", 12, Color.BLACK);
                page.text("TVA (" + invoice.getTauxTVA().stripTrailingZeros().toPlainString() + " %) : " + invoice.getMontantTVA().toPlainString() + " MAD", 12, Color.BLACK);
                page.text("TOTAL TTC : " + invoice.getMontantTTC().toPlainString() + " MAD", 16, new Color(22, 93, 75));
                page.gap(12);
                page.text("Reception validee par " + signature.getSignataire(), 11, Color.BLACK);
                page.text("Compte client n. " + signature.getUtilisateurId() + " | " + date(signature.getValidationAt()), 9, Color.DARK_GRAY);
                page.image(signature.getImagePng(), 200, 65);
                page.text("Je confirme la reception de cette livraison et signe le bon de reception.", 9, Color.DARK_GRAY);
                page.text("Signature manuscrite recueillie dans l'espace client authentifie. Cette facture ne vaut pas acquittement.", 9, Color.DARK_GRAY);
            }
            doc.getDocumentInformation().setTitle("Facture " + invoice.getNumero());
            doc.getDocumentInformation().setAuthor(e.getNom());
            doc.save(buffer);
            return buffer.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Generation de la facture impossible", exception);
        }
    }

    private static String date(java.time.Instant value) {
        return value == null ? "Non renseignee" : DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm z", Locale.FRANCE)
                .withZone(ZoneId.of("Africa/Casablanca")).format(value);
    }

    private static final class Layout implements AutoCloseable {
        private final PDDocument doc;
        private final PDType0Font font;
        private PDPageContentStream stream;
        private float y;
        Layout(PDDocument doc, PDType0Font font) throws IOException { this.doc=doc; this.font=font; next(); }
        private void next() throws IOException {
            if (stream != null) stream.close();
            PDPage page = new PDPage(PDRectangle.A4); doc.addPage(page);
            stream = new PDPageContentStream(doc, page); y=790;
        }
        private void room(float height) throws IOException { if (y-height < 48) next(); }
        void gap(float height) throws IOException { room(height); y-=height; }
        void text(String value, float size, Color color) throws IOException {
            if (value == null || value.isBlank()) return;
            // Wrap by actual font metrics, including long unbroken identifiers.
            StringBuilder line = new StringBuilder();
            for (int cp : value.replaceAll("[\\p{Cntrl}]", " ").codePoints().toArray()) {
                String character = new String(Character.toChars(cp));
                try { font.encode(character); } catch (IllegalArgumentException unsupported) {
                    throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                            "Un caractere de facturation n'est pas pris en charge par la police PDF. Utilisez la transcription latine.");
                }
                if (font.getStringWidth(line + character)*size/1000 > 490 && !line.isEmpty()) {
                    line(line.toString(), size, color); line.setLength(0);
                }
                line.append(character);
            }
            if (!line.isEmpty()) line(line.toString(), size, color);
        }
        private void line(String value, float size, Color color) throws IOException {
            room(size+7); stream.beginText(); stream.setFont(font,size); stream.setNonStrokingColor(color);
            stream.newLineAtOffset(50,y); stream.showText(value); stream.endText(); y-=size+7;
        }
        void image(byte[] bytes, float maxWidth, float maxHeight) throws IOException {
            var image = PDImageXObject.createFromByteArray(doc,bytes,"image");
            float ratio = Math.min(maxWidth/image.getWidth(),maxHeight/image.getHeight());
            float width=image.getWidth()*ratio, height=image.getHeight()*ratio;
            room(height+10); stream.drawImage(image,50,y-height,width,height); y-=height+10;
        }
        public void close() throws IOException { if (stream != null) stream.close(); }
    }
}
