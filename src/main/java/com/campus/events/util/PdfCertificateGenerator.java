package com.campus.events.util;

import com.campus.events.model.Certificate;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Component
public class PdfCertificateGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    public byte[] generateCertificate(Certificate cert) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // A4 in Landscape orientation for certificate layout
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // Outer border container table
            PdfPTable borderTable = new PdfPTable(1);
            borderTable.setWidthPercentage(100);

            PdfPCell borderCell = new PdfPCell();
            borderCell.setBorderColor(new Color(20, 50, 90));
            borderCell.setBorderWidth(4f);
            borderCell.setPadding(28f);
            borderCell.setBackgroundColor(new Color(253, 254, 255));

            // Fonts
            Font institutionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(70, 80, 95));
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, new Color(20, 50, 90));
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, new Color(100, 110, 125));
            Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(25, 30, 45));
            Font eventTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(15, 95, 175));
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 12, new Color(50, 55, 65));
            Font codeFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 11, new Color(80, 90, 100));

            // Inner content table
            PdfPTable innerTable = new PdfPTable(1);
            innerTable.setWidthPercentage(100);

            // Institution Name
            Paragraph instPara = new Paragraph("CAMPUS EVENT MANAGEMENT SYSTEM", institutionFont);
            instPara.setAlignment(Element.ALIGN_CENTER);
            PdfPCell cellInst = new PdfPCell(instPara);
            cellInst.setBorder(Rectangle.NO_BORDER);
            cellInst.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellInst.setPaddingBottom(6f);
            innerTable.addCell(cellInst);

            // Certificate Title
            Paragraph titlePara = new Paragraph("CERTIFICATE OF PARTICIPATION", titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            PdfPCell cellTitle = new PdfPCell(titlePara);
            cellTitle.setBorder(Rectangle.NO_BORDER);
            cellTitle.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellTitle.setPaddingBottom(12f);
            innerTable.addCell(cellTitle);

            // Decorative separator line
            Paragraph sepPara = new Paragraph("◆ ◆ ◆", subtitleFont);
            sepPara.setAlignment(Element.ALIGN_CENTER);
            PdfPCell cellSep = new PdfPCell(sepPara);
            cellSep.setBorder(Rectangle.NO_BORDER);
            cellSep.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellSep.setPaddingBottom(12f);
            innerTable.addCell(cellSep);

            // This is to certify that
            Paragraph certifyPara = new Paragraph("THIS IS PROUDLY PRESENTED TO", subtitleFont);
            certifyPara.setAlignment(Element.ALIGN_CENTER);
            PdfPCell cellCertify = new PdfPCell(certifyPara);
            cellCertify.setBorder(Rectangle.NO_BORDER);
            cellCertify.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellCertify.setPaddingBottom(10f);
            innerTable.addCell(cellCertify);

            // Student Name
            Paragraph namePara = new Paragraph(cert.getStudentName().toUpperCase(), nameFont);
            namePara.setAlignment(Element.ALIGN_CENTER);
            PdfPCell cellName = new PdfPCell(namePara);
            cellName.setBorder(Rectangle.NO_BORDER);
            cellName.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellName.setPaddingBottom(10f);
            innerTable.addCell(cellName);

            // Recognition statement
            String dept = cert.getStudent().getDepartment() != null ? cert.getStudent().getDepartment() : "General Student";
            Paragraph textPara = new Paragraph("from Department of " + dept + 
                    " for active participation and verified attendance in the campus event", bodyFont);
            textPara.setAlignment(Element.ALIGN_CENTER);
            PdfPCell cellText = new PdfPCell(textPara);
            cellText.setBorder(Rectangle.NO_BORDER);
            cellText.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellText.setPaddingBottom(10f);
            innerTable.addCell(cellText);

            // Event Title
            Paragraph eventPara = new Paragraph("“" + cert.getEventTitle() + "”", eventTitleFont);
            eventPara.setAlignment(Element.ALIGN_CENTER);
            PdfPCell cellEvent = new PdfPCell(eventPara);
            cellEvent.setBorder(Rectangle.NO_BORDER);
            cellEvent.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellEvent.setPaddingBottom(8f);
            innerTable.addCell(cellEvent);

            // Conducted On
            String dateFormatted = cert.getEventDate().format(DATE_FORMATTER);
            Paragraph datePara = new Paragraph("Conducted on " + dateFormatted + " | Venue: " + cert.getEvent().getVenue(), bodyFont);
            datePara.setAlignment(Element.ALIGN_CENTER);
            PdfPCell cellDate = new PdfPCell(datePara);
            cellDate.setBorder(Rectangle.NO_BORDER);
            cellDate.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellDate.setPaddingBottom(24f);
            innerTable.addCell(cellDate);

            // Footer with Signatures and Verification code
            PdfPTable footerTable = new PdfPTable(3);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{35f, 30f, 35f});

            // Left: Dean / Academic Head
            Paragraph sig1 = new Paragraph("__________________________\nDean of Academic Affairs\nCampus Administration", subtitleFont);
            sig1.setAlignment(Element.ALIGN_CENTER);
            PdfPCell cellSig1 = new PdfPCell(sig1);
            cellSig1.setBorder(Rectangle.NO_BORDER);
            cellSig1.setHorizontalAlignment(Element.ALIGN_CENTER);
            footerTable.addCell(cellSig1);

            // Center: Verification Code Badge
            Paragraph codePara = new Paragraph("VERIFIED CREDENTIAL\n" + cert.getCertificateCode() + 
                    "\nIssued: " + cert.getIssuedAt().format(DATE_FORMATTER), codeFont);
            codePara.setAlignment(Element.ALIGN_CENTER);
            PdfPCell cellCode = new PdfPCell(codePara);
            cellCode.setBorder(Rectangle.BOX);
            cellCode.setBorderColor(new Color(200, 210, 220));
            cellCode.setBackgroundColor(new Color(245, 248, 252));
            cellCode.setPadding(6f);
            cellCode.setHorizontalAlignment(Element.ALIGN_CENTER);
            footerTable.addCell(cellCode);

            // Right: Event Coordinator
            Paragraph sig2 = new Paragraph("__________________________\nFaculty Coordinator\nEvent Organizing Committee", subtitleFont);
            sig2.setAlignment(Element.ALIGN_CENTER);
            PdfPCell cellSig2 = new PdfPCell(sig2);
            cellSig2.setBorder(Rectangle.NO_BORDER);
            cellSig2.setHorizontalAlignment(Element.ALIGN_CENTER);
            footerTable.addCell(cellSig2);

            PdfPCell footerContainer = new PdfPCell(footerTable);
            footerContainer.setBorder(Rectangle.NO_BORDER);
            footerContainer.setPaddingTop(10f);
            innerTable.addCell(footerContainer);

            borderCell.addElement(innerTable);
            borderTable.addCell(borderCell);
            document.add(borderTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF certificate: " + e.getMessage(), e);
        }
    }
}

