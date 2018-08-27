/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rundeck.verb.client.repository

import com.rundeck.verb.ResponseBatch
import com.rundeck.verb.ResponseCodes
import com.rundeck.verb.ResponseMessage
import com.rundeck.verb.artifact.VerbArtifact
import com.rundeck.verb.client.artifact.RundeckVerbArtifact
import com.rundeck.verb.client.manifest.FilesystemManifestCreator
import com.rundeck.verb.client.util.ArtifactFileset
import com.rundeck.verb.client.util.ArtifactUtils
import com.rundeck.verb.manifest.ManifestCreator
import com.rundeck.verb.manifest.ManifestEntry
import com.rundeck.verb.manifest.ManifestService
import com.rundeck.verb.repository.RepositoryDefinition
import com.rundeck.verb.repository.VerbArtifactRepository


class FilesystemArtifactRepository implements VerbArtifactRepository {
    private static final String ARTIFACT_BASE = "artifacts/"
    private static final String BINARY_BASE = "binary/"
    private RepositoryDefinition repositoryDefinition
    private ManifestService manifestService
    private FilesystemManifestCreator manifestCreator
    private File repoBase

    FilesystemArtifactRepository(RepositoryDefinition repoDef, ManifestService manifestService) {
        this.repositoryDefinition = repoDef
        this.manifestService = manifestService
        repoBase = new File(repoDef.repositoryLocation.toURI())
        if(!repoBase.exists()) {
            if(!repoBase.mkdirs()) throw new Exception("Repository base dir: ${repoBase.absolutePath} does not exist. Unable to create dir")
        }
        File manifestFile = new File(repoDef.manifestLocation.toURI())
        if(!manifestFile.exists()) {
            if(!manifestFile.createNewFile()) throw new Exception("Unable to create repo manifest file: ${manifestFile.absolutePath}")
            manifestFile << "{}"
        }
        ensureExists(repoBase,ARTIFACT_BASE)
        ensureExists(repoBase,BINARY_BASE)
        manifestCreator = new FilesystemManifestCreator(repoBase.absolutePath+"/artifacts")
    }

    void ensureExists(final File base, final String dirName) {
        new File(base,dirName).mkdirs()
    }

    @Override
    RepositoryDefinition getRepositoryDefinition() {
        return repositoryDefinition
    }

    @Override
    VerbArtifact getArtifact(final String artifactId, final String version = null) {
        String artifactVer = version ?: manifestService.getEntry(artifactId).currentVersion
        return ArtifactUtils.createArtifactFromStream(new File(repoBase,ARTIFACT_BASE+ArtifactUtils.artifactMetaFileName(artifactId,artifactVer)).newInputStream())
    }

    @Override
    InputStream getArtifactBinary(final String artifactId, final String version = null) {
        ManifestEntry entry = manifestService.getEntry(artifactId)
        String artifactVer = version ?: entry.currentVersion
        String extension = ArtifactUtils.artifactTypeFromNice(entry.artifactType).extension
        return new File(repoBase,BINARY_BASE+ArtifactUtils.artifactBinaryFileName(artifactId,artifactVer,extension)).newInputStream()
    }

    @Override
    ResponseBatch uploadArtifact(final InputStream artifactInputStream) {
        ResponseBatch responseBatch = new ResponseBatch()
        ArtifactFileset artifactFileset = ArtifactUtils.constructArtifactFileset(artifactInputStream)
        //responseBatch.messages.addAll(validateArtifactBinary(uploadTmp))
        if(!responseBatch.batchSucceeded()) return responseBatch

        responseBatch.messages.addAll(uploadArtifactMeta(artifactFileset.artifact, artifactFileset.artifactMeta.newInputStream()).messages)
        if(artifactFileset.hasBinary()) {
            responseBatch.messages.addAll(uploadArtifactBinary(artifactFileset.artifact, artifactFileset.artifactBinary.newInputStream()).messages)
        }
        responseBatch
    }

    ResponseBatch uploadArtifactMeta(final RundeckVerbArtifact artifact, final InputStream inputStream) {
        ResponseBatch rbatch = new ResponseBatch()
        try {
            File saveFile = new File(repoBase,ARTIFACT_BASE+ArtifactUtils.artifactMetaFileName(artifact))
            saveFile << inputStream
            recreateAndSaveManifest()
            manifestService.syncManifest()
            rbatch.addMessage(ResponseMessage.success())
        } catch(Exception ex) {
            rbatch.addMessage(new ResponseMessage(code: ResponseCodes.META_UPLOAD_FAILED,message:ex.message))
        }
        return rbatch
    }

    ResponseBatch uploadArtifactBinary(final RundeckVerbArtifact artifact, final InputStream inputStream) {
        ResponseBatch rbatch = new ResponseBatch()
        try {
            File saveFile = new File(repoBase,BINARY_BASE+ArtifactUtils.artifactBinaryFileName(artifact))
            saveFile << inputStream
            rbatch.addMessage(ResponseMessage.success())
        } catch(Exception ex) {
            rbatch.addMessage(new ResponseMessage(code: ResponseCodes.META_UPLOAD_FAILED,message:ex.message))
        }
        return rbatch
    }

    @Override
    ManifestService getManifestService() {
        return manifestService
    }

    @Override
    void configure(final Map configProperties) {}

    void recreateAndSaveManifest() {
        //TODO: this assumes the URL used in the repo defn is a writable location.
        ArtifactUtils.writeArtifactManifestToFile(manifestCreator.createManifest(),new File(repositoryDefinition.manifestLocation.toURI()).newOutputStream())
    }
}
