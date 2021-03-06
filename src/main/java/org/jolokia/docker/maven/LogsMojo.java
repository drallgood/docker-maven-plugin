package org.jolokia.docker.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.jolokia.docker.maven.access.DockerAccess;
import org.jolokia.docker.maven.access.DockerAccessException;
import org.jolokia.docker.maven.config.*;
import org.jolokia.docker.maven.log.LogDispatcher;

/**
 * Mojo for printing logs of a container. By default the logs of all containers are shown interwoven
 * with the time occured. The log output can be highly customized in the plugin configuration, please
 * refer to the reference manual for documentation.
 *
 * This Mojo is intended for standalone usage. See {@link StartMojo} for how to enabling logging when
 * starting containers.
 *
 * @author roland
 * @since 26.03.14
 *
 * @goal logs
 */
public class LogsMojo extends AbstractDockerMojo {

    /**
     * Whether to log infinitely or to show only the logs happened until now.
     *
     * @parameter property = "docker.follow" default-value = "false"
     */
    private boolean follow;

    // Whether to log all containers or only the newest ones
    /** @parameter property = "docker.logAll" default-value = "false" */
    private boolean logAll;

    protected void executeInternal(DockerAccess access) throws MojoExecutionException, DockerAccessException {

        LogDispatcher logDispatcher = getLogDispatcher(access);

        for (ImageConfiguration image : getImages()) {
            String imageName = image.getName();
            if (logAll) {
                for (String container : access.getContainersForImage(imageName)) {
                    doLogging(logDispatcher, image, container);
                }
            } else {
                String container = access.getNewestImageForContainer(imageName);
                doLogging(logDispatcher, image, container);
            }
        }
        if (follow) {
            // Block forever ....
            waitForEver();
        }
    }

    private void doLogging(LogDispatcher logDispatcher, ImageConfiguration image, String container) {
        if (follow) {
            logDispatcher.trackContainerLog(container, getContainerLogSpec(container, image));
        } else {
            logDispatcher.fetchContainerLog(container, getContainerLogSpec(container, image));
        }
    }

    private synchronized void waitForEver() {
        while (true) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                // sleep again
            }
        }
    }
}
