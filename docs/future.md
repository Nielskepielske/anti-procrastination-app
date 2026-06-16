# Future Architectural Improvements

This document tracks ideas and planned architectural improvements for the application that are not yet implemented.

## Storage and Analytics

### Migrate from CSV to Parquet for Completed Sessions
Currently, when a session is completed, its high-frequency sensor data is extracted from the live database and stored locally as a CSV file (which is then compressed) to save database space.

In the future, we should migrate this cold storage format from CSV to **Apache Parquet**.

**Why Parquet?**
- **Columnar Format:** Unlike CSV (which stores data row by row), Parquet stores data column by column. This makes analytics queries incredibly fast because you only need to read the specific columns you are analyzing.
- **Smaller File Size:** Parquet has built-in compression (like dictionary encoding) that is vastly more efficient than plain GZIP on a CSV. It often results in file sizes that are a fraction of the size of compressed CSVs.
- **Schema Evolution:** Parquet supports typed columns and nested data structures better than plain CSV.

**Migration Path:**
1. Keep the local file storage mechanism.
2. Replace the CSV serialization step with a Parquet serialization library.
3. Update the Analytics page to parse Parquet files instead of CSVs (there are good libraries for this in Kotlin/multiplatform or via a web backend if the frontend is web-based).
