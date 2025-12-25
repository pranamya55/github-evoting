/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.process.sharepublickeys;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public record CertificatesDownloadDto(String filename, String content) {
    public CertificatesDownloadDto {
        checkNotNull(filename);
        checkNotNull(content);
        checkState(!filename.isBlank());
        checkState(!content.isBlank());
    }
}
