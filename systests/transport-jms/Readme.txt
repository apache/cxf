When creating tests make sure you close the destinations and conduits after use.

Destinations are closed automatically when their bus is shut down. Conduits have to be closed by casting the
service proxy to Closeable and closing it.
