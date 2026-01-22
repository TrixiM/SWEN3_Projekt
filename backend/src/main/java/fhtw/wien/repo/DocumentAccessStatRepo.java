package fhtw.wien.repo;

import fhtw.wien.domain.DocumentAccessStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentAccessStatRepo extends JpaRepository<DocumentAccessStat, UUID> {
    @Query("SELECT s FROM DocumentAccessStat s WHERE s.document.id = :documentId AND s.accessDate = :accessDate")
    Optional<DocumentAccessStat> findByDocumentAndDate(@Param("documentId") UUID documentId,
                                                       @Param("accessDate") LocalDate accessDate);

    @Query("SELECT s FROM DocumentAccessStat s WHERE s.document.id = :documentId")
    List<DocumentAccessStat> findByDocumentId(@Param("documentId") UUID documentId);


    void deleteByDocumentId(UUID documentId);


}
