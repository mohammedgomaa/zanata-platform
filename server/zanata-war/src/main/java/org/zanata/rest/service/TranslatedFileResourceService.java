/*
 * Copyright 2016, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.zanata.rest.service;

import java.io.InputStream;

import javax.annotation.Nullable;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.apache.deltaspike.jpa.api.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zanata.dao.DocumentDAO;
import org.zanata.file.FilePersistService;
import org.zanata.file.GlobalDocumentId;
import org.zanata.file.TranslationDocumentUpload;
import org.zanata.rest.dto.JobStatus;
import org.zanata.service.TranslationFileService;

import lombok.extern.slf4j.Slf4j;

/**
 * By convention, this would be called TranslatedFileService, but we already have one of those.
 * @author Sean Flanigan <a
 *         href="mailto:sflaniga@redhat.com">sflaniga@redhat.com</a>
 */
@RequestScoped
@Transactional
public class TranslatedFileResourceService implements TranslatedFileResource {

    private static final Logger log = LoggerFactory.getLogger(SourceFileService.class);

    @Inject
    private DocumentDAO documentDAO;
    @Inject
    private FilePersistService filePersistService;
    @Inject
    private ResourceUtils resourceUtils;
    @Inject
    private TranslatedDocResourceService translatedDocResourceService;
    @Inject
    private TranslationDocumentUpload translationUploader;
    @Inject
    private TranslationFileService translationFileService;
    @Inject
    private VirusScanner virusScanner;

    // PUT
    @Override
    public Response uploadTranslationFile(String projectSlug,
            String versionSlug, String localeId, String docId, String merge,
            InputStream fileStream,
            @Nullable Long size,
            String projectType) {
        //assignCreditToUploader is not supported from here
        boolean assignCreditToUploader = false;

        GlobalDocumentId id =
                new GlobalDocumentId(projectSlug, versionSlug, docId);
        // FIXME
        JobStatus jobStatus = new JobStatus();
        return Response.status(Response.Status.ACCEPTED).entity(jobStatus).build();
//        return translationUploader.tryUploadTranslationFile(id, localeId,
//                merge, assignCreditToUploader, fileStream,
//                TranslationSourceType.API_UPLOAD);
    }

    @Override
    public Response downloadTranslationFile(String projectSlug,
            String versionSlug, String locale,
            String docId, String projectType,
            boolean includeFuzzy) {
        // FIXME
        return null;
    }

}
