package core.interfaces;

import utilities.StatSummary;

import java.util.*;

public interface IStatisticLogger {

    /**
     * Use to register a set of data in one go
     *
     * @param data A map of name -> value pairs
     */
    void record(Map<String, ?> data);

    /**
     * Use to record a single datum. For example
     * @param key
     * @param datum
     */
    void record(String key, Object datum);

    /**
     * Trigger any specific batch processing of data by this Logger.
     * This should be called once all data has been collected. This may also, for example,
     * purge all buffers and close files/database connections.
     */
    void processDataAndFinish();

    /**
     * This should return a Map with one entry for each type of data
     *
     * @return A summary of the data
     */
    Map<String, StatSummary> summary();

}
