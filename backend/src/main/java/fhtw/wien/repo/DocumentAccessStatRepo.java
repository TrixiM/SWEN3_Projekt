package fhtw.wien.repo;

import fhtw.wien.domain.DocumentAccessStat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentAccessStatRepo extends JpaRepository<DocumentAccessStat, Long> {
}
