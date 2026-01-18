package fhtw.wien.repo;

import fhtw.wien.domain.DocumentAccessStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DocumentAccessStatRepo extends JpaRepository<DocumentAccessStat, Long> {
    @Query("SELECT s FROM DocumentAccessStat s WHERE s.document.id = :documentId AND s.accessDate = :accessDate")
    Optional<DocumentAccessStat> findByDocumentAndDate(@Param("documentId") UUID documentId,
                                                       @Param("accessDate") LocalDate accessDate);

}
