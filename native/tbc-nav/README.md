# tbcnav (optional)

Detour JNI used by `org.tbc.world.mmap.DetourNative`. CI compiles Java without this library.

```
cmake -S native/tbc-nav -B native/tbc-nav/build
cmake --build native/tbc-nav/build
```

Put `tbcnav.dll` / `libtbcnav.so` on `java.library.path`. `mmap.enabled` + DataDir `mmaps/` then uses Detour. Without the native lib, PathFinder reports unavailable / shortcut (in-memory) or NOPATH when mesh is required.
