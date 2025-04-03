package AssetManagement.AssetManagement.dto;

import java.time.LocalDateTime;

public class DocumentResponse {
    private String documentUrl;
    private String addedBy;
    private LocalDateTime addedAt;

    public DocumentResponse(String documentUrl, String addedBy, LocalDateTime addedAt) {
        this.documentUrl = documentUrl;
        this.addedBy = addedBy;
        this.addedAt = addedAt;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }
}
