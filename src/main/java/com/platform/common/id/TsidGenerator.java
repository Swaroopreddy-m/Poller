package com.platform.common.id;

import io.hypersistence.tsid.TSID;
import org.springframework.stereotype.Component;

/**
 * High-performance, time-sortable, URL-safe business reference identifier.
 * Generates REF_NO in the format: MSG-01J9Z5K3V0A4W889BQC19X.
 * Guarantees monotonic ordering in PostgreSQL B-Trees to avoid leaf fragmentation.
 */
@Component
public class TsidGenerator {

    private static final String PREFIX = "MSG-";

    public String generateRefNo() {
        return PREFIX + TSID.fast().toString();
    }
}
