cmake_minimum_required(VERSION 3.16)
project(tbcnav LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 17)
find_package(JNI REQUIRED)
set(DETOUR_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../recastnavigation/Detour)
add_library(tbcnav SHARED
  tbc_nav.cpp
  ${DETOUR_DIR}/Source/DetourAlloc.cpp
  ${DETOUR_DIR}/Source/DetourAssert.cpp
  ${DETOUR_DIR}/Source/DetourCommon.cpp
  ${DETOUR_DIR}/Source/DetourNavMesh.cpp
  ${DETOUR_DIR}/Source/DetourNavMeshBuilder.cpp
  ${DETOUR_DIR}/Source/DetourNavMeshQuery.cpp
  ${DETOUR_DIR}/Source/DetourNode.cpp
)
target_include_directories(tbcnav PRIVATE ${JNI_INCLUDE_DIRS} ${DETOUR_DIR}/Include)
target_compile_definitions(tbcnav PRIVATE DT_POLYREF64)
