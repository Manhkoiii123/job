package com.manh.job.service.impl;

import com.manh.job.dto.JobCategoryResponse;
import com.manh.job.mapper.JobCategoryMapper;
import com.manh.job.mapper.JobMapper;
import com.manh.job.model.JobCategory;
import com.manh.job.payload.request.JobCategoryRequest;
import com.manh.job.repository.JobCategoryRepository;
import com.manh.job.service.JobCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobCategoryServiceImpl implements JobCategoryService {

    private final JobCategoryRepository jobCategoryRepository;

    @Override
    public JobCategoryResponse createCategory(JobCategoryRequest req) throws Exception {
        if(jobCategoryRepository.existsByName(req.getName())) {
            throw  new Exception("Category name already exist, choose different name");
        }
        JobCategory parent = null;
        if(req.getParentId()!=null) {
            parent = getCategoryEntityById(req.getParentId());
        }
        String slug = generateUniqueSlug(req.getName());

        JobCategory category = JobCategory.builder()
                .name(req.getName())
                .slug(slug)
                .description(req.getDescription())
                .iconUrl(req.getIconUrl())
                .parent(parent)
                .build();
        JobCategory saved =  jobCategoryRepository.save(category);

        return JobCategoryMapper.toJobCategoryResponse(saved,true);
    }

    @Override
    public List<JobCategoryResponse> getAllCategories() {
        return jobCategoryRepository.findByActiveTrue().stream()
                .map((c -> JobCategoryMapper.toJobCategoryResponse(c,false)))
                .collect(Collectors.toList());
    }

    @Override
    public JobCategoryResponse getCategoryById(Long id) throws Exception {
       JobCategory jobCategory = getCategoryEntityById(id);
       return JobCategoryMapper.toJobCategoryResponse(jobCategory,true);
    }

    @Override
    public JobCategoryResponse updateCategory(Long id, JobCategoryRequest req) throws Exception {
        JobCategory category = getCategoryEntityById(id);

        if (!category.getName().equals(req.getName()) &&
                jobCategoryRepository.existsByName(req.getName())) {
            throw new Exception("Category name already exist, choose different name");
        }

        JobCategory parent = null;
        if (req.getParentId() != null) {
            if (req.getParentId().equals(id)) {
                throw new Exception("A category cannot be its own parent");
            }
            parent = getCategoryEntityById(req.getParentId());
        }

        category.setName(req.getName());
        category.setDescription(req.getDescription());
        category.setIconUrl(req.getIconUrl());
        category.setParent(parent);

        JobCategory updated = jobCategoryRepository.save(category);
        return JobCategoryMapper.toJobCategoryResponse(updated,true);
    }

    @Override
    public void deleteCategory(Long id) throws Exception {
        JobCategory category = getCategoryEntityById(id);
        category.setActive(false);
        jobCategoryRepository.save(category);
    }

    @Override
    public JobCategory getCategoryEntityById(Long id) throws Exception {
       return jobCategoryRepository.findById(id).orElseThrow(
               () -> new Exception("Category not found")
       );
    }

    private String generateUniqueSlug(String name) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("[\\s-]+", "-");

        if (!jobCategoryRepository.existsBySlug(base)) {
            return base;
        }

        int counter = 1;
        while (jobCategoryRepository.existsBySlug(base + "-" + counter)) {
            counter++;
        }

        return base + "-" + counter;
    }
}
