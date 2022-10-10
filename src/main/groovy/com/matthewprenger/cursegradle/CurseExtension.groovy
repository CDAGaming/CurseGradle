package com.matthewprenger.cursegradle

import org.gradle.api.Action
import org.gradle.api.Project

class CurseExtension {

    /**
     * Optional global apiKey. Will be applied to all projects that don't declare one
     */
    def apiKey = '' // Initialize to empty string to delay error until the task is actually ran

    final Collection<CurseProject> curseProjects = new ArrayList<>()

    Options curseGradleOptions

    CurseExtension(Project project) {
        curseGradleOptions = new Options(project)
    }

    @Deprecated
    boolean getDebug() {
        return curseGradleOptions.debug
    }

    @Deprecated
    void setDebug(boolean debug) {
        curseGradleOptions.debug = debug
    }

    /**
     * Define a new CurseForge project for deployment
     *
     * @param configClosure The configuration closure
     */
    void project(Action<CurseProject> configClosure) {
        CurseProject curseProject = new CurseProject()
        configClosure.execute(curseProject)
        if (curseProject.apiKey == null) {
            curseProject.apiKey = this.apiKey
        }
        curseProjects.add(curseProject)
    }

    void options(Action<Options> configClosure) {
        configClosure.execute(curseGradleOptions)
    }
}
