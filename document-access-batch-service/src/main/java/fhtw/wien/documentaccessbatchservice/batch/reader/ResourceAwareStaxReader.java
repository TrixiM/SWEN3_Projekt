package fhtw.wien.documentaccessbatchservice.batch.reader;

import org.springframework.batch.item.ResourceAware;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.core.io.Resource;

public class ResourceAwareStaxReader<T> extends StaxEventItemReader<T>
        implements ResourceAware {

    private Resource resource;

    @Override
    public void setResource(Resource resource) {
        super.setResource(resource);
        this.resource = resource;
    }
    public Resource getResource() {
        return resource;
    }
}