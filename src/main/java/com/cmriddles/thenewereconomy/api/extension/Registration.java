package com.cmriddles.thenewereconomy.api.extension;
/** A process-local extension registration. Close it when the integrating plugin stops. */
public interface Registration extends AutoCloseable { @Override void close(); }
