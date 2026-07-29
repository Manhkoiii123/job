package com.manh.job.service.impl;

import com.manh.job.mapper.JobTagMapper;
import com.manh.job.model.Job;
import com.manh.job.model.JobTag;
import com.manh.job.payload.request.JobTagRequest;
import com.manh.job.payload.response.JobTagResponse;
import com.manh.job.repository.JobTagRepository;
import com.manh.job.service.JobTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobTagServiceImpl implements JobTagService {
    private final JobTagRepository jobTagRepository;
    @Override
    public JobTagResponse createTag(JobTagRequest req) throws Exception {
        if(jobTagRepository.existsByName(req.getName())) {
            throw new Exception("Tag name already exist");
        }
        String slug = generateUniqueSlug(req.getName());
        JobTag jobTag = JobTag.builder()
                .name(req.getName())
                .slug(slug)
                .build();
        JobTag saved = jobTagRepository.save(jobTag);
        return JobTagMapper.toTagResponse(saved);
    }

    @Override
    public List<JobTagResponse> getAllTags() {
        return jobTagRepository.findAll()
                .stream().map(JobTagMapper::toTagResponse)
                .collect(Collectors.toList());
    }

    @Override
    public JobTagResponse getById(Long id) throws Exception {
        JobTag jobtag = getTagEntityById(id);
        return JobTagMapper.toTagResponse(jobtag);
    }

    @Override
    public JobTagResponse updateTag(Long id, JobTagRequest req) throws Exception {
        JobTag jobtag = getTagEntityById(id);
        if(!jobtag.getName().equals(req.getName()) && jobTagRepository.existsByName(req.getName())) {
            throw new Exception("Tag name already exist");
        }
        jobtag.setName(req.getName());
        JobTag saved = jobTagRepository.save(jobtag);
        return JobTagMapper.toTagResponse(saved);
    }

    @Override
    public void deleteTag(Long id) throws Exception {
        JobTag jobtag = getTagEntityById(id);
        jobTagRepository.delete(jobtag);
    }

    @Override
    public JobTag getTagEntityById(Long id) throws Exception {
       return jobTagRepository.findById(id).orElseThrow(
               () -> new Exception("Job tag not found")
       );
    }

    private String generateUniqueSlug(String name) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("[\\s-]+", "-");

        if (!jobTagRepository.existsBySlug(base)) {
            return base;
        }

        int counter = 1;
        while (jobTagRepository.existsBySlug(base + "-" + counter)) {
            counter++;
        }

        return base + "-" + counter;
    }
}
