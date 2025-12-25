/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
package ch.post.it.evoting.directtrusttool.backend.process.downloadkeystores;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public record KeystoresDownloadDto(String filename, String content) {
    public KeystoresDownloadDto {
        checkNotNull(filename);
        checkNotNull(content);
        checkState(!filename.isBlank());
        checkState(!content.isBlank());
    }
}
