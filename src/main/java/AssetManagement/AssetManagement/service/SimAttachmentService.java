package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.SimAttachmentDto;
import AssetManagement.AssetManagement.entity.SimAttachment;
import AssetManagement.AssetManagement.entity.SimCard;
import AssetManagement.AssetManagement.enums.SimStatus;
import AssetManagement.AssetManagement.mapper.SimAttachmentMapper;
import AssetManagement.AssetManagement.repository.SimAttachmentRepository;
import AssetManagement.AssetManagement.repository.SimCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SimAttachmentService {

    private final SimCardRepository simCardRepository;
    private final SimAttachmentRepository simAttachmentRepository;

//    private final String BASE_UPLOAD_DIR = "/uploads/cug/";

    private final String BASE_UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/cug/";


    public SimAttachmentDto uploadAttachment(Long simId, MultipartFile file, String note) throws Exception {

        SimCard sim = simCardRepository.findById(simId)
                .orElseThrow(() -> new RuntimeException("SIM not found"));

        // Create a folder for this SIM
        String simFolderPath = BASE_UPLOAD_DIR + "sim-" + simId;
        File simFolder = new File(simFolderPath);
        if (!simFolder.exists()) {
            simFolder.mkdirs();
        }

        // Final file path
        String filePath = simFolderPath + "/" + file.getOriginalFilename();

        // Save file locally
        File dest = new File(filePath);
        file.transferTo(dest);

        // Create attachment entry
        SimAttachment attachment = new SimAttachment();
        attachment.setSimCard(sim);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setFileUrl(filePath);
        attachment.setNote(note);

        simAttachmentRepository.save(attachment);

        // Update flag → only if status is ASSIGNED & first time
        if (sim.getStatus() == SimStatus.ASSIGNED && !sim.getAssignmentUploaded()) {
            sim.setAssignmentUploaded(true);
            simCardRepository.save(sim);
        }

        return SimAttachmentMapper.toDto(attachment);
    }


    public List<SimAttachmentDto> getAttachments(Long simId) {
        List<SimAttachment> list = simAttachmentRepository.findBySimCardId(simId);
        return list.stream()
                .map(SimAttachmentMapper::toDto)
                .toList();
    }

    public Resource download(Long attachmentId) throws Exception {
        SimAttachment att = simAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));

        File file = new File(att.getFileUrl());
        if (!file.exists()) {
            throw new RuntimeException("File not found on disk");
        }
        return new FileSystemResource(file);
    }

}
