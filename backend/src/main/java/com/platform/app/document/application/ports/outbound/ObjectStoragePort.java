package com.platform.app.document.application.ports.outbound;

import java.io.InputStream;

public interface ObjectStoragePort {

  /**
   * Uploads an input stream to object storage at the specified storage key.
   *
   * @param storageKey target key in bucket (e.g. documents/{doc_id}/v1/{file_name})
   * @param inputStream stream containing the file binary
   * @param contentLength size in bytes of the content
   * @param contentType MIME content type
   */
  void upload(String storageKey, InputStream inputStream, long contentLength, String contentType);

  /**
   * Deletes an object from object storage by storage key (used for compensation).
   *
   * @param storageKey target key in bucket to delete
   */
  void delete(String storageKey);
}
