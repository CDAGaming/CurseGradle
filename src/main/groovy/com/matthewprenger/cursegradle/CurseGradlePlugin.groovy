package com.matthewprenger.cursegradle

import com.google.common.base.Strings
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class CurseGradlePlugin implements Plugin<Project> {

    static final String TASK_NAME = 'curseforge'
    static final String TASK_GROUP = 'upload'
    static final String EXTENSION_NAME = 'curseforge'

    static final Set<String> VALID_RELEASE_TYPES = ['alpha', 'beta', 'release']
    static final Set<String> VALID_RELATIONS = ['requiredDependency', 'embeddedLibrary', 'optionalDependency', 'tool', 'incompatible']

    static String API_BASE_URL
    static String getApiBaseUrl() {
        return API_BASE_URL
    }

    static String getVersionTypesUrl() {
        return "$API_BASE_URL/api/game/version-types"
    }

    static String getVersionUrl() {
        return "$API_BASE_URL/api/game/versions"
    }

    static String getUploadUrl() {
        return "$API_BASE_URL/api/projects/%s/upload-file"
    }

    Project project
    CurseExtension extension

    @Override
    void apply(final Project project) {
        this.project = project

        final Task mainTask = project.tasks.create(TASK_NAME, DefaultTask)
        mainTask.description = "Uploads all CurseForge projects"
        mainTask.group = TASK_GROUP

        extension = project.extensions.create(EXTENSION_NAME, CurseExtension, project)

        project.afterEvaluate {
            if (project.state.failure != null) {
                project.logger.info 'Failure detected. Not running afterEvaluate'
            }

            extension.curseProjects.each { curseProject ->

                Util.check(!Strings.isNullOrEmpty(curseProject.id), "A CurseForge project was configured without an id")

                CurseUploadTask uploadTask = project.tasks.create("curseforge$curseProject.id", CurseUploadTask)
                curseProject.uploadTask = uploadTask
                uploadTask.group = TASK_GROUP
                uploadTask.description = "Uploads CurseForge project $curseProject.id"
                uploadTask.additionalArtifacts = curseProject.additionalArtifacts
                uploadTask.apiKey = curseProject.apiKey
                uploadTask.projectId = curseProject.id

                CurseExtension ext = project.extensions.getByType(CurseExtension)
                final boolean isGeneric = ext.curseGradleOptions.genericIntegration
                final boolean isBukkit = ext.curseGradleOptions.bukkitIntegration && !ext.curseGradleOptions.genericIntegration
                final boolean isMC = (!isBukkit && !isGeneric)

                if (ext.curseGradleOptions.javaVersionAutoDetect && isMC) {
                    Integration.checkJavaVersion(project, curseProject)
                }

                if (ext.curseGradleOptions.javaIntegration && isMC) {
                    Integration.checkJava(project, curseProject)
                }
                if (ext.curseGradleOptions.forgeGradleIntegration && isMC) {
                    Integration.checkForgeGradle(project, curseProject)
                }
                if (ext.curseGradleOptions.fabricLoomIntegration && isMC) {
                    Integration.checkFabric(project, ext.curseGradleOptions.javaIntegration, curseProject)
                }

                API_BASE_URL = ext.curseGradleOptions.apiBaseUrl
                if (isBukkit) {
                    API_BASE_URL = 'https://dev.bukkit.org'
                }

                curseProject.copyConfig()

                uploadTask.mainArtifact = curseProject.mainArtifact

                // At this stage, all artifacts should be in a state ready to upload
                // ForgeGradle's reobf tasks are dependants of this
                uploadTask.dependsOn project.tasks.getByName('assemble')

                // Run after build if it's on the task graph. This is useful because if tests fail,
                // the artifacts won't be uploaded
                uploadTask.mustRunAfter project.tasks.getByName('build')

                mainTask.dependsOn uploadTask
                uploadTask.onlyIf { mainTask.enabled }

                curseProject.validate()

                if (curseProject.mainArtifact.artifact instanceof Task) {
                    uploadTask.dependsOn((Task) curseProject.mainArtifact.artifact)
                }

                curseProject.additionalArtifacts.each { artifact ->
                    if (artifact.artifact instanceof Task) {
                        uploadTask.dependsOn((Task) artifact.artifact)
                    }
                }
            }
        }
    }
}
