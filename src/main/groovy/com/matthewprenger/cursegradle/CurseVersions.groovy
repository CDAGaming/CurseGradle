package com.matthewprenger.cursegradle

import com.google.common.base.Throwables
import com.matthewprenger.cursegradle.jsonresponse.VersionType
import com.matthewprenger.cursegradle.jsonresponse.GameVersion
import gnu.trove.map.TObjectIntMap
import gnu.trove.map.hash.TObjectIntHashMap
import gnu.trove.set.TIntSet
import gnu.trove.set.hash.TIntHashSet
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class CurseVersions {

    private static final Logger log = Logging.getLogger(CurseVersions)

    private static final TObjectIntMap<String> gameVersions = new TObjectIntHashMap<>()

    /**
     * Load the valid game versions from CurseForge
     * @param apiKey The api key to use to connect to CurseForge
     */
    static void initialize(String apiKey, Project project) {

        gameVersions.clear()

        log.info 'Initializing CurseForge versions...'

        CurseExtension ext = project.extensions.getByType(CurseExtension)
        Options curseGradleOptions = ext.curseGradleOptions

        try {
            TIntSet validVersionTypes = new TIntHashSet()

            String versionTypesJson = Util.httpGet(apiKey, CurseGradlePlugin.getVersionTypesUrl())
            //noinspection GroovyAssignabilityCheck
            VersionType[] types = Util.gson.fromJson(versionTypesJson, VersionType[].class)
            types.each { type ->
                if (
                        (type.slug.startsWith('minecraft') || type.slug == 'java' || type.slug == 'environment' || type.slug == 'modloader') ||
                        (curseGradleOptions.bukkitIntegration && type.slug == 'bukkit') ||
                        (curseGradleOptions.genericIntegration && type.slug == 'game')
                ) {
                    validVersionTypes.add(type.id)
                }
            }

            String gameVersionsJson = Util.httpGet(apiKey, CurseGradlePlugin.getVersionUrl())
            //noinspection GroovyAssignabilityCheck
            GameVersion[] versions = Util.gson.fromJson(gameVersionsJson, GameVersion[].class)
            versions.each { version ->
                if (validVersionTypes.contains(version.gameVersionTypeID)) {
                    gameVersions.put(version.name, version.id)
                }
            }

            log.info 'CurseForge versions initialized'
        } catch (Throwable t) {
            throw Throwables.propagate(t)
        }
    }

    static int[] resolveGameVersion(final Iterable<Object> objects) {
        TIntSet set = new TIntHashSet()
        objects.each { obj ->
            final String version = obj.toString()
            int id = gameVersions.get(version)
            if (id == 0) {
                log.warn("$version is not a valid game version. Valid versions are: ${gameVersions.keySet()}")
            } else {
                set.add(id)
            }
        }

        return set.toArray()
    }
}
