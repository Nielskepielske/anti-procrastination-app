package com.example.procrastination_detection.domain.pipeline.resampling

interface WindowedReducer<T> {
    /** * Takes a list of items that occurred in a specific time bucket
     * and reduces them to a single float value for a chart.
     */
    fun reduce(bucketTimestamp: Long, items: List<T>): Float
}