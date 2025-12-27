package com.switflow.swiftFlow.Service;

import com.switflow.swiftFlow.Entity.Status;
import com.switflow.swiftFlow.Repo.StatusRepository;
import com.switflow.swiftFlow.Response.StatusResponse;
import com.switflow.swiftFlow.pdf.PdfRow;
import com.switflow.swiftFlow.pdf.PdfRowExtractor;
import com.switflow.swiftFlow.utility.Department;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Objects;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PdfService {

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private StatusService statusService;

    public List<PdfRow> analyzePdfRows(long orderId) throws IOException {
        String pdfUrl = findDesignPdfUrl(orderId);
        if (pdfUrl == null) {
            return new ArrayList<>();
        }

        try (InputStream in = new URL(pdfUrl).openStream(); PDDocument document = PDDocument.load(in)) {
            PdfRowExtractor extractor = new PdfRowExtractor();
            extractor.setStartPage(1);
            extractor.setEndPage(document.getNumberOfPages());
            // We only need PdfRowExtractor's collected row metadata; discard text output
            extractor.writeText(document, new StringWriter());
            return extractor.getRows();
        }
    }

    private String findDesignPdfUrl(long orderId) {
        List<Status> statuses = statusRepository.findByOrdersOrderId(orderId);
        return statuses.stream()
                .filter(s -> s.getAttachmentUrl() != null
                        && s.getAttachmentUrl().toLowerCase().endsWith(".pdf")
                        && s.getNewStatus() == Department.DESIGN)
                .sorted(Comparator.comparing(Status::getId).reversed())
                .map(Status::getAttachmentUrl)
                .findFirst()
                .orElse(null);
    }

    public StatusResponse generateFilteredPdf(long orderId, List<String> selectedRowIds) throws IOException {
        String pdfUrl = findDesignPdfUrl(orderId);
        if (pdfUrl == null) {
            throw new IllegalStateException("No DESIGN PDF found for order " + orderId);
        }

        List<PdfRow> allRows = analyzePdfRows(orderId);
        Map<String, PdfRow> rowMap = allRows.stream()
                .collect(Collectors.toMap(PdfRow::getRowId, r -> r));

        List<PdfRow> selectedRows = selectedRowIds.stream()
                .map(rowMap::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(PdfRow::getPageNumber).thenComparing(PdfRow::getYPosition))
                .collect(Collectors.toList());

        if (selectedRows.isEmpty()) {
            throw new IllegalArgumentException("No valid rows selected");
        }

        byte[] filteredPdfBytes = createSimplePdfFromRows(selectedRows);
        String filteredUrl = cloudinaryService.uploadBytes(filteredPdfBytes);

        // Always create PRODUCTION status pointing at DESIGN-based filtered PDF
        return statusService.createFilteredPdfStatus(orderId, filteredUrl, Department.PRODUCTION);
    }

    public StatusResponse saveRowSelection(long orderId,
                                           List<String> selectedRowIds,
                                           Department targetStatus,
                                           String attachmentUrl,
                                           Long machineId,
                                           String machineName) {
        StringBuilder commentBuilder = new StringBuilder();
        commentBuilder.append("{\"selectedRowIds\":");
        commentBuilder.append(selectedRowIds.toString());
        if (machineId != null) {
            commentBuilder.append(",\"machineId\":").append(machineId);
        }
        if (machineName != null && !machineName.isBlank()) {
            commentBuilder.append(",\"machineName\":\"")
                    .append(machineName.replace("\"", "\\\""))
                    .append("\"");
        }
        commentBuilder.append('}');

        String jsonComment = commentBuilder.toString();
        com.switflow.swiftFlow.Request.StatusRequest request = new com.switflow.swiftFlow.Request.StatusRequest();
        // Use provided targetStatus when available, defaulting to PRODUCTION
        Department effectiveStatus = (targetStatus != null) ? targetStatus : Department.PRODUCTION;
        request.setNewStatus(effectiveStatus);
        request.setComment(jsonComment);
        request.setPercentage(null);

        // Prefer explicit attachmentUrl from client (e.g. uploaded PDF),
        // but fall back to the DESIGN PDF if none is provided.
        String finalUrl = (attachmentUrl != null && !attachmentUrl.isBlank())
                ? attachmentUrl
                : findDesignPdfUrl(orderId);
        request.setAttachmentUrl(finalUrl);

        return statusService.createStatus(request, orderId);
    }

    // Variant used by the three-checkbox selection flow: it records the
    // selectedRowIds in a Status comment but does NOT update the
    // Orders.department field. Workflow transitions are handled
    // separately via StatusController.createStatus.
    public StatusResponse saveRowSelectionWithoutTransition(long orderId,
                                                            List<String> selectedRowIds,
                                                            Department targetStatus,
                                                            String attachmentUrl,
                                                            Long machineId,
                                                            String machineName) {
        StringBuilder commentBuilder = new StringBuilder();
        commentBuilder.append("{\"selectedRowIds\":");
        commentBuilder.append(selectedRowIds.toString());
        if (machineId != null) {
            commentBuilder.append(",\"machineId\":").append(machineId);
        }
        if (machineName != null && !machineName.isBlank()) {
            commentBuilder.append(",\"machineName\":\"")
                    .append(machineName.replace("\"", "\\\""))
                    .append("\"");
        }
        commentBuilder.append('}');

        String jsonComment = commentBuilder.toString();
        com.switflow.swiftFlow.Request.StatusRequest request = new com.switflow.swiftFlow.Request.StatusRequest();
        Department effectiveStatus = (targetStatus != null) ? targetStatus : Department.PRODUCTION;
        request.setNewStatus(effectiveStatus);
        request.setComment(jsonComment);
        request.setPercentage(null);

        String finalUrl = (attachmentUrl != null && !attachmentUrl.isBlank())
                ? attachmentUrl
                : findDesignPdfUrl(orderId);
        request.setAttachmentUrl(finalUrl);

        return statusService.createCheckboxStatus(request, orderId);
    }

    private byte[] createSimplePdfFromRows(List<PdfRow> rows) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                content.setFont(PDType1Font.HELVETICA, 10);
                float leading = 14.0f;
                float margin = 40;
                PDRectangle mediaBox = page.getMediaBox();
                float y = mediaBox.getUpperRightY() - margin;

                content.beginText();
                content.newLineAtOffset(margin, y);

                for (PdfRow row : rows) {
                    content.showText(row.getText());
                    content.newLineAtOffset(0, -leading);
                }

                content.endText();
            }

            doc.save(out);
            return out.toByteArray();
        }
    }
}
