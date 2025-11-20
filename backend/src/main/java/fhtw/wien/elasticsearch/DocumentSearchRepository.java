package fhtw.wien.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface DocumentSearchRepository extends ElasticsearchRepository<DocumentIndex, String> {
}
