package com.example.videostreaming.controller;

import com.example.videostreaming.model.VideoMetadata;
import com.example.videostreaming.repository.VideoRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "*")
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);
    private final VideoRepository videoRepository;
    private Path uploadPath;

    @Value("${video.upload.dir}")
    private String uploadDir;

    public VideoController(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @PostConstruct
    public void init() {
        try {
            this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideo(@RequestParam("file") MultipartFile file,
                                         @RequestParam("title") String title) throws IOException {
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path dest = uploadPath.resolve(filename);
        file.transferTo(dest);

        VideoMetadata metadata = new VideoMetadata();
        metadata.setFilename(filename);
        metadata.setTitle(title);
        videoRepository.save(metadata);

        logger.info("Uploaded video: {} (title: {})", filename, title);
        return ResponseEntity.ok(metadata);
    }

    @GetMapping
    public List<VideoMetadata> listVideos() {
        return videoRepository.findAll();
    }

    @GetMapping("/stream/{id}")
    public ResponseEntity<InputStreamResource> streamVideo(
            @PathVariable Long id,
            @RequestHeader(value = "Range", required = false) String rangeHeader) throws IOException {

        VideoMetadata metadata = videoRepository.findById(id).orElseThrow();
        Path videoPath = uploadPath.resolve(metadata.getFilename());
        long fileLength = Files.size(videoPath);

        long rangeStart = 0;
        long rangeEnd = fileLength - 1;
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.substring(6).split("-");
            rangeStart = Long.parseLong(ranges[0]);
            if (ranges.length > 1 && !ranges[1].isEmpty()) {
                rangeEnd = Long.parseLong(ranges[1]);
            }
        }

        long contentLength = rangeEnd - rangeStart + 1;
        InputStream inputStream = Files.newInputStream(videoPath);
        inputStream.skip(rangeStart);

        logger.info("Streaming video: {} (ID: {}), Range: {} ({}-{} / {})", metadata.getFilename(), id, rangeHeader, rangeStart, rangeEnd, fileLength);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "video/mp4");
        headers.set("Accept-Ranges", "bytes");
        headers.set("Content-Length", String.valueOf(contentLength));
        headers.set("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .body(new InputStreamResource(inputStream));
    }
} 