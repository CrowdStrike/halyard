/*
 * Copyright 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.netflix.spinnaker.halyard.core.registry.v1;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.storage.Storage;
import com.netflix.spinnaker.halyard.core.provider.v1.google.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.apache.commons.io.IOUtils;

@Component
@Slf4j
public class ProfileRegistry {
  @Autowired
  String spinconfigBucket;

  @Autowired
  Storage googleStorage;

  @Autowired
  boolean s3Enabled;

  @Autowired
  AmazonS3 s3Storage;

  @Bean
  public Storage googleStorage() {
    HttpTransport httpTransport;
    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
    String applicationName = "Spinnaker/Halyard";

    return new Storage.Builder(GoogleCredentials.buildHttpTransport(), jsonFactory, GoogleCredentials.emptyRequestInitializer())
        .setApplicationName(applicationName)
        .build();
  }

  @Bean
  public AmazonS3 s3Storage() {
    // Default client using the S3CredentialsProviderChain and DefaultAwsRegionProviderChain chain
    return AmazonS3ClientBuilder.defaultClient();
  }

  public static String profilePath(String artifactName, String version, String profileFileName) {
    return String.join("/", artifactName, version, profileFileName);
  }

  public static String bomPath(String version) {
    return String.join("/", "bom", version + ".yml");
  }

  public InputStream getObjectContents(String objectName) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    log.info("Getting object contents of " + objectName);
    if s3Enabled {
      S3Object s3object = s3Storage.getObject(new GetObjectRequest(spinconfigBucket, objectName));
      return new ByteArrayInputStream(IOUtils.toByteArray(s3object.getObjectContent()));
    } else {
      googleStorage.objects().get(spinconfigBucket, objectName).executeMediaAndDownloadTo(output);
      return new ByteArrayInputStream(output.toByteArray());
    }
  }
}
