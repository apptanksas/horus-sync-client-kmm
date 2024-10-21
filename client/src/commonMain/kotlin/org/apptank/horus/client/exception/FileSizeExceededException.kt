package org.apptank.horus.client.exception

class FileSizeExceededException(maxSize: Int) : Exception(
    "The file size exceeds the maximum allowed. Max size: $maxSize bytes."
)