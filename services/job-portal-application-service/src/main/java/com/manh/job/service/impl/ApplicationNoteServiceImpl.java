package com.manh.job.service.impl;

import com.manh.job.dto.request.AddApplicationNoteRequest;
import com.manh.job.dto.response.ApplicationNoteResponse;
import com.manh.job.mapper.ApplicationMapper;
import com.manh.job.modal.Application;
import com.manh.job.modal.ApplicationNote;
import com.manh.job.repository.ApplicationNoteRepository;
import com.manh.job.service.ApplicationNoteService;
import com.manh.job.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationNoteServiceImpl implements ApplicationNoteService {
    private final ApplicationService applicationService;
    private final ApplicationNoteRepository applicationNoteRepository;

    @Override
    public ApplicationNoteResponse addNote(Long applicationId, Long employerId, AddApplicationNoteRequest req) throws Exception {
        Application application = applicationService.getApplicationEntity(applicationId);

        assertEmployer(application, employerId);

        ApplicationNote applicationNote = ApplicationNote.builder()
                .application(application)
                .addedByUserId(employerId)
                .content(req.getContent())
                .build();

        ApplicationNote savedNote = applicationNoteRepository.save(applicationNote);

        return ApplicationMapper.toNoteResponse(savedNote);
    }

    @Override
    public List<ApplicationNoteResponse> getNotesByApplication(Long applicationId, Long employerId) {
        return applicationNoteRepository.findByApplicationId(applicationId)
                .stream().map(ApplicationMapper::toNoteResponse).toList();
    }

    @Override
    public void deleteNote(Long applicationId, Long noteId, Long employerId) throws Exception {
        Application application = applicationService.getApplicationEntity(applicationId);

        assertEmployer(application, employerId);

        ApplicationNote note = applicationNoteRepository.findById(noteId).orElseThrow(
                () -> new Exception("not does not belong to application")
        );
        applicationNoteRepository.delete(note);
    }
    private void assertEmployer(Application application, Long employerId) throws Exception {
        if (!application.getEmployerId().equals(employerId)) {
            throw new Exception("you are not the employer for this application");
        }
    }
}
