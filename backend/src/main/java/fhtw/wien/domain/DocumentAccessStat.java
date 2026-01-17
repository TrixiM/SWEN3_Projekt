package fhtw.wien.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "document_access_stats",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"document_id", "access_date"})})
@Getter
@Setter
@NoArgsConstructor
public class DocumentAccessStat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "access_date", nullable = false)
    private LocalDate accessDate;

    @Column(name = "access_count", nullable = false)
    private int accessCount;

    public DocumentAccessStat(Document document, int accessCount, LocalDate date) {
        this.document=document;
        this.accessCount=accessCount;
        this.accessDate=date;
    }

    // getters and setters
}
