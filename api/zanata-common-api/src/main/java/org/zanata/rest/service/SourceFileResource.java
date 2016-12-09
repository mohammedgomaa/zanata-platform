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
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.webcohesion.enunciate.metadata.rs.TypeHint;
import org.zanata.common.LocaleId;
import org.zanata.common.ProjectType;
import org.zanata.rest.dto.JobStatus;

import static org.zanata.rest.service.RestConstants.SOURCE_FILE_SERVICE_PATH;

/**
 * REST Interface for upload and download of source files.
 * @author Sean Flanigan <a
 *         href="mailto:sflaniga@redhat.com">sflaniga@redhat.com</a>
 */
@Path(SOURCE_FILE_SERVICE_PATH)
@Consumes({ MediaType.WILDCARD })
@Produces({ MediaType.WILDCARD })
public interface SourceFileResource extends RestResource {

    /**
     * Upload a source file as a stream to Zanata. File will be streamed to
     * disk as it uploads, then processed asynchronously.
     *
     * @param projectSlug The project slug where to store the document.
     * @param versionSlug The project version slug where to store the document.
     * @param docId The full Document identifier (including file extension)
     * @param fileStream Contents of the file to be uploaded
     * @param size size of the file in bytes (for sanity checking; use null (omit) if unknown)
     * @param projectType A ProjectType used for mapping of file extensions.
     *                    This value may be stored if the project does not
     *                    have a ProjectType set already.
     * @return A JobStatus with information about the upload operation.
     * @see JobStatus
     */
    @PUT
    @Consumes({ MediaType.WILDCARD })
    @Produces({ MediaType.APPLICATION_JSON })
    @TypeHint(JobStatus.class)
    Response uploadSourceFile(
            @PathParam("projectSlug") String projectSlug,
            @PathParam("versionSlug") String versionSlug,
            @QueryParam("docId") String docId,
            @QueryParam("lang") LocaleId lang,
            InputStream fileStream,
            // We could use Content-Length, but RESTEasy client API doesn't
            // make it easy to set Content-Length for InputStream (only File).
            @QueryParam("size") @Nullable Long size,
            // TODO rename this to clientProjectType, asProjectType, asFileType, asType?
            @QueryParam("projectType") @Nullable ProjectType projectType);

    /**
     * Downloads a single source file.
     *
     * @param projectSlug
     * @param versionSlug
     * @param docId The full Document identifier
     * @param projectType A ProjectType used for mapping of file extensions.
     * @return response with status code 404 if the document is not found, 415
     *         if fileType is not valid for the document, otherwise 200 with
     *         attached document in the response body.
     */
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Response downloadSourceFile(
            @PathParam("projectSlug") String projectSlug,
            @PathParam("versionSlug") String versionSlug,
            @QueryParam("docId") String docId,
            @QueryParam("projectType") String projectType);

}
