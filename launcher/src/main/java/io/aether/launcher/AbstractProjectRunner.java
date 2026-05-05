package io.aether.launcher;

import java.nio.file.Path;

/**
 * Base class for project runners, providing common fields and utility methods.
 */
public abstract class AbstractProjectRunner implements ProjectRunner {
    protected LauncherContext ctx;
    protected final String repoDir;

    protected AbstractProjectRunner(LauncherContext ctx, String repoDir) {
        this.ctx = ctx;
        this.repoDir = repoDir;
    }

    @Override
    public Path getRepoPath() {
        return ctx.workspace().resolve(repoDir);
    }

    public String getRepoDir() {
        return repoDir;
    }

}