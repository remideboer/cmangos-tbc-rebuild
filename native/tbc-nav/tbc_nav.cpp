/*
 * Optional Detour JNI (tbcnav). Build: cmake -S native/tbc-nav -B native/tbc-nav/build
 * Copies Detour from ../recastnavigation/Detour (vendored, not mangos-tbc).
 */
#include <jni.h>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <mutex>
#include <string>
#include <unordered_map>
#include <vector>

#include "DetourNavMesh.h"
#include "DetourNavMeshQuery.h"
#include "DetourCommon.h"

#define MMAP_MAGIC 0x4d4d4150
#define MMAP_VERSION 8
#define SIZE_OF_GRIDS 533.33333f
#define MAX_POLYS 74
#define MAX_POINTS 74

struct TileHeader {
    unsigned int mmapMagic;
    unsigned int dtVersion;
    unsigned int mmapVersion;
    unsigned int size;
    unsigned int usesLiquids;
};

struct MapData {
    dtNavMesh* mesh{};
    dtNavMeshQuery* query{};
};

static std::string g_base;
static std::mutex g_mu;
static std::unordered_map<int, MapData> g_maps;

static int gridX(float x) {
    return (int)(32.f - x / SIZE_OF_GRIDS);
}
static int gridY(float y) {
    return (int)(32.f - y / SIZE_OF_GRIDS);
}

static bool loadMap(int mapId) {
    if (g_maps.count(mapId)) {
        return g_maps[mapId].mesh != nullptr;
    }
    char name[64];
    snprintf(name, sizeof(name), "%03d.mmap", mapId);
    std::string path = g_base + "/mmaps/" + name;
    FILE* f = fopen(path.c_str(), "rb");
    if (!f) {
        g_maps[mapId] = {};
        return false;
    }
    dtNavMeshParams params{};
    if (fread(&params, sizeof(params), 1, f) != 1) {
        fclose(f);
        g_maps[mapId] = {};
        return false;
    }
    fclose(f);
    dtNavMesh* mesh = dtAllocNavMesh();
    if (!mesh || dtStatusFailed(mesh->init(&params))) {
        if (mesh) {
            dtFreeNavMesh(mesh);
        }
        g_maps[mapId] = {};
        return false;
    }
    MapData d;
    d.mesh = mesh;
    d.query = dtAllocNavMeshQuery();
    if (!d.query || dtStatusFailed(d.query->init(mesh, 2048))) {
        dtFreeNavMesh(mesh);
        if (d.query) {
            dtFreeNavMeshQuery(d.query);
        }
        g_maps[mapId] = {};
        return false;
    }
    g_maps[mapId] = d;
    return true;
}

static bool loadTile(int mapId, int gx, int gy) {
    auto it = g_maps.find(mapId);
    if (it == g_maps.end() || !it->second.mesh) {
        return false;
    }
    char name[64];
    snprintf(name, sizeof(name), "%03d%02d%02d.mmtile", mapId, gx, gy);
    std::string path = g_base + "/mmaps/" + name;
    FILE* f = fopen(path.c_str(), "rb");
    if (!f) {
        return false;
    }
    TileHeader hdr{};
    if (fread(&hdr, sizeof(hdr), 1, f) != 1 || hdr.mmapMagic != MMAP_MAGIC || hdr.mmapVersion != MMAP_VERSION) {
        fclose(f);
        return false;
    }
    unsigned char* data = (unsigned char*)dtAlloc(hdr.size, DT_ALLOC_PERM);
    if (!data || fread(data, hdr.size, 1, f) != 1) {
        fclose(f);
        if (data) {
            dtFree(data);
        }
        return false;
    }
    fclose(f);
    dtTileRef ref = 0;
    dtStatus st = it->second.mesh->addTile(data, hdr.size, DT_TILE_FREE_DATA, 0, &ref);
    if (dtStatusFailed(st)) {
        dtFree(data);
        return false;
    }
    return true;
}

