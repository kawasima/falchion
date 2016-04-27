# Falchion

Falchion is a JVM container for the fault tolerant server.
Falchion container requires JDK9 because it's using SO_REUSEPORT flag. JDK9 supports SO_REUSEPORT only on Linux.

When the container has started, it forks JVM processes until the given pool size.
If the container catch a HUP signal, it creates a new JVM processes and kill old processes.
