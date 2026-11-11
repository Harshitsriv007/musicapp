package com.musicapp.download;

import java.util.Optional;

/** A single HTTP byte range ({@code Range: bytes=start-end}), end inclusive. */
public record ByteRange(long start, long end) {

    public long length() {
        return end - start + 1;
    }

    public static class NotSatisfiableException extends RuntimeException {
        public NotSatisfiableException(String header) {
            super("Range not satisfiable: " + header);
        }
    }

    /**
     * Parses the first range of a Range header against a resource of {@code size} bytes.
     * Returns empty for no/unsupported header (serve the whole file).
     */
    public static Optional<ByteRange> parse(String header, long size) {
        if (header == null || !header.startsWith("bytes=")) {
            return Optional.empty();
        }
        String spec = header.substring(6).split(",")[0].trim();
        int dash = spec.indexOf('-');
        if (dash < 0) {
            return Optional.empty();
        }
        try {
            String a = spec.substring(0, dash).trim();
            String b = spec.substring(dash + 1).trim();
            long start;
            long end;
            if (a.isEmpty()) { // suffix range: last N bytes
                long n = Long.parseLong(b);
                if (n <= 0) {
                    throw new NotSatisfiableException(header);
                }
                start = Math.max(0, size - n);
                end = size - 1;
            } else {
                start = Long.parseLong(a);
                end = b.isEmpty() ? size - 1 : Math.min(Long.parseLong(b), size - 1);
            }
            if (start >= size || start > end) {
                throw new NotSatisfiableException(header);
            }
            return Optional.of(new ByteRange(start, end));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