static void ensureTiles(int mapId, float x, float y) {
    int gx = gridX(x);
    int gy = gridY(y);
    loadTile(mapId, gx, gy);
}

static std::vector<float> queryPath(int mapId, float sx, float sy, float sz,
        float dx, float dy, float dz, bool straight) {
    std::lock_guard<std::mutex> lock(g_mu);
    if (!loadMap(mapId)) {
        return {};
    }
    ensureTiles(mapId, sx, sy);
    ensureTiles(mapId, dx, dy);
    MapData& d = g_maps[mapId];
    float start[3] = {sy, sz, sx}; // Detour: recast Y-up, CMaNGOS converts
    float end[3] = {dy, dz, dx};
    /* CMaNGOS PathFinder uses (x, z, y) in recast space: Vector3(y, z, x) wait —
       PathFinder.cpp: float vertices[VERTEX_SIZE] = { y, z, x };  WoW (x,y,z) → recast (y,z,x) */
    start[0] = sy;
    start[1] = sz;
    start[2] = sx;
    end[0] = dy;
    end[1] = dz;
    end[2] = dx;
    float ext[3] = {3.f, 5.f, 3.f};
    dtQueryFilter filter;
    dtPolyRef startRef = 0, endRef = 0;
    d.query->findNearestPoly(start, ext, &filter, &startRef, start);
    d.query->findNearestPoly(end, ext, &filter, &endRef, end);
    if (!startRef || !endRef) {
        return {};
    }
    dtPolyRef polys[MAX_POLYS];
    int npolys = 0;
    if (straight) {
        d.query->raycast(startRef, start, end, &filter, nullptr, nullptr, polys, &npolys, MAX_POLYS);
    }
    dtStatus st = d.query->findPath(startRef, endRef, start, end, &filter, polys, &npolys, MAX_POLYS);
    if (dtStatusFailed(st) || npolys == 0) {
        return {};
    }
    float sl[MAX_POINTS * 3];
    unsigned char flags[MAX_POINTS];
    dtPolyRef sp[MAX_POINTS];
    int nstraight = 0;
    d.query->findStraightPath(start, end, polys, npolys, sl, flags, sp, &nstraight, MAX_POINTS);
    std::vector<float> out;
    for (int i = 1; i < nstraight; i++) {
        float rx = sl[i * 3 + 2];
        float ry = sl[i * 3 + 0];
        float rz = sl[i * 3 + 1];
        out.push_back(rx);
        out.push_back(ry);
        out.push_back(rz);
    }
    return out;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_tbc_world_mmap_DetourNative_setDataDir(JNIEnv* env, jclass, jstring path) {
    const char* p = env->GetStringUTFChars(path, nullptr);
    std::lock_guard<std::mutex> lock(g_mu);
    g_base = p ? p : "";
    env->ReleaseStringUTFChars(path, p);
    return JNI_TRUE;
}

static jfloatArray toJava(JNIEnv* env, const std::vector<float>& v) {
    jfloatArray a = env->NewFloatArray((jsize)v.size());
    if (!v.empty()) {
        env->SetFloatArrayRegion(a, 0, (jsize)v.size(), v.data());
    }
    return a;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_org_tbc_world_mmap_DetourNative_findPath(JNIEnv* env, jclass, jint mapId,
        jfloat sx, jfloat sy, jfloat sz, jfloat dx, jfloat dy, jfloat dz, jboolean straight) {
    return toJava(env, queryPath(mapId, sx, sy, sz, dx, dy, dz, straight == JNI_TRUE));
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_org_tbc_world_mmap_DetourNative_randomPoint(JNIEnv* env, jclass, jint mapId,
        jfloat hx, jfloat hy, jfloat hz, jfloat radius, jfloat angle01, jfloat dist01) {
    float x = hx + (float)(cos(angle01 * 6.2831853) * dist01 * radius);
    float y = hy + (float)(sin(angle01 * 6.2831853) * dist01 * radius);
    return toJava(env, queryPath(mapId, hx, hy, hz, x, y, hz, false));
}
