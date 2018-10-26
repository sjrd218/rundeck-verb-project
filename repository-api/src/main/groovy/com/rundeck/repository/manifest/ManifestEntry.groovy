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
package com.rundeck.repository.manifest

import com.rundeck.repository.manifest.search.Searchable

class ManifestEntry {
    String id
    @Searchable
    String name
    @Searchable
    String author
    @Searchable
    String description
    @Searchable
    String organization
    String artifactType
    @Searchable
    String support
    @Searchable
    String currentVersion
    @Searchable
    String rundeckCompatibility
    @Searchable
    String targetHostCompatibility
    String binaryLink
    Long lastRelease
    @Searchable
    Collection<String> tags = []
    @Searchable
    Collection<String> providesServices = []
    Collection<String> oldVersions = []
    Integer rating //1-5 stars
    Integer installs
    boolean installed

    static List<String> searchableFieldList() {
        def fieldList = []
        ManifestEntry.getDeclaredFields().each {
            if(it.getAnnotation(Searchable)) { fieldList.add(it.name)}
        }
        return fieldList.sort()
    }

    boolean equals(final o) {
        if (this.is(o)) {
            return true
        }
        if (getClass() != o.class) {
            return false
        }

        final ManifestEntry that = (ManifestEntry) o

        if (id != that.id) {
            return false
        }

        return true
    }

    int hashCode() {
        return (id != null ? id.hashCode() : 0)
    }
}
