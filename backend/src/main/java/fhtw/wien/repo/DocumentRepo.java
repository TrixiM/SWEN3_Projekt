package fhtw.wien.repo;

import fhtw.wien.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepo extends JpaRepository<Document, UUID>{

Optional<Document> findByChecksumSha256(String checksumSha256);

}