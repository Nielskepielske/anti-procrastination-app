package com.example.procrastination_detection.domain.pipeline.resampling

object TimeSeriesResampler {

    /**
     * Slices a timeframe into equal buckets and groups the data into them.
     * Guarantees that empty buckets are created so charts don't have visual gaps.
     */
    fun <T> bucketData(
        data: List<T>,
        startTime: Long,
        endTime: Long,
        bucketSizeMillis: Long,
        timestampSelector: (T) -> Long
    ): Map<Long, List<T>> {
        val buckets = mutableMapOf<Long, MutableList<T>>()

        // 1. Pre-fill all buckets with empty lists to guarantee uniform chart x-axes
        var currentBucket = startTime - (startTime % bucketSizeMillis)
        while (currentBucket <= endTime) {
            buckets[currentBucket] = mutableListOf()
            currentBucket += bucketSizeMillis
        }

        // 2. Drop the data into the correct buckets
        data.forEach { item ->
            val timestamp = timestampSelector(item)
            val bucketStart = timestamp - (timestamp % bucketSizeMillis)

            // Only add if it falls within our chart's timeframe
            buckets[bucketStart]?.add(item)
        }

        return buckets
    }
}